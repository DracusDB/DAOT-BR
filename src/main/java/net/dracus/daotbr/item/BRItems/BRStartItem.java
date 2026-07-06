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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class BRStartItem extends Item {
    public BRStartItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        world.playSound(
                null,
                user.getBlockPos(),
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );

        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add((Text.literal("Begin a small battle royale in Paradis").formatted(Formatting.GREEN)));
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
        return 50;
    }

    //TO BE CHANGED PER TITAN
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                String playerName = serverPlayer.getName().getString();

                List<String> commands = List.of(
                        //sets all players to eldian and resets shifters
                        "daot bloodline remove @a ackerman",
                        "daot bloodline remove @a royal",
                        "daot bloodline remove @a marleyan",
                        "daot bloodline set @a eldian",
                        "daot shifter reset",

                        //sets all players to adventure mode so they can't break the map and use "cheese" strategies like hiding underground and crafting items
                        "gamemode adventure @a",

                        //sets preferred gamerules as intended
                        "gamerule villagersSpawnWithPowers false",
                        "gamerule forceShifting false",
                        "gamerule allowCuffing true",
                        "gamerule allowODM true",
                        "gamerule allowSelfInject true",
                        "gamerule allowThunderSpears true",
                        "gamerule injectOtherPlayers true",
                        "gamerule multipleShifters true",
                        "gamerule shifterBossBars true",
                        "gamerule thunderSpearGriefing true",
                        "gamerule titanGore true",
                        "gamerule titanGriefing true",
                        "gamerule confirmedTitanShifterKill false",
                        "gamerule YmirCurse false",
                        "gamerule realisticResourceUse false",
                        "gamerule weatherAffectsODM false",
                        "gamerule shifterSnitching false",
                        "gamerule ShifterExplosionDamage false",
                        "gamerule fairTitanPowerLoss false",
                        "gamerule unfairPureTitans false",
                        //pvp should be set to false when joining lobby, but set to true when in game
                        "gamerule pvp true",
                        "gamerule announceAdvancements false",
                        "gamerule doFireTick false",
                        "gamerule keepInventory false",
                        "gamerule doLimitedCrafting true",

                        "time set day",

                        //creates teams
                        "team add Attack",
                        "team modify Attack color dark_green",
                        "team add Armored",
                        "team modify Armored color gold",
                        "team add Beast",
                        "team modify Beast color red",
                        "team add Cart",
                        "team modify Cart gray",
                        "team add Colossal",
                        "team modify Colossal color dark_red",
                        "team add Female",
                        "team modify Female color light_purple",
                        "team add Jaw",
                        "team modify Jaw color yellow",
                        "team add Warhammer",
                        "team modify Warhammer color dark_purple",
                        "team add Ackerman",
                        "team modify Ackerman color dark_gray",

                        "team leave @a",

                        "effect give @a resistance 30 4 true",

                        "flexborder set 1750 1750 -1750 -1750 dannys-aot:paradis",
                        "execute in dannys-aot:paradis run spreadplayers 0 0 1700 1700 false @a",
                        "execute in dannys-aot:paradis run flexborder start_full 300 200 30 30 100",
                        "execute in dannys-aot:paradis run worldborder center 0 0",
                        "flexborder hploss 0"


                );

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(
                            Text.literal("You have thirty seconds of invulnerability before combat begins, and five minutes before the border begins to shrink. Good luck!")
                                    .formatted(Formatting.GOLD, Formatting.BOLD)
                    );

                    for (String command : commands) {
                        System.out.println("Running command: [" + command + "]");
                        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
                    }

                    stack.decrement(1);
                }

            }

        }
        return stack;
    }
}

