package net.dracus.daotbr.item;

import net.dracus.daotbr.DAOTBR;
import net.dracus.daotbr.item.Airdrops.ShifterAirdropItem;
import net.dracus.daotbr.item.BRItems.APGPackageItem;
import net.dracus.daotbr.item.BRItems.BRStartItem;
import net.dracus.daotbr.item.BRItems.ThunderSpearPackageItem;
import net.dracus.daotbr.item.Syringes.*;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    //syringes added
    public static final Item ACKERMAN_SYRINGE = registerItem("ackerman_syringe", new AckermanSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item ARMORED_SYRINGE = registerItem("armored_syringe", new ArmoredSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item ATTACK_SYRINGE = registerItem("attack_syringe", new AttackSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item BEAST_SYRINGE = registerItem("beast_syringe", new BeastSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item CART_SYRINGE = registerItem("cart_syringe", new CartSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item COLOSSAL_SYRINGE = registerItem("colossal_syringe", new ColossalSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item FEMALE_SYRINGE = registerItem("female_syringe", new FemaleSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item JAW_SYRINGE = registerItem("jaw_syringe", new JawSyringeItem(new Item.Settings().maxCount(1)));
    public static final Item WARHAMMER_SYRINGE = registerItem("warhammer_syringe", new WarhammerSyringeItem(new Item.Settings().maxCount(1)));

    //daotbr items added
    public static final Item THUNDER_SPEAR_PACKAGE = registerItem("thunder_spear_package", new ThunderSpearPackageItem(new Item.Settings().maxCount(1)));
    public static final Item APG_PACKAGE = registerItem("apg_package", new APGPackageItem(new Item.Settings().maxCount(1)));
    public static final Item BR_START = registerItem("br_start", new BRStartItem(new Item.Settings().maxCount(1)));

    public static final Item SHIFTER_AIRDROP = registerItem("shifter_airdrop", new ShifterAirdropItem(new Item.Settings().maxCount(1)));


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(DAOTBR.MOD_ID, name), item);
    }

    public static void  registerModItems() {
        DAOTBR.LOGGER.info("Registering Mod Items for " + DAOTBR.MOD_ID);
    }
}
