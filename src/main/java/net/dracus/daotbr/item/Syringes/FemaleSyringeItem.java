package net.dracus.daotbr.item.Syringes;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;


//TO BE CHANGED PER TITAN
public class FemaleSyringeItem extends Item {
    public FemaleSyringeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));

    }

    //TO BE CHANGED PER TITAN
    @Override
    public void appendTooltip (ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Its greatest strength is its versatility. It boasts a combination of high mobility and endurance,").formatted(Formatting.WHITE, Formatting.ITALIC)));
        tooltip.add((Text.literal("and its hardened strikes are destructive.").formatted(Formatting.WHITE, Formatting.ITALIC)));
    }

    @Override
    public boolean hasGlint (ItemStack stack) {
        return true;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 30;
    }

    //TO BE CHANGED PER TITAN
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                String playerName = serverPlayer.getName().getString();

                List<String> commands = List.of(
                        "daot bloodline remove " + playerName + " ackerman",
                        "daot bloodline set " + playerName + " eldian",
                        "daot shifter set " + playerName + " female",
                        "team join Female " + playerName,
                        "tag " + playerName + " add titan_stealth",
                        "tag " + playerName + " remove has_hardening",
                        "execute at @a run playsound dannys-aot:femaletitan_roar ambient @a ~ ~ ~ 1.0"

                );

                for (String command : commands) {
                    System.out.println("Running command: [" + command + "]");
                    server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
                }


                List<SoundEvent> sounds = List.of(
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT
                );

                for (SoundEvent sound : sounds) {
                    world.playSound(
                            null,
                            serverPlayer.getBlockPos(),
                            sound,
                            SoundCategory.PLAYERS,
                            6.0f,
                            0.3f
                    );
                }

                //TO BE CHANGED PER TITAN
                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(
                            Text.literal(playerName + " has claimed the power of the Female Titan!")
                                    .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    );
                }
            }
        }

        if (user instanceof PlayerEntity player) {
            Item emptySyringe = Registries.ITEM.get(Identifier.of("dannys-aot", "empty_syringe"));
            return ItemUsage.exchangeStack(stack, player, new ItemStack(emptySyringe));
        }

        return stack;
    }
}