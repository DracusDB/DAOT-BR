package net.dracus.daotbr.util.BRFeatures;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class GameQueueManager {
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final int COUNTDOWN_SECONDS = 5;

    private static final Set<UUID> readyPlayers = new HashSet<>();
    private static boolean countdownActive = false;
    private static int countdownTicksRemaining = 0;

    public static void register() {
        CommandRegistrationCallback.EVENT.register(GameQueueManager::registerCommands);
        ServerTickEvents.END_SERVER_TICK.register(GameQueueManager::onServerTick);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         net.minecraft.command.CommandRegistryAccess registryAccess,
                                         net.minecraft.server.command.CommandManager.RegistrationEnvironment env) {

        dispatcher.register(literal("daotbr")
                        .then(literal("ready")

                .executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;

            if (!LobbyManager.isInLobby(player.getWorld())) {
                player.sendMessage(Text.literal("You must be in the lobby to ready up.").formatted(Formatting.RED), false);
                return 0;
            }

            readyPlayers.add(player.getUuid());

            Scoreboard scoreboard = ctx.getSource().getServer().getScoreboard();
            Team readyTeam = getOrCreateReadyTeam(ctx.getSource().getServer());
            scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), readyTeam);

            broadcastStatus(ctx.getSource().getServer());
            checkReadyState(ctx.getSource().getServer());
            return 1;
        })));

        dispatcher.register(literal("daotbr")
                        .then(literal("unready")

                .executes(ctx -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return 0;

            readyPlayers.remove(player.getUuid());

            Scoreboard scoreboard = ctx.getSource().getServer().getScoreboard();
            scoreboard.removeScoreHolderFromTeam(player.getNameForScoreboard(), scoreboard.getTeam(READY_TEAM_NAME));

            if (countdownActive) {
                countdownActive = false;
                ctx.getSource().getServer().getPlayerManager().broadcast(
                        Text.literal("Countdown cancelled - a player unreadied.").formatted(Formatting.YELLOW), false);
            }
            broadcastStatus(ctx.getSource().getServer());
            return 1;
        })));

        dispatcher.register(literal("daotbr")
                .then(literal("start")
                        .requires(source -> source.hasPermissionLevel(2)) // OP level 2+
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();

                            // Reset any in-progress queue state so it doesn't fire again after this forced start
                            countdownActive = false;
                            readyPlayers.clear();

                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("Battle royale force-started by admin.").formatted(Formatting.YELLOW),
                                    true // broadcast to other ops
                            );

                            startBattleRoyale(server);
                            return 1;
                        })
                )
        );
    }

    private static final String READY_TEAM_NAME = "ready_daotbr";

    private static Team getOrCreateReadyTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(READY_TEAM_NAME);
        if (team == null) {
            team = scoreboard.addTeam(READY_TEAM_NAME);
            team.setColor(Formatting.GREEN);
            // Optional: stop the team color from also prefixing chat messages
            team.setShowFriendlyInvisibles(false);
        }
        return team;
    }

    private static void broadcastStatus(MinecraftServer server) {
        int lobbyCount = server.getCurrentPlayerCount();
        server.getPlayerManager().broadcast(
                Text.literal("Ready: " + readyPlayers.size() + "/" + lobbyCount).formatted(Formatting.AQUA), false);
    }

    private static void checkReadyState(MinecraftServer server) {
        int lobbyCount = getLobbyPlayerCount(server);

        if (lobbyCount < MIN_PLAYERS_TO_START) return;
        if (readyPlayers.size() < lobbyCount) return;

        //Everyone in lobby is ready:
        if (!countdownActive) {
            countdownActive = true;
            countdownTicksRemaining = COUNTDOWN_SECONDS * 20;
            server.getPlayerManager().broadcast(
                    Text.literal("All players are ready! Beginning match in " + COUNTDOWN_SECONDS + " seconds").formatted(Formatting.GREEN), false);
        }
    }

    private static int getLobbyPlayerCount(MinecraftServer server) {
        ServerWorld lobbyWorld = server.getWorld(LobbyManager.LOBBY_DIMENSION);
        if (lobbyWorld == null) return 0;
        return lobbyWorld.getPlayers().size();
    }

    private static void onServerTick(MinecraftServer server) {
        if (!countdownActive) return;

        if (countdownTicksRemaining % 20 == 0) {
            int secondsLeft = countdownTicksRemaining / 20;
            if (secondsLeft <= 5 || secondsLeft % 5 == 0) {
                server.getPlayerManager().broadcast(
                        Text.literal("Battle royale starts in " + secondsLeft).formatted(Formatting.GOLD), false);
            }
        }

        countdownTicksRemaining--;

        if (countdownTicksRemaining <= 0) {
            countdownActive = false;
            readyPlayers.clear();
            startBattleRoyale(server);
        }
    }

    public static void startBattleRoyale(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Team readyTeam = scoreboard.getTeam(READY_TEAM_NAME);
        if (readyTeam != null) {
            // Copy to a new list first - removing while iterating the team's own member set will throw
            for (String name : new java.util.ArrayList<>(readyTeam.getPlayerList())) {
                scoreboard.removeScoreHolderFromTeam(name, readyTeam);
            }
        }

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
                "gamerule fairTitanPowerLoss true",
                "gamerule unfairPureTitans false",
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
        }
    }
}
