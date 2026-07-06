package net.dracus.daotbr;

import net.dracus.daotbr.item.Airdrops.ShifterAirdropManager;
import net.dracus.daotbr.item.BRFeatures.FlareGunListener;
import net.dracus.daotbr.item.ModItemGroups;
import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.ModLootTableModifiers;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

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


		// Two apparent ways to do things on death, using the entity events or a mixin IE // Source - https://stackoverflow.com/a/73941878
		// @Inject(method="onDeath", at=@At("TAIL"))
		// This uses Server Entity Events

		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				MinecraftServer server = player.getServer();
				if (server == null) return;

				String name = player.getName().getString();

				// Resolve the target dimension. If this logs a warning, the ID is wrong
				RegistryKey<World> paradisKey =
						RegistryKey.of(RegistryKeys.WORLD, Identifier.of("dannys-aot", "paradis"));
				ServerWorld paradis = server.getWorld(paradisKey);
				if (paradis == null) {
					LOGGER.warn("Dimension {} not found — cannot teleport {}", paradisKey.getValue(), name);
				}

				player.teleport(paradis, 0, 64, 0, java.util.Set.of(), 0, 0);
				player.changeGameMode(GameMode.SPECTATOR);
				server.getCommandManager().executeWithPrefix(server.getCommandSource(), "team leave " + name);

				LOGGER.info("{} died, teleported to {}, and was set to spectator", name, paradisKey.getValue());
			}
		});
		ShifterAirdropManager.init();
		ShifterAirdropManager.initWaypointScheduler();
		FlareGunListener.register();

		ModLootTableModifiers.modifyLootTables();

	}


	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
