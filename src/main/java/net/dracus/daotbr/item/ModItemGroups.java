package net.dracus.daotbr.item;

import net.dracus.daotbr.DAOTBR;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup DAOTBR_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(DAOTBR.MOD_ID, "daotbr_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.SHIFTER_AIRDROP))
                    .displayName(Text.translatable("itemgroup.daotbr.daotbr_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.THUNDER_SPEAR_PACKAGE);
                        entries.add(ModItems.APG_PACKAGE);
                        entries.add(ModItems.SHIFTER_AIRDROP);
                        entries.add(ModItems.SHIELD_POTION);
                    })

                    .build());


    public static void registerItemGroups() {
        DAOTBR.LOGGER.info("Registering Item Groups for " + DAOTBR.MOD_ID);
    }
}
