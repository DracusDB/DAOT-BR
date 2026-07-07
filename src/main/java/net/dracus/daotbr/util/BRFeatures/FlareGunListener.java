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

            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                ServerWorld serverWorld = (ServerWorld) world;

                for (ServerPlayerEntity nearby : serverWorld.getPlayers()) {
                    if (nearby == serverPlayer) {
                        continue;
                    }

                    if (nearby.getPos().distanceTo(serverPlayer.getPos()) <= 150) {
                        nearby.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 150, 0, false, true));
                    }
                }
            }

            return TypedActionResult.pass(stack);
        });
    }
}