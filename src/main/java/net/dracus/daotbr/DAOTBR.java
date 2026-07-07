package net.dracus.daotbr;

import net.dracus.daotbr.item.Airdrops.ShifterAirdropManager;
import net.dracus.daotbr.util.BRFeatures.FlareGunListener;
import net.dracus.daotbr.item.ModItemGroups;
import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.BRFeatures.GameQueueManager;
import net.dracus.daotbr.util.BRFeatures.LobbyManager;
import net.dracus.daotbr.util.ModLootTableModifiers;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOTBR implements ModInitializer {
	public static final String MOD_ID = "daotbr";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		registerDeathHandler();

		ShifterAirdropManager.init();
		ShifterAirdropManager.initWaypointScheduler();
		FlareGunListener.register();

		ModLootTableModifiers.modifyLootTables();

		LobbyManager.register();
		GameQueueManager.register();


	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}


	// Set to spectator on death
	private static void registerDeathHandler(){
		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				MinecraftServer server = player.getServer();
				if (server == null) return;

				String name = player.getName().getString();
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "team leave " + name);
				player.changeGameMode(GameMode.SPECTATOR);

//				double targetY = player.getY();
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
						"execute in dannys-aot:paradis run tp " + name + " 0 " + 100 + " 0");

				// TP wasn't good enough, had to set respawn
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
						"execute in dannys-aot:paradis run spawnpoint " + name + " 0 " + 100 + " 0");
			}
		});

	}


	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
