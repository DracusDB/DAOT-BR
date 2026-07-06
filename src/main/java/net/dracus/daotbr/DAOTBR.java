package net.dracus.daotbr;

import net.dracus.daotbr.item.Airdrops.ShifterAirdropManager;
import net.dracus.daotbr.item.BRFeatures.FlareGunListener;
import net.dracus.daotbr.item.ModItemGroups;
import net.dracus.daotbr.item.ModItems;
import net.dracus.daotbr.util.ModLootTableModifiers;
import net.fabricmc.api.ModInitializer;

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

		ShifterAirdropManager.init();
		ShifterAirdropManager.initWaypointScheduler();
		FlareGunListener.register();

		ModLootTableModifiers.modifyLootTables();

	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
