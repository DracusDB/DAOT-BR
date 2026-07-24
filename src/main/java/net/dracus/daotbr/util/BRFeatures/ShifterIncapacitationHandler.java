package net.dracus.daotbr.util.BRFeatures;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.packet.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;


public class ShifterIncapacitationHandler {

    // players currently on the post-incapacitation cooldown (mirrors the "shifter_regen_cooldown" tag)
    private static final Set<UUID> incapacitationCooldown = new HashSet<>();

    // ---- reflection into Danny's ModNetworking (soft dependency — no hard import allowed) ----
    private static Class<?> modNetworkingClass;
    private static boolean modNetworkingLookupAttempted = false;
    private static Method getMaxStaminaMethod;
    private static Method drainStaminaMethod;

    private static void resolveModNetworking() {
        if (modNetworkingLookupAttempted) return;
        modNetworkingLookupAttempted = true;
        try {
            modNetworkingClass = Class.forName("daot.network.ModNetworking");
            getMaxStaminaMethod = modNetworkingClass.getMethod("getMaxStaminaForUUID", UUID.class);
            drainStaminaMethod = modNetworkingClass.getMethod("drainStamina", UUID.class, float.class);
        } catch (Exception e) {
            modNetworkingClass = null;
        }
    }

    private static float getMaxStamina(UUID playerUUID) {
        resolveModNetworking();
        if (modNetworkingClass == null) return 2000f; // ModNetworking's own fallback default
        try {
            return (float) getMaxStaminaMethod.invoke(null, playerUUID);
        } catch (Exception e) {
            return 2000f;
        }
    }

    private static void drainStamina(UUID playerUUID, float amount) {
        resolveModNetworking();
        if (modNetworkingClass == null) return;
        try {
            drainStaminaMethod.invoke(null, playerUUID, amount);
        } catch (Exception ignored) {
        }
    }

    // ---- reflection into Danny's DannysAot for the forceShifting gamerule ----
    private static Class<?> dannysAotClass;
    private static boolean dannysAotLookupAttempted = false;
    private static Object ruleForceShifting; // GameRules.Key<GameRules.BooleanRule>

    private static void resolveDannysAot() {
        if (dannysAotLookupAttempted) return;
        dannysAotLookupAttempted = true;
        try {
            dannysAotClass = Class.forName("daot.DannysAot");
            ruleForceShifting = dannysAotClass.getField("RULE_FORCE_SHIFTING").get(null);
        } catch (Exception e) {
            dannysAotClass = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isForceShiftingEnabled(ServerPlayerEntity player) {
        resolveDannysAot();
        if (ruleForceShifting == null) return false;
        try {
            GameRules.Key<GameRules.BooleanRule> key = (GameRules.Key<GameRules.BooleanRule>) ruleForceShifting;
            return player.getWorld().getGameRules().getBoolean(key);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- reflection into Danny's ODMJamPayload (soft dependency — already registered by ModNetworking) ----
    private static Class<?> odmJamPayloadClass;
    private static boolean odmJamPayloadLookupAttempted = false;
    private static java.lang.reflect.Constructor<?> odmJamPayloadConstructor;

    private static void resolveOdmJamPayload() {
        if (odmJamPayloadLookupAttempted) return;
        odmJamPayloadLookupAttempted = true;
        try {
            odmJamPayloadClass = Class.forName("daot.network.ODMJamPayload");
            odmJamPayloadConstructor = odmJamPayloadClass.getConstructor(int.class, boolean.class);
        } catch (Exception e) {
            odmJamPayloadClass = null;
        }
    }

    private static void jamOdm(ServerPlayerEntity player, int durationMs, boolean releaseHooks) {
        resolveOdmJamPayload();
        if (odmJamPayloadClass == null) return;
        try {
            CustomPayload payload = (CustomPayload) odmJamPayloadConstructor.newInstance(durationMs, releaseHooks);
            ServerPlayNetworking.send(player, payload);
        } catch (Exception ignored) {
        }
    }

    private static void registerInputLock() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (incapacitationCooldown.contains(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (incapacitationCooldown.contains(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (incapacitationCooldown.contains(player.getUuid())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            if (incapacitationCooldown.contains(player.getUuid())) return TypedActionResult.fail(player.getStackInHand(hand));
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }

    // ---- registration ----
    public static void register() {
        registerInputLock();
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (player.getWorld().isClient()) return true;
            if (GameStageManager.isInLobby(player.getWorld())) return true; // lobby handler already owns this case

            boolean isShifter = hasAnyShifterTag(player);
            if (!isShifter) return true;

            if (incapacitationCooldown.contains(player.getUuid())) return true;

            if (isForceShiftingEnabled(player)) return true; // matches !forceShiftingEnabled in the original script

            boolean shifterDefeated = isWeaknessFiveOrHigher(player);
            if (shifterDefeated) return true;

            float maxStamina = getMaxStamina(player.getUuid());

            incapacitate(player, maxStamina);
            return false; // cancel the death
        });
    }

    private static boolean hasAnyShifterTag(ServerPlayerEntity player) {
        Set<String> tags = player.getCommandTags();
        return tags.contains("attack") || tags.contains("armored") || tags.contains("beast")
                || tags.contains("colossal") || tags.contains("female") || tags.contains("jaw")
                || tags.contains("warhammer");
    }

    private static boolean isWeaknessFiveOrHigher(ServerPlayerEntity player) {
        StatusEffectInstance weakness = player.getStatusEffect(StatusEffects.WEAKNESS);
        return weakness != null && weakness.getAmplifier() >= 4; // amplifier is 0-indexed; level 5 = amplifier 4
    }

    private static void incapacitate(ServerPlayerEntity player, float maxStamina) {
        player.setHealth(1.0f);

        String name = player.getName().getString();
        MinecraftServer server = player.getServer();
        if (server == null) return;

//        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "tag " + name + " add shifter_regen_cooldown");
//        incapacitationCooldown.add(player.getUuid());
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "tag " + name + " add shifter_regen_cooldown");
        incapacitationCooldown.add(player.getUuid());
        scheduledCooldownRemovals.add(new ScheduledCooldownRemoval(player, COOLDOWN_DURATION_MS));

        drainStamina(player.getUuid(), maxStamina * 0.4f);

        jamOdm(player, 45_000, true);

        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect clear @a");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:instant_health 1 1 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:slowness 45 4 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:regeneration 45 0 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:weakness 45 3 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:mining_fatigue 45 4 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:slow_falling 10 1 true");
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:glowing 45 0 true");

        player.sendMessage(Text.literal("You've suffered from what would have been a killing blow for a normal human. You need to give your body time to regenerate.")
                .formatted(Formatting.AQUA), false);

        server.getPlayerManager().broadcast(
                Text.literal("Titan Shifter " + name + " suffered a killing blow at " + (int) Math.ceil(player.getX()) + " " + (int) Math.ceil(player.getY()) + " " + (int) Math.ceil(player.getZ()) + "!")
                        .formatted(Formatting.RED, Formatting.BOLD), false);

        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ---- cooldown removal scheduling ----
    private static final List<ScheduledCooldownRemoval> scheduledCooldownRemovals = new ArrayList<>();
    private static final long COOLDOWN_DURATION_MS = 45_000L;

    private static class ScheduledCooldownRemoval {
        final ServerPlayerEntity player;
        final UUID playerUUID;
        final long startTimeMillis;
        final long durationMillis;

        ScheduledCooldownRemoval(ServerPlayerEntity player, long durationMillis) {
            this.player = player;
            this.playerUUID = player.getUuid();
            this.startTimeMillis = System.currentTimeMillis();
            this.durationMillis = durationMillis;
        }
    }

    public static void initCooldownScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledCooldownRemoval> iterator = scheduledCooldownRemovals.iterator();
            while (iterator.hasNext()) {
                ScheduledCooldownRemoval task = iterator.next();
                long elapsed = System.currentTimeMillis() - task.startTimeMillis;

                if (elapsed >= task.durationMillis) {
                    incapacitationCooldown.remove(task.playerUUID);

                    if (task.player.isRemoved()) {
                        iterator.remove();
                        continue;
                    }

                    String name = task.player.getName().getString();
                    server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "tag " + name + " remove shifter_regen_cooldown");
                    server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "effect give " + name + " minecraft:resistance infinite 0 true");
                    iterator.remove();
                }
            }
        });
    }
}
