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
    public static final ItemGroup SHIFTER_SYRINGES_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(DAOTBR.MOD_ID, "shifter_syringes_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.ATTACK_SYRINGE))
                    .displayName(Text.translatable("itemgroup.daotbr.shifter_syringes_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.ACKERMAN_SYRINGE);
                        entries.add(ModItems.ARMORED_SYRINGE);
                        entries.add(ModItems.ATTACK_SYRINGE);
                        entries.add(ModItems.BEAST_SYRINGE);
                        entries.add(ModItems.CART_SYRINGE);
                        entries.add(ModItems.COLOSSAL_SYRINGE);
                        entries.add(ModItems.FEMALE_SYRINGE);
                        entries.add(ModItems.JAW_SYRINGE);
                        entries.add(ModItems.WARHAMMER_SYRINGE);
                    })

                    .build());

    public static final ItemGroup DAOTBR_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(DAOTBR.MOD_ID, "daotbr_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.BR_START))
                    .displayName(Text.translatable("itemgroup.daotbr.daotbr_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.THUNDER_SPEAR_PACKAGE);
                        entries.add(ModItems.APG_PACKAGE);
                        entries.add(ModItems.BR_START);
                        entries.add(ModItems.SHIFTER_AIRDROP);
                    })

                    .build());


    public static void registerItemGroups() {
        DAOTBR.LOGGER.info("Registering Item Groups for " + DAOTBR.MOD_ID);
    }
}
