package net.dracus.daotbr.item.BRItems;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class ShieldPotionItem extends Item {
    public ShieldPotionItem(Settings settings) {
        super(settings);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));

    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Consume to gain overhealth.").formatted(Formatting.AQUA)));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 40;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            float currentAbsorption = serverPlayer.getAbsorptionAmount();
            float maxAbsorption = 20.0f;
            float absorptionPerUse = 8.0f;

            EntityAttributeInstance maxAbsorptionAttr = serverPlayer.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
            if (maxAbsorptionAttr != null && maxAbsorptionAttr.getBaseValue() < maxAbsorption) {
                maxAbsorptionAttr.setBaseValue(maxAbsorption);
            }

            float newAbsorption = Math.min(currentAbsorption + absorptionPerUse, maxAbsorption);
            serverPlayer.setAbsorptionAmount(newAbsorption);

            if (newAbsorption == maxAbsorption) {
                serverPlayer.sendMessage(Text.literal("Max overhealth reached!").formatted(Formatting.AQUA));
            }

            world.playSound(
                    null,
                    serverPlayer.getBlockPos(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
            );

            stack.decrement(1);
        }
        return stack;
    }
}