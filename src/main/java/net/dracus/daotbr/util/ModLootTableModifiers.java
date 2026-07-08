package net.dracus.daotbr.util;

import net.dracus.daotbr.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModLootTableModifiers {

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (key.getValue().getNamespace().equals("minecraft")
                    && key.getValue().getPath().startsWith("chests/")) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .with(ItemEntry.builder(
                                        Registries.ITEM.get(Identifier.of("backpacked", "backpack"))
                                ).weight(1))
                        .with(ItemEntry.builder(
                                        Registries.ITEM.get(Identifier.of("dannys-aot", "blade_component"))
                                ).weight(1)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3))))
                        .with(ItemEntry.builder(
                                        Registries.ITEM.get(Identifier.of("dannys-aot", "ice_burst_cluster"))
                                ).weight(2)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(3, 5))))
                        .with(ItemEntry.builder(
                                        Registries.ITEM.get(Identifier.of("dannys-aot", "apg_cartridge"))
                                ).weight(1)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2, 4))));
                LootPool.Builder poolBuilder1 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.002f))
                        .with(ItemEntry.builder(ModItems.SHIFTER_AIRDROP));

                LootPool.Builder poolBuilder2 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.0009f))
                        .with(ItemEntry.builder(ModItems.ACKERMAN_SYRINGE));

                LootPool.Builder poolBuilder3 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.03f))
                        .with(ItemEntry.builder(ModItems.THUNDER_SPEAR_PACKAGE));

                LootPool.Builder poolBuilder4 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.07f))
                        .with(ItemEntry.builder(ModItems.APG_PACKAGE));

                LootPool.Builder poolBuilder11 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.02f))
                        .with(ItemEntry.builder(ModItems.SHIELD_POTION));



                Item blackCloak = Registries.ITEM.get(Identifier.of("dannys-aot", "black_cloak"));
                Item greenCloak = Registries.ITEM.get(Identifier.of("dannys-aot", "green_cloak"));
                Item royalCloak = Registries.ITEM.get(Identifier.of("dannys-aot", "royal_cloak"));
                Item armorPotion = Registries.ITEM.get(Identifier.of("dannys-aot", "armor_potion"));
                Item redFlareCartridge = Registries.ITEM.get(Identifier.of("dannys-aot", "red_flare_cartridge"));
                Item flareGun = Registries.ITEM.get(Identifier.of("dannys-aot", "flare_gun"));
                LootPool.Builder poolBuilder5 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.01f))
                        .with(ItemEntry.builder(blackCloak));
                LootPool.Builder poolBuilder6 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.01f))
                        .with(ItemEntry.builder(greenCloak));
                LootPool.Builder poolBuilder7 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.006f))
                        .with(ItemEntry.builder(royalCloak));
                LootPool.Builder poolBuilder8 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.009f))
                        .with(ItemEntry.builder(armorPotion));
                LootPool.Builder poolBuilder9 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.009f))
                        .with(ItemEntry.builder(redFlareCartridge));
                LootPool.Builder poolBuilder10 = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.02f))
                        .with(ItemEntry.builder(flareGun));

                tableBuilder.pool(poolBuilder);
                tableBuilder.pool(poolBuilder1);
                tableBuilder.pool(poolBuilder2);
                tableBuilder.pool(poolBuilder3);
                tableBuilder.pool(poolBuilder4);
                tableBuilder.pool(poolBuilder5);
                tableBuilder.pool(poolBuilder6);
                tableBuilder.pool(poolBuilder7);
                tableBuilder.pool(poolBuilder8);
                tableBuilder.pool(poolBuilder9);
                tableBuilder.pool(poolBuilder10);
                tableBuilder.pool(poolBuilder11);
            }
        });
    }
}
