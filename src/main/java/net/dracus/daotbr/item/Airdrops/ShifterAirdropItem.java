package net.dracus.daotbr.item.Airdrops;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
                ServerWorld serverWorld = (ServerWorld) world;
                BlockPos playerPos = serverPlayer.getBlockPos();
                int groundY = serverWorld.getTopY(Heightmap.Type.MOTION_BLOCKING, playerPos.getX(), playerPos.getZ());
                BlockPos targetPos = new BlockPos(playerPos.getX(), groundY, playerPos.getZ());

                String playerName = serverPlayer.getName().getString();
                String message = playerName + " has summoned a shifter syringe airdrop at "
                        + targetPos.getX() + " " + targetPos.getZ() + "! It will land in 60 seconds!";

                ShifterAirdropManager.spawnAirdropAt(server, serverWorld, targetPos,
                        Text.literal(message).formatted(Formatting.AQUA, Formatting.ITALIC, Formatting.BOLD));

                stack.decrement(1);
            }
        }
        return stack;
    }
}
