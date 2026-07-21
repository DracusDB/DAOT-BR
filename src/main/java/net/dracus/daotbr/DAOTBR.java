package net.dracus.daotbr;

import net.dracus.daotbr.item.Airdrops.ShifterAirdropManager;
import net.dracus.daotbr.util.BRFeatures.FlareGunListener;
import net.dracus.daotbr.item.ModItemGroups;
import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.BRFeatures.GameQueueManager;
import net.dracus.daotbr.util.ModLootTableModifiers;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.dracus.daotbr.util.BRFeatures.GameStageManager;
import net.dracus.daotbr.network.ZoneUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

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

		PayloadTypeRegistry.playS2C().register(ZoneUpdatePayload.ID, ZoneUpdatePayload.CODEC);

		ShifterAirdropManager.init();
		ShifterAirdropManager.initWaypointScheduler();
		ShifterAirdropManager.initRandomDropScheduler();
		FlareGunListener.register();

		ModLootTableModifiers.modifyLootTables();

		GameQueueManager.register();
		GameStageManager.register();

	}


	// Set to spectator on death
	private static void registerDeathHandler(){
		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				MinecraftServer server = player.getServer();
				if (server == null) return;

				String name = player.getName().getString();
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "team leave " + name);
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
						"execute in dannys-aot:paradis run tp " + name + " 0 " + 100 + " 0");

				// TP wasn't good enough, had to set respawn
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),
						"execute in dannys-aot:paradis run spawnpoint " + name + " 0 " + 100 + " 0");

				GameStageManager.onPlayerEliminated(player);
			}
		});

	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
