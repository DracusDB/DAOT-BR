package net.dracus.daotbr.item.BRItems;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;


public class ThunderSpearPackageItem extends Item {
    public ThunderSpearPackageItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));

    }

    @Override
    public void appendTooltip (ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Right click to open a bundle of thunder spears. Re-craft them to make the package again.").formatted(Formatting.GRAY)));
    }

    @Override
    public boolean hasGlint (ItemStack stack) {
        return true;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 1;
    }


    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            Item thunderSpear = Registries.ITEM.get(Identifier.of("dannys-aot", "thunder_spear"));

            ItemStack spearStack = new ItemStack(thunderSpear, 3);
            if (!player.getInventory().insertStack(spearStack)) {
                player.dropItem(spearStack, false);
            }

            world.playSound(
                    null,
                    player.getBlockPos(),
                    SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
            );

            stack.decrement(1);
            return stack;
        }



        return stack;
    }
}

