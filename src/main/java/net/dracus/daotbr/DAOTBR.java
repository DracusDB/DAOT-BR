package net.dracus.daotbr;

import net.dracus.daotbr.item.Airdrops.ShifterAirdropManager;
import net.dracus.daotbr.util.BRFeatures.FlareGunListener;
import net.dracus.daotbr.item.ModItemGroups;
import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.BRFeatures.GameQueueManager;
import net.dracus.daotbr.util.BRFeatures.ShifterIncapacitationHandler;
import net.dracus.daotbr.util.ModLootTableModifiers;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.dracus.daotbr.util.BRFeatures.GameStageManager;
import net.dracus.daotbr.network.ZoneUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
		installDefaultStarterKit();

		PayloadTypeRegistry.playS2C().register(ZoneUpdatePayload.ID, ZoneUpdatePayload.CODEC);

		ShifterAirdropManager.init();
		ShifterAirdropManager.initWaypointScheduler();
		ShifterAirdropManager.initRandomDropScheduler();
		ShifterAirdropManager.initChunkUnforceScheduler();
		FlareGunListener.register();
		ShifterIncapacitationHandler.initCooldownScheduler();
		ShifterIncapacitationHandler.register();

		ModLootTableModifiers.modifyLootTables();

		GameQueueManager.register();
		GameStageManager.register();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "sk reload");
		});

	}

	private static void installDefaultStarterKit() {
		Path targetDir = FabricLoader.getInstance().getConfigDir().resolve("starterkit").resolve("kits");
		Path targetFile = targetDir.resolve("ODM.txt");

		if (Files.exists(targetFile)) {
			return; // don't overwrite if it's already there — respects any manual edits
		}

		try {
			Files.createDirectories(targetDir);
			try (InputStream in = DAOTBR.class.getClassLoader().getResourceAsStream("starterkit_defaults/ODM.txt")) {
				if (in == null) {
					LOGGER.warn("Bundled ODM.txt resource not found — cannot install default starter kit.");
					return;
				}
				Files.copy(in, targetFile);
				LOGGER.info("Installed default ODM starter kit to " + targetFile);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to install default ODM starter kit", e);
		}
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
