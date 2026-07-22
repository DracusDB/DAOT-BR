package net.dracus.daotbr.util.BRFeatures;

import net.dracus.daotbr.network.ZoneUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

import java.util.List;

public class BattleZoneManager {
    public record ZonePhase(double endRadius, long shrinkDurationMs, long waitDurationMs, float damagePerSecond,
                            double buffer, double titanDamagePercentPerSecond) {
    }

    private static final List<ZonePhase> PHASES = List.of(
            //make first stage 180 after testing is done, second state 120_000
            new ZonePhase(1300, 180_000, 210_000, 1f, 5, 0.01),
            new ZonePhase(1000, 120_000, 120_000, 2f, 4, 0.02),
            new ZonePhase(700, 90_000, 90_000, 3f, 3, 0.03),
            new ZonePhase(500, 60_000, 60_000, 5f, 2, 0.04),
            new ZonePhase(100, 45_000, 30_000, 8f, 1, 0.05)
    );

    private static final RegistryKey<DamageType> ZONE_DAMAGE_TYPE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("daotbr", "zone_damage"));

    private static Class<?> shifterTitanClass;
    private static boolean shifterTitanLookupAttempted = false;

    private static Class<?> resolveShifterTitanClass() {
        if (!shifterTitanLookupAttempted) {
            shifterTitanLookupAttempted = true;
            try {
                shifterTitanClass = Class.forName("daot.ShifterTitan");
            } catch (ClassNotFoundException e) {
                shifterTitanClass = null;
            }
        }
        return shifterTitanClass;
    }

    private final ServerWorld arenaWorld;
    private final double centerX, centerZ;

    private int phaseIndex = 0;
    private boolean waiting = true;
    private long phaseStartTime;
    private double shrinkStartRadius;

    private long lastDamageTick = 0;
    private long lastActionBarTick = 0;
    private long lastVisualTick = 0;

    private static final long DAMAGE_INTERVAL_MS = 1000;
    private static final long ACTIONBAR_INTERVAL_MS = 1000;
    private static final long VISUAL_INTERVAL_MS = 250;

    public BattleZoneManager(ServerWorld arenaWorld, double centerX, double centerZ, double startingRadius) {
        this.arenaWorld = arenaWorld;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.shrinkStartRadius = startingRadius;
    }

    public double getRadius() {
        return getCurrentRadius(System.currentTimeMillis());
    }

    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }

    public void start() {
        phaseIndex = 0;
        waiting = true;
        phaseStartTime = System.currentTimeMillis();
    }

    private boolean paused = false;
    private long pauseStartedAt = 0;

    public void setPaused(boolean pause){
        if (pause == this.paused) return;
        long now = System.currentTimeMillis();
        if (pause){
            paused = true;
            pauseStartedAt = now;
        } else {
            paused = false;
            phaseStartTime += (now - pauseStartedAt);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void tick(ServerWorld world, List<ServerPlayerEntity> alivePlayers) {
        if (world != arenaWorld) return;

        long now = System.currentTimeMillis();

        if (!paused) {
            boolean wasWaiting = waiting;
            advancePhaseIfNeeded(now);

            if (wasWaiting && !waiting) {
                playSoundToAll(alivePlayers, SHRINK_START_SOUND);
            } else if (!wasWaiting && waiting && phaseIndex < PHASES.size()) {
                playSoundToAll(alivePlayers, SHRINK_STOP_SOUND);
            }
        }

        double radius = getCurrentRadius(now);

        if (!paused) {
            if (now - lastDamageTick >= DAMAGE_INTERVAL_MS) {
                lastDamageTick = now;
                applyZoneDamage(alivePlayers, radius);
            }
            if (now - lastActionBarTick >= ACTIONBAR_INTERVAL_MS) {
                lastActionBarTick = now;
                sendActionBar(alivePlayers, radius, now);
            }
        } else {
            if (now - lastActionBarTick >= ACTIONBAR_INTERVAL_MS) {
                lastActionBarTick = now;
                sendPausedActionBar(alivePlayers);
            }
        }

        // ring keeps rendering at the frozen radius regardless of pause state
        if (now - lastVisualTick >= VISUAL_INTERVAL_MS) {
            lastVisualTick = now;
            renderZoneRing(radius, alivePlayers);
            renderZoneRing(radius + 10, alivePlayers);
        }
    }

    private void sendPausedActionBar(List<ServerPlayerEntity> players) {
        Text text = Text.literal("⏸ Rumbling timer paused").formatted(Formatting.YELLOW);
        for (ServerPlayerEntity player : players) {
            if (player.getWorld() != arenaWorld) continue;
            player.sendMessage(text, true);
        }
    }

    private void playSoundToAll(List<ServerPlayerEntity> players, SoundEvent sound) {
        for (ServerPlayerEntity player : players) {
            if (player.getWorld() != arenaWorld) continue;
            player.playSoundToPlayer(sound, SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    private static final SoundEvent SHRINK_START_SOUND = SoundEvents.ENTITY_WITHER_SPAWN;
    private static final SoundEvent SHRINK_STOP_SOUND = SoundEvents.BLOCK_BEACON_DEACTIVATE;

    private void advancePhaseIfNeeded(long now) {
        if (phaseIndex >= PHASES.size()) return;
        ZonePhase phase = PHASES.get(phaseIndex);
        long elapsed = now - phaseStartTime;

        if (waiting) {
            if (elapsed >= phase.waitDurationMs()) {
                waiting = false;
                phaseStartTime = now;
                shrinkStartRadius = getCurrentRadius(now);
            }
        } else if (elapsed >= phase.shrinkDurationMs()) {
            shrinkStartRadius = getCurrentRadius(now); //testing to see if fixes
            phaseIndex++;
            waiting = true;
            phaseStartTime = now;
        }
    }

    private double getCurrentRadius(long now) {
        if (phaseIndex >= PHASES.size()) return PHASES.get(PHASES.size() - 1).endRadius();
        ZonePhase phase = PHASES.get(phaseIndex);
        if (waiting) return shrinkStartRadius;
        double t = Math.min(1.0, (double) (now - phaseStartTime) / phase.shrinkDurationMs());
        return shrinkStartRadius + (phase.endRadius() - shrinkStartRadius) * t;
    }

    private ZonePhase currentPhase() {
        return PHASES.get(Math.min(phaseIndex, PHASES.size() - 1));
    }

    private void applyZoneDamage(List<ServerPlayerEntity> players, double radius) {
        ZonePhase phase = currentPhase();
        double damageThreshold = radius + phase.buffer();
        Class<?> titanClass = resolveShifterTitanClass();

        for (ServerPlayerEntity player : players) {
            if (player.getWorld() != arenaWorld) continue;
            if (distanceFromCenter(player) <= damageThreshold) continue;

            player.damage(arenaWorld.getDamageSources().create(ZONE_DAMAGE_TYPE), phase.damagePerSecond());

            Entity vehicle = player.getVehicle();
            if (titanClass != null && vehicle instanceof LivingEntity livingVehicle && titanClass.isInstance(vehicle)) {
                float titanDamage = (float) (livingVehicle.getMaxHealth() * phase.titanDamagePercentPerSecond());
                livingVehicle.damage(arenaWorld.getDamageSources().create(ZONE_DAMAGE_TYPE), titanDamage);
            }
        }
    }

    private void sendActionBar(List<ServerPlayerEntity> players, double radius, long now) {
        ZonePhase phase = currentPhase();
        Text phaseText = buildPhaseTimerText(phase, now);

        for (ServerPlayerEntity player : players) {
            if (player.getWorld() != arenaWorld) continue;

            ServerPlayNetworking.send(player, new ZoneUpdatePayload(centerX, centerZ, radius));

            double dist = distanceFromCenter(player);
            Text statusText;
            if (dist <= radius) {
                statusText = Text.literal("");
            } else if (dist <= radius + phase.buffer()) {
                statusText = Text.literal("⚠ At the edge of the Rumbling! ").formatted(Formatting.RED);
            } else {
                statusText = Text.literal("☠ Taking damage from the Rumbling! ").formatted(Formatting.DARK_RED);
            }

            player.sendMessage(statusText.copy().append(phaseText), true); // true = action bar
        }
    }

    private Text buildPhaseTimerText(ZonePhase phase, long now) {
        if (phaseIndex >= PHASES.size()) {
            return Text.literal("The Rumbling has fully closed in.").formatted(Formatting.DARK_RED);
        }
        long remainingSeconds = (waiting
                ? phase.waitDurationMs() - (now - phaseStartTime)
                : phase.shrinkDurationMs() - (now - phaseStartTime)) / 1000;

        String timeText = formatTime(remainingSeconds);

        return waiting
                ? Text.literal("Rumbling begins in " + timeText).formatted(Formatting.YELLOW)
                : Text.literal("Rumbling is activated! " + timeText).formatted(Formatting.RED);
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static final double RENDER_RADIUS = 400;
    private static final double POINT_SPACING = 4; // much denser now that it's cheap
    private static final int COLUMN_HEIGHT = 100;
    private static final int PARTICLES_PER_COLUMN = 40; // one call spreads all 40 through the column

    private void renderZoneRing(double radius, List<ServerPlayerEntity> players) {
        if (players.isEmpty()) return;

        int totalPoints = Math.max(64, (int) (2 * Math.PI * radius / POINT_SPACING));

        for (int i = 0; i < totalPoints; i++) {
            double angle = 2 * Math.PI * i / totalPoints;
            double x = centerX + radius * Math.cos(angle);
            double z = centerZ + radius * Math.sin(angle);

            int surfaceY = arenaWorld.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) x, (int) z);
            double columnCenterY = surfaceY + (COLUMN_HEIGHT / 2.0);

            for (ServerPlayerEntity player : players) {
                if (player.getWorld() != arenaWorld) continue;
                double dx = player.getX() - x, dz = player.getZ() - z;
                if (dx * dx + dz * dz > RENDER_RADIUS * RENDER_RADIUS) continue;

                arenaWorld.spawnParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, columnCenterY, z,
                        PARTICLES_PER_COLUMN, 1.0, COLUMN_HEIGHT / 2.0, 1.0, 0.01);
                arenaWorld.spawnParticles(player, ParticleTypes.FLAME, true, x + 1.0, columnCenterY, z + 1.0,
                        PARTICLES_PER_COLUMN, 1.0, COLUMN_HEIGHT / 2.0, 1.0, 0.01);
                arenaWorld.spawnParticles(player, ParticleTypes.CLOUD, true, x + 0.5, columnCenterY, z + 0.5,
                        PARTICLES_PER_COLUMN, 1.0, COLUMN_HEIGHT / 2.0, 1.0, 0.01);
//                arenaWorld.spawnParticles(player, ParticleTypes.EXPLOSION, true, x, surfaceY, z,
//                        1, 1.0, 10.0 / 2.0, 1.0, 0.00001);
            }
        }
    }



    private double distanceFromCenter(ServerPlayerEntity player) {
        double dx = player.getX() - centerX, dz = player.getZ() - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
