package net.dracus.daotbr.item.Airdrops;

import net.dracus.daotbr.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.particle.ParticleTypes;


public class ShifterAirdropManager {
    private static final List<FallingCrate> activateCrates = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static void register(DisplayEntity.BlockDisplayEntity entity, DisplayEntity.BlockDisplayEntity parachute, double targetY) {
        double distance = entity.getY() - targetY;
        double fallSpeed = distance / (45 * 20);
        activateCrates.add(new FallingCrate(entity, parachute, targetY, fallSpeed));
    }

    private static class FallingCrate {
        final DisplayEntity.BlockDisplayEntity entity;
        final DisplayEntity.BlockDisplayEntity parachute;
        final double targetY;
        final double fallSpeed;

        FallingCrate(DisplayEntity.BlockDisplayEntity entity, DisplayEntity.BlockDisplayEntity parachute, double targetY, double fallSpeed) {
            this.entity = entity;
            this.parachute = parachute;
            this.targetY = targetY;
            this.fallSpeed = fallSpeed;
        }
    }

    private static void addWeighted(List<Item> pool, Item item, int weight) {
        for (int i = 0; i < weight; i++) {
            pool.add(item);
        }
    }

    private static final List<ScheduledRemoval> scheduledRemovals = new ArrayList<>();

    public static void scheduleWaypointRemoval(MinecraftServer server, String waypointName, int delayTicks) {
        scheduledRemovals.add(new ScheduledRemoval(server, waypointName, delayTicks));
    }

    public static void initWaypointScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledRemoval> iterator = scheduledRemovals.iterator();
            while (iterator.hasNext()) {
                ScheduledRemoval task = iterator.next();
                task.ticksRemaining--;

                if (task.ticksRemaining <= 0) {
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
        int ticksRemaining;

        ScheduledRemoval(MinecraftServer server, String waypointName, int ticksRemaining) {
            this.server = server;
            this.waypointName = waypointName;
            this.ticksRemaining = ticksRemaining;
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

                double newY = crate.entity.getY() - crate.fallSpeed;

                if (newY <= crate.targetY) {
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