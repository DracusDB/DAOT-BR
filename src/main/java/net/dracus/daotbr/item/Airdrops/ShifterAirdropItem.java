package net.dracus.daotbr.item.Airdrops;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import java.util.List;

public class ShifterAirdropItem extends Item {
    public ShifterAirdropItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Right click to call in a titan shifter syringe airdrop.").formatted(Formatting.GRAY)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 20;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                String playerName = serverPlayer.getName().getString();
                ServerWorld serverWorld = (ServerWorld) world;
                BlockPos playerPos = serverPlayer.getBlockPos();
                int groundY = serverWorld.getTopY(Heightmap.Type.MOTION_BLOCKING, playerPos.getX(), playerPos.getZ());
                BlockPos targetPos = new BlockPos(playerPos.getX(), groundY, playerPos.getZ());
                double spawnY = 250;

                DisplayEntity.BlockDisplayEntity crate = EntityType.BLOCK_DISPLAY.create(serverWorld);

                if (crate != null) {
                    crate.setBlockState(Blocks.CHEST.getDefaultState());
                    crate.refreshPositionAndAngles(
                            targetPos.getX() + 0.5,
                            spawnY,
                            targetPos.getZ() + 0.5,
                            0f,
                            0f
                    );
                    serverWorld.spawnEntity(crate);

                    DisplayEntity.BlockDisplayEntity parachute = EntityType.BLOCK_DISPLAY.create(serverWorld);

                    if (parachute != null) {
                        parachute.setBlockState(Blocks.WHITE_WOOL.getDefaultState());
                        parachute.refreshPositionAndAngles(
                                targetPos.getX() + 0.5,
                                spawnY + 3,
                                targetPos.getZ() + 0.5,
                                0f,
                                0f
                        );
                        serverWorld.spawnEntity(parachute);

                        // Register both entities together
                        ShifterAirdropManager.register(crate, parachute, targetPos.getY(), 60);
                    }

                    String dimensionId = serverWorld.getRegistryKey().getValue().toString();
                    String waypointName = "Shifter_Airdrop";
                    String location = targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ();

                    String createCommand = "jm waypoint create " + waypointName + " " + dimensionId + " " + location + " aqua @a true";
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), createCommand);

                    ShifterAirdropManager.scheduleWaypointRemoval(server, waypointName, 90); // 90 seconds
                }

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(
                            Text.literal(playerName + " has summoned a shifter syringe airdrop at "
                                            + targetPos.getX() + " "
                                            + targetPos.getZ()
                                            + "! It will land in 60 seconds!")
                                    .formatted(Formatting.AQUA, Formatting.ITALIC, Formatting.BOLD)
                    );
                }

                world.playSound(
                        null,
                        serverPlayer.getBlockPos(),
                        SoundEvents.BLOCK_BEACON_ACTIVATE,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                stack.decrement(1);
            }
        }

        return stack;
    }
}
