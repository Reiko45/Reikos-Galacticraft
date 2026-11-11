package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.dungeon.DungeonConfig;
import io.github.reiko45.galacticraft.world.dungeon.DungeonPiece;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 1.12.2 loot_tables/dungeon_tier_1.json and dungeon_tier_2.json: the Moon and Mars dungeon chests and treasure chests
 * (5-8 and 6-9 rolls). Metadata items are now separate items; the "oil_canister_partial" at damage 1001 was an empty
 * Oil Canister. Tier 2's Carbon Fragments (weight 10) come with the Asteroids and are left out until then.
 */
public class GCChestLootProvider implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    public GCChestLootProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(DungeonPiece.MOON_CHEST, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(UniformGenerator.between(5.0F, 8.0F))
                .add(item(GCItems.CHEESE_CURD.get(), 10, 1, 4))
                .add(item(Items.IRON_INGOT, 10, 2, 7))
                .add(item(GCItems.DEHYDRATED_APPLE.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_CARROT.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_MELON.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_POTATO.get(), 6, 1, 2))
                .add(item(GCItems.OIL_CANISTER.get(), 5))
                .add(item(Items.GOLDEN_APPLE, 15))
                .add(item(Items.ENCHANTED_GOLDEN_APPLE, 2))
                .add(item(Items.REDSTONE, 10, 2, 7))
                .add(item(Items.MUSIC_DISC_BLOCKS, 4))
                .add(item(Items.MUSIC_DISC_FAR, 4))
                .add(item(GCItems.HEAVY_DUTY_SHOVEL.get(), 10))
                .add(item(GCItems.LUNAR_SAPPHIRE.get(), 2, 1, 2))
                .add(item(GCItems.METEORIC_IRON_RAW.get(), 5, 1, 5))
                .add(item(GCItems.FREQUENCY_MODULE.get(), 1))
                .add(item(Items.BOOK, 8).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))
                .add(item(Items.BOOK, 8).apply(EnchantRandomlyFunction.randomApplicableEnchantment(this.registries)))));
        // "treasure: true" books are now the on_random_loot options, the default of enchantWithLevels
        output.accept(DungeonConfig.MARS.loot(), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(UniformGenerator.between(6.0F, 9.0F))
                .add(item(GCItems.DEHYDRATED_APPLE.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_CARROT.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_MELON.get(), 6, 1, 2))
                .add(item(GCItems.DEHYDRATED_POTATO.get(), 6, 1, 2))
                .add(item(GCItems.OIL_CANISTER.get(), 5))
                .add(item(GCItems.DESH_BOOTS.get(), 2))
                .add(item(GCItems.DESH_CHESTPLATE.get(), 2))
                .add(item(GCItems.DESH_HELMET.get(), 2))
                .add(item(GCItems.DESH_LEGGINGS.get(), 2))
                .add(item(Items.REDSTONE, 10, 2, 7))
                .add(item(Items.MUSIC_DISC_MALL, 4))
                .add(item(Items.MUSIC_DISC_MELLOHI, 4))
                .add(item(GCItems.RAW_DESH.get(), 5, 1, 2))
                .add(item(GCItems.DESH_STICK.get(), 5))
                .add(item(Items.BOOK, 8).apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(22.0F))))
                .add(item(Items.BOOK, 8).apply(EnchantWithLevelsFunction.enchantWithLevels(this.registries, ConstantValue.exactly(24.0F))))));
    }

    private static LootPoolSingletonContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
        return item(item, weight).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}
