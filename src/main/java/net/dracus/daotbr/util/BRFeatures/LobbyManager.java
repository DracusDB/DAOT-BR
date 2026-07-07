package net.dracus.daotbr.util.BRFeatures;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LobbyManager {
    public static final RegistryKey<World> LOBBY_DIMENSION = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("dannys-aot", "paths")
    );

    private static final Vec3d LOBBY_SPAWN_POS = new Vec3d(0.5, 91, 0.5);
    private static final float LOBBY_SPAWN_YAW = 0f;
    private static final float LOBBY_SPAWN_PITCH = 0f;

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            teleportToLobby(player);
            //later add if statement checking which stage we're in
        }));

        ServerLivingEntityEvents.ALLOW_DEATH.register(((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof PlayerEntity)) return true;
            if (entity.getWorld().isClient()) return true;

            if (isInLobby(entity.getWorld())) {
                    return false;
            }
            return true;
        }));
    }

    private static void teleportToLobby(ServerPlayerEntity player) {
        ServerWorld lobbyWorld = player.getServer().getWorld(LOBBY_DIMENSION);
        if (lobbyWorld == null) {
            return;
        }

        player.teleport(
                lobbyWorld,
                LOBBY_SPAWN_POS.x, LOBBY_SPAWN_POS.y, LOBBY_SPAWN_POS.z,
                java.util.Set.of(),
                LOBBY_SPAWN_YAW, LOBBY_SPAWN_PITCH
        );
    }

    public static boolean isInLobby(World world) {
        return world.getRegistryKey().equals(LOBBY_DIMENSION);
    }
}
