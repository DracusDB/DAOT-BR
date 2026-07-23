package net.dracus.daotbr.item.Airdrops;

import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.BRFeatures.GameStageManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.*;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;


public class ShifterAirdropManager {
    private static final List<FallingCrate> activateCrates = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static void register(DisplayEntity.BlockDisplayEntity entity, DisplayEntity.BlockDisplayEntity parachute, double targetY, int fallDurationSeconds) {
        double startY = entity.getY();
        long durationMillis = fallDurationSeconds * 1000L;
        activateCrates.add(new FallingCrate(entity, parachute, startY, targetY, durationMillis));
    }

    private static class FallingCrate {
        final DisplayEntity.BlockDisplayEntity entity;
        final DisplayEntity.BlockDisplayEntity parachute;
        final double startY;
        final double targetY;
        final long startTimeMillis;
        final long durationMillis;

        FallingCrate(DisplayEntity.BlockDisplayEntity entity, DisplayEntity.BlockDisplayEntity parachute, double startY, double targetY, long durationMillis) {
            this.entity = entity;
            this.parachute = parachute;
            this.startY = startY;
            this.targetY = targetY;
            this.startTimeMillis = System.currentTimeMillis();
            this.durationMillis = durationMillis;
        }
    }

    private static void addWeighted(List<Item> pool, Item item, int weight) {
        for (int i = 0; i < weight; i++) {
            pool.add(item);
        }
    }

    // ---- shared spawn logic, used by both the item and the random zone-drop scheduler ----
    public static void spawnAirdropAt(MinecraftServer server, ServerWorld serverWorld, BlockPos targetPos, Text announceMessage) {
        double spawnY = 250;

        DisplayEntity.BlockDisplayEntity crate = EntityType.BLOCK_DISPLAY.create(serverWorld);
        if (crate == null) return;

        crate.setBlockState(Blocks.CHEST.getDefaultState());
        crate.refreshPositionAndAngles(targetPos.getX() + 0.5, spawnY, targetPos.getZ() + 0.5, 0f, 0f);
        serverWorld.spawnEntity(crate);

        DisplayEntity.BlockDisplayEntity parachute = EntityType.BLOCK_DISPLAY.create(serverWorld);
        if (parachute == null) return;

        parachute.setBlockState(Blocks.WHITE_WOOL.getDefaultState());
        parachute.refreshPositionAndAngles(targetPos.getX() + 0.5, spawnY + 3, targetPos.getZ() + 0.5, 0f, 0f);
        serverWorld.spawnEntity(parachute);

        register(crate, parachute, targetPos.getY(), 60);

        String dimensionId = serverWorld.getRegistryKey().getValue().toString();
        String waypointName = "Shifter_Airdrop";
        String location = targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ();
        String createCommand = "jm waypoint create " + waypointName + " " + dimensionId + " " + location + " aqua @a true";
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), createCommand);
        scheduleWaypointRemoval(server, waypointName, 90);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(announceMessage);
        }

        serverWorld.playSound(null, targetPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ---- random drop-in-zone scheduler ----
    private static final long RANDOM_DROP_CHECK_INTERVAL_MS = 60_000;
    private static final double[] PHASE_DROP_CHANCE = { 0.02, 0.04, 0.06, 0.08, 0.10 };
    private static long lastRandomDropCheck = 0;


    public static void initRandomDropScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!GameStageManager.isInArenaStage()) return;

            long now = System.currentTimeMillis();
            if (now - lastRandomDropCheck < RANDOM_DROP_CHECK_INTERVAL_MS) return;
            lastRandomDropCheck = now;

            int phaseIndex = GameStageManager.getZonePhaseIndex();
            double chance = PHASE_DROP_CHANCE[Math.min(phaseIndex, PHASE_DROP_CHANCE.length - 1)];
            if (RANDOM.nextDouble() >= chance) return;

            ServerWorld arenaWorld = server.getWorld(GameStageManager.ARENA_DIMENSION);
            if (arenaWorld == null) return;

            double centerX = GameStageManager.getZoneCenterX();
            double centerZ = GameStageManager.getZoneCenterZ();
            double radius = GameStageManager.getZoneRadius();

            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double distance = Math.sqrt(RANDOM.nextDouble()) * radius;
            int x = (int) (centerX + distance * Math.cos(angle));
            int z = (int) (centerZ + distance * Math.sin(angle));

            ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
            arenaWorld.getChunkManager().setChunkForced(chunkPos, true);
            scheduledUnforces.add(new ScheduledUnforce(arenaWorld, chunkPos, 70_000));

            arenaWorld.getChunk(x >> 4, z >> 4); // force synchronous load before reading the heightmap

            int groundY = arenaWorld.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            BlockPos targetPos = new BlockPos(x, groundY, z);

            spawnAirdropAt(server, arenaWorld, targetPos,
                    Text.literal("A random shifter syringe airdrop has been spawned at " + x + " " + z + "! It will land in 60 seconds!")
                            .formatted(Formatting.GREEN, Formatting.ITALIC, Formatting.BOLD));
        });
    }

    private static final List<ScheduledUnforce> scheduledUnforces = new ArrayList<>();

    private static class ScheduledUnforce {
        final ServerWorld world;
        final ChunkPos pos;
        final long startTimeMillis;
        final long durationMillis;

        ScheduledUnforce(ServerWorld world, ChunkPos pos, long durationMillis) {
            this.world = world;
            this.pos = pos;
            this.startTimeMillis = System.currentTimeMillis();
            this.durationMillis = durationMillis;
        }
    }

    public static void initChunkUnforceScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledUnforce> iterator = scheduledUnforces.iterator();
            while (iterator.hasNext()) {
                ScheduledUnforce task = iterator.next();
                if (System.currentTimeMillis() - task.startTimeMillis >= task.durationMillis) {
                    task.world.getChunkManager().setChunkForced(task.pos, false);
                    iterator.remove();
                }
            }
        });
    }

    private static final List<ScheduledRemoval> scheduledRemovals = new ArrayList<>();

    public static void scheduleWaypointRemoval(MinecraftServer server, String waypointName, int delaySeconds) {
        scheduledRemovals.add(new ScheduledRemoval(server, waypointName, delaySeconds * 1000L));
    }

    public static void initWaypointScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledRemoval> iterator = scheduledRemovals.iterator();
            while (iterator.hasNext()) {
                ScheduledRemoval task = iterator.next();
                long elapsed = System.currentTimeMillis() - task.startTimeMillis;

                if (elapsed >= task.durationMillis) {
                    String deleteCommand = "jm waypoint delete " + task.waypointName + " @a true";
                    System.out.println("Attempting to delete waypoint: [" + deleteCommand + "]");
                    task.server.getCommandManager().executeWithPrefix(task.server.getCommandSource(), deleteCommand);
                    iterator.remove();
                }
            }
        });
    }

    private static class ScheduledRemoval {
        final MinecraftServer server;
        final String waypointName;
        final long startTimeMillis;
        final long durationMillis;

        ScheduledRemoval(MinecraftServer server, String waypointName, long durationMillis) {
            this.server = server;
            this.waypointName = waypointName;
            this.startTimeMillis = System.currentTimeMillis();
            this.durationMillis = durationMillis;
        }
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Iterator<FallingCrate> iterator = activateCrates.iterator();
            while (iterator.hasNext()) {
                FallingCrate crate = iterator.next();

                if (crate.entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                if (crate.entity.getWorld() != world) {
                    continue;
                }

                long elapsed = System.currentTimeMillis() - crate.startTimeMillis;
                double progress = Math.min(1.0, (double) elapsed / crate.durationMillis);
                double newY = crate.startY + (crate.targetY - crate.startY) * progress;

                if (progress >= 1.0) {
                    BlockPos landPos = new BlockPos(
                            (int) Math.floor(crate.entity.getX()),
                            (int) crate.targetY,
                            (int) Math.floor(crate.entity.getZ())
                    );

                    world.setBlockState(landPos, Blocks.CHEST.getDefaultState());

                    BlockEntity blockEntity = world.getBlockEntity(landPos);
                    if (blockEntity instanceof ChestBlockEntity chestEntity) {
                        List<Item> weightedPool = new ArrayList<>();

                        addWeighted(weightedPool, ModItems.ATTACK_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.ARMORED_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.BEAST_SYRINGE, 3);
//                        addWeighted(weightedPool, ModItems.CART_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.FEMALE_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.JAW_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.WARHAMMER_SYRINGE, 3);
                        addWeighted(weightedPool, ModItems.COLOSSAL_SYRINGE, 1);

                        Item syringe = weightedPool.get(RANDOM.nextInt(weightedPool.size()));
                        chestEntity.setStack(13, new ItemStack(syringe));
                    }

                    crate.entity.discard();
                    crate.parachute.discard();
                    iterator.remove();
                } else {
                    crate.entity.setPosition(crate.entity.getX(), newY, crate.entity.getZ());
                    crate.parachute.setPosition(crate.entity.getX(), newY + 3, crate.entity.getZ());

                    ((ServerWorld) world).spawnParticles(
                            ParticleTypes.WHITE_SMOKE,
                            crate.entity.getX(),
                            crate.targetY + 0.5,
                            crate.entity.getZ(),
                            4,
                            0.2,
                            0.2,
                            0.2,
                            0.03
                    );
                }
            }
        });
    }
}