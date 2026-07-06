package net.dracus.daotbr.item.BRItems;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class APGPackageItem extends Item {
    public APGPackageItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));

    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Right click to open a full anti-personnel gear set").formatted(Formatting.GRAY, Formatting.ITALIC)));
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
        return 1;
    }

    //TO BE CHANGED PER TITAN
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                String playerName = serverPlayer.getName().getString();

                List<String> commands = List.of(
                        "give " + playerName + " dannys-aot:odm_apg[custom_data={Gas:500}]",
                        "give " + playerName + " dannys-aot:apg_gun 2",
                        "give " + playerName + " dannys-aot:apg_cartridge 8"
                );

                for (String command : commands) {
                    System.out.println("Running command: [" + command + "]");
                    server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
                }

                world.playSound(
                        null,
                        serverPlayer.getBlockPos(),
                        SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),
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

