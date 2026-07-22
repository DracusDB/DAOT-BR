package net.dracus.daotbr.util.BRFeatures;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameStageManager {
    // ---- state ----
    private static GameStage currentStage = GameStage.LOBBY;
    private static final Set<ServerPlayerEntity> alivePlayers = new HashSet<>();

    // ---- lobby dimension info ----
    public static final RegistryKey<World> LOBBY_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("dannys-aot", "paths")
    );
    private static final Vec3d LOBBY_SPAWN_POS = new Vec3d(0.5, 91, 0.5);
    private static final float LOBBY_SPAWN_YAW = 0f;
    private static final float LOBBY_SPAWN_PITCH = 0f;

    // ---- arena dimension info ----
    public static final RegistryKey<World> ARENA_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("dannys-aot", "paradis")
    );
    private static final Vec3d ARENA_SPAWN_POS = new Vec3d(0.5, 91, 0.5);
    private static final float ARENA_SPAWN_YAW = 0f;
    private static final float ARENA_SPAWN_PITCH = 0f;

    // ---- battle zone info ----
    private static final double ZONE_CENTER_X = 0.0;
    private static final double ZONE_CENTER_Z = 0.0;
    private static final double ZONE_STARTING_RADIUS = 1700.0;

    private static BattleZoneManager battleZoneManager;

    // ---- return to lobby timer ----
    private static final int RETURN_TO_LOBBY_SECONDS = 15;
    private static boolean returnTimerActive = false;
    private static long returnAtMillis = 0;

    // ---- registration: called once from your mod initializer ----
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            stageBossBar.addPlayer(player);

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource().withSilent(),
                    "execute as " + player.getName().getString() + " run sk choose odm"
            );


            if (currentStage == GameStage.LOBBY) {
                server.execute(() -> {
                    teleportToLobby(player);
                    player.changeGameMode(GameMode.ADVENTURE);
                    player.sendMessage(Text.literal("Welcome to the lobby! Use /daotbr ready to ready up!").formatted(Formatting.GREEN), false);
                });
            } else {
                server.execute(() -> {
                    teleportLateJoinerToArena(player);
                    player.changeGameMode(GameMode.SPECTATOR);
                    player.sendMessage(Text.literal("A game is in progress. You'll have to spectate for now until the next round begins.").formatted(Formatting.RED), false);
                });
            }
        }

        ));

        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> {
            onPlayerDisconnect(handler.getPlayer());
        }));

        ServerLivingEntityEvents.ALLOW_DEATH.register(((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof PlayerEntity)) return true;
            if (entity.getWorld().isClient()) return true;

            if (isInLobby(entity.getWorld())) {
                System.out.println(Text.literal("Is in lobby = true"));
                entity.setHealth(entity.getMaxHealth());
                return false;
            }
            return true;
        }));

        ServerTickEvents.END_SERVER_TICK.register(GameStageManager::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (currentStage == GameStage.ARENA && battleZoneManager != null) {
            ServerWorld arenaWorld = server.getWorld(ARENA_DIMENSION);
            battleZoneManager.tick(arenaWorld, new ArrayList<>(alivePlayers));
        }

        if (!returnTimerActive) return;

        long remainingMillis = returnAtMillis - System.currentTimeMillis();

        if (System.currentTimeMillis() >= returnAtMillis) {
            returnTimerActive = false;
            resetToLobby(server);
            return;
        }

        int remainingSeconds = (int) Math.ceil(remainingMillis / 1000.0);

        if (remainingSeconds != lastAnnouncedReturnSecond
                && (remainingSeconds <= 5 || remainingSeconds % 10 == 0)) {
            lastAnnouncedReturnSecond = remainingSeconds;
            server.getPlayerManager().broadcast(
                    Text.literal("Returning to lobby in " + remainingSeconds + "...").formatted(Formatting.GOLD), false);
        }

    }

    //boss bar showing which stage we're in (for testing)
    private static final ServerBossBar stageBossBar = new ServerBossBar(
            Text.literal("Stage: Lobby"),
            BossBar.Color.GREEN,
            BossBar.Style.PROGRESS
    );

    private static void updateBossBar() {
        switch (currentStage) {
            case LOBBY -> {
                stageBossBar.setName(Text.literal("Stage: Lobby"));
                stageBossBar.setColor(BossBar.Color.GREEN);
            }
            case ARENA -> {
                stageBossBar.setName(Text.literal("Stage: Battle Royale In Progress"));
                stageBossBar.setColor(BossBar.Color.RED);
            }
            case ENDED -> {
                stageBossBar.setName(Text.literal("Stage: Match Ended"));
                stageBossBar.setColor(BossBar.Color.YELLOW);
            }
        }
    }

    // ---- lobby helpers ----
    public static boolean isInLobby(World world) {
        return world.getRegistryKey().equals(LOBBY_DIMENSION);
    }

    private static void teleportToLobby(ServerPlayerEntity player) {
        ServerWorld lobbyWorld = player.getServer().getWorld(LOBBY_DIMENSION);
        if (lobbyWorld == null) return;

        player.teleport(
                lobbyWorld,
                LOBBY_SPAWN_POS.x, LOBBY_SPAWN_POS.y, LOBBY_SPAWN_POS.z,
                Set.of(),
                LOBBY_SPAWN_YAW, LOBBY_SPAWN_PITCH
        );
    }

    // ---- arena / stage helpers ----
    public static GameStage getCurrentStage() {
        return currentStage;
    }

    public static boolean isInArenaStage() {
        return currentStage == GameStage.ARENA;
    }

    public static Set<ServerPlayerEntity> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    // ---- battle zone pass-throughs ----
    public static double getZoneRadius() {
        return battleZoneManager != null ? battleZoneManager.getRadius() : 0;
    }

    public static double getZoneCenterX() {
        return battleZoneManager != null ? battleZoneManager.getCenterX() : 0;
    }

    public static double getZoneCenterZ() {
        return battleZoneManager != null ? battleZoneManager.getCenterZ() : 0;
    }

    public static double getZoneStartingRadius() {
        return ZONE_STARTING_RADIUS;
    }

    public static void setZonePaused(boolean paused) {
        if (battleZoneManager != null) {
            battleZoneManager.setPaused(paused);
        }
    }

    public static boolean isZonePaused() {
        return battleZoneManager != null && battleZoneManager.isPaused();
    }

    /** Called by GameQueueManager once its own teleport/gamemode commands have run. */
    public static void beginArenaStage(MinecraftServer server, Set<ServerPlayerEntity> players) {
        if (currentStage != GameStage.LOBBY) {
            return; // don't let a second start hijack a running match
        }
        alivePlayers.clear();
        alivePlayers.addAll(players);
        currentStage = GameStage.ARENA;
        updateBossBar();

        ServerWorld arenaWorld = server.getWorld(ARENA_DIMENSION);
        battleZoneManager = new BattleZoneManager(arenaWorld, ZONE_CENTER_X, ZONE_CENTER_Z, ZONE_STARTING_RADIUS);
        battleZoneManager.start();
    }

    private static void teleportLateJoinerToArena(ServerPlayerEntity player) {
        // prefer teleporting next to a still-alive player over a fixed empty point
        Set<ServerPlayerEntity> alive = getAlivePlayers();
        if (!alive.isEmpty()) {
            ServerPlayerEntity target = alive.iterator().next();
            player.teleport((ServerWorld) target.getWorld(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYaw(), target.getPitch());
            return;
        }

        ServerWorld arenaWorld = player.getServer().getWorld(ARENA_DIMENSION);
        if (arenaWorld == null) return;

        player.teleport(
                arenaWorld,
                ARENA_SPAWN_POS.x, ARENA_SPAWN_POS.y, ARENA_SPAWN_POS.z,
                Set.of(),
                ARENA_SPAWN_YAW, ARENA_SPAWN_PITCH
        );
    }

    public static void onPlayerEliminated(ServerPlayerEntity player) {
        if (currentStage != GameStage.ARENA) return;

        alivePlayers.remove(player);
        player.changeGameMode(GameMode.SPECTATOR);
        player.sendMessage(Text.literal("You're in spectator mode, use your scroll wheel to increase your speed."));
        checkWinCondition(player.getServer());
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        if (currentStage == GameStage.ARENA) {
            alivePlayers.remove(player);
            checkWinCondition(player.getServer());
        }
    }

    private static void checkWinCondition(MinecraftServer server) {
        if (alivePlayers.size() == 1) {
            endBattleRoyale(server, alivePlayers.iterator().next());
        } else if (alivePlayers.isEmpty()) {
            endBattleRoyale(server, null);
        }
    }

    private static int lastAnnouncedReturnSecond = -1;

    private static void endBattleRoyale(MinecraftServer server, ServerPlayerEntity winner) {
        currentStage = GameStage.ENDED;
        updateBossBar();
        String message = winner != null
                ? winner.getName().getString() + " has won the battle royale!"
                : "The battle royale ended with no survivors.";
        server.getPlayerManager().broadcast(Text.literal(message).formatted(Formatting.GREEN), false);

        Text titleText = Text.literal(winner.getName().getString() + " has won!").formatted(Formatting.GOLD, Formatting.BOLD);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(titleText));
        }

        returnTimerActive = true;
        returnAtMillis = System.currentTimeMillis() + (RETURN_TO_LOBBY_SECONDS * 1000L);
        lastAnnouncedReturnSecond = RETURN_TO_LOBBY_SECONDS;

        server.getPlayerManager().broadcast(
                Text.literal("Returning to lobby in " + RETURN_TO_LOBBY_SECONDS + " seconds...").formatted(Formatting.GOLD), false);

        returnTimerActive = true;
        returnAtMillis = System.currentTimeMillis() + (RETURN_TO_LOBBY_SECONDS * 1000L);
    }

    private static void resetToLobby(MinecraftServer server) {

        // remove the battle teams created at match start
        List<String> teamsToRemove = List.of(
                "Attack", "Armored", "Beast", "Cart", "Colossal",
                "Female", "Jaw", "Warhammer", "Ackerman"
        );
        for (String team : teamsToRemove) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
                    "team remove " + team);
        }

        // bring everyone back to the lobby and reset their mode
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            teleportToLobby(player);
            player.changeGameMode(GameMode.ADVENTURE);
        }

        alivePlayers.clear();
        currentStage = GameStage.LOBBY;
        updateBossBar();

        List<String> commands = List.of(
                "execute in dannys-aot:paradis run kill @e[type=!minecraft:player]",
                "clear @a",
                "daot shifter reset",
                "daot bloodline set @a eldian",
                "sk reset",
                "execute as @a run sk choose odm"
        );

        for (String command : commands) {
            System.out.println("Running command: [" + command + "]");
            server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), command);
        }


        server.getPlayerManager().broadcast(
                Text.literal("Welcome to the lobby! Use /daotbr ready to ready up!").formatted(Formatting.GREEN), false);
    }
}