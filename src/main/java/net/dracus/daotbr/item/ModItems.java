package net.dracus.daotbr.item;

import net.dracus.daotbr.DAOTBR;
import net.dracus.daotbr.item.Airdrops.ShifterAirdropItem;
import net.dracus.daotbr.item.BRItems.APGPackageItem;
import net.dracus.daotbr.item.BRItems.ShieldPotionItem;
import net.dracus.daotbr.item.BRItems.ThunderSpearPackageItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {



    //daotbr items added
    public static final Item THUNDER_SPEAR_PACKAGE = registerItem("thunder_spear_package", new ThunderSpearPackageItem(new Item.Settings().maxCount(1)));
    public static final Item APG_PACKAGE = registerItem("apg_package", new APGPackageItem(new Item.Settings().maxCount(1)));
    public static final Item SHIELD_POTION = registerItem("shield_potion", new ShieldPotionItem(new Item.Settings().maxCount(2)));

    public static final Item SHIFTER_AIRDROP = registerItem("shifter_airdrop", new ShifterAirdropItem(new Item.Settings().maxCount(1).fireproof()));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(DAOTBR.MOD_ID, name), item);
    }

    public static void  registerModItems() {
        DAOTBR.LOGGER.info("Registering Mod Items for " + DAOTBR.MOD_ID);
    }
}
