package net.dracus.daotbr.util.BRFeatures;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FlareGunListener {



    public static void register() {
        UseItemCallback.EVENT.register((PlayerEntity player, World world, Hand hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            Item flareGun = Registries.ITEM.get(Identifier.of("dannys-aot", "flare_gun"));

            if (!stack.isOf(flareGun) || world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null) {
                return TypedActionResult.pass(stack);
            }

            NbtCompound nbt = customData.copyNbt();
            if (!nbt.contains("FlareColor") || nbt.getInt("FlareColor") == -1) {
                return TypedActionResult.pass(stack);
            }

            int nearbyPlayerCount = 0;

            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                ServerWorld serverWorld = (ServerWorld) world;

                for (ServerPlayerEntity nearby : serverWorld.getPlayers()) {
                    if (nearby == serverPlayer || nearby.isSpectator()) {
                        continue;
                    }

                    if (nearby.getPos().distanceTo(serverPlayer.getPos()) <= 200) {
                        nearby.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, true));
                        nearbyPlayerCount++;
                    }

                }

                if (nearbyPlayerCount > 0) {
                    String playerWord = nearbyPlayerCount == 1 ? " player" : " players";
                    String hasHave = nearbyPlayerCount == 1 ? " has" : " have";
                    serverPlayer.sendMessage(
                            Text.literal(nearbyPlayerCount + playerWord + hasHave + " been marked by your flare.").formatted(Formatting.GREEN)
                    );
                }

                else {
                    serverPlayer.sendMessage(
                            Text.literal("Your flare has not marked any players.").formatted(Formatting.RED)
                    );
                }
            }

            return TypedActionResult.pass(stack);
        });
    }
}