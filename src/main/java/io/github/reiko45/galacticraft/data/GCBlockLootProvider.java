package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.Set;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block drops, following 1.12.2 getItemDropped/quantityDropped. */
public class GCBlockLootProvider extends BlockLootSubProvider {
    public GCBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            if (block == GCBlocks.GLOWSTONE_WALL_TORCH.get() || block == GCBlocks.UNLIT_WALL_TORCH.get() || block == GCBlocks.SPACE_WART.get()
                    || block == GCBlocks.MINER_BASE_FULL.get()
                    || block.getLootTable() == BuiltInLootTables.EMPTY) {
                continue; // wall torches use the standing torch's table; solar parts, liquids and breathable air drop nothing
            }
            if (block instanceof net.minecraft.world.level.block.SlabBlock) {
                add(block, createSlabItemTable(block));
            } else {
                dropSelf(block);
            }
        }
        // 1.12.2 BlockMinerBaseFull#getDrops: a built base breaks back into the eight blocks it was made of
        add(GCBlocks.MINER_BASE_FULL.get(), block -> LootTable.lootTable().withPool(applyExplosionCondition(block, net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(GCBlocks.MINER_BASE.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(8.0F)))))));
        // 1.12.2 BlockSpaceWart is BlockNetherWart: its drops are nether wart's
        final var fortuneLevel = this.registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        add(GCBlocks.SPACE_WART.get(), block -> LootTable.lootTable().withPool(applyExplosionDecay(block, net.minecraft.world.level.storage.loot.LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(net.minecraft.world.item.Items.NETHER_WART)
                        .apply(SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(2.0F, 4.0F))
                                .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(net.minecraft.world.level.block.NetherWartBlock.AGE, 3))))
                        .apply(net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.addUniformBonusCount(fortuneLevel)
                                .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(net.minecraft.world.level.block.NetherWartBlock.AGE, 3))))))));
        // Ores that drop items (fortune applies, like 1.12.2 quantityDropped).
        add(GCBlocks.SILICON_ORE.get(), block -> createOreDrop(block, GCItems.RAW_SILICON.get()));
        add(GCBlocks.MOON_CHEESE_ORE.get(), block -> createOreDrop(block, GCItems.CHEESE_CURD.get()));
        add(GCBlocks.MOON_SAPPHIRE_ORE.get(), block -> createOreDrop(block, GCItems.LUNAR_SAPPHIRE.get()));
        // Mars (1.12.2 BlockBasicMars): stone breaks to cobblestone; desh ore gives Raw Desh, and with Fortune a chance of
        // a second one of (level x 0.29 - 0.25), i.e. 4% / 33% / 62% (quantityDropped).
        add(GCBlocks.MARS_STONE.get(), block -> createSingleItemTableWithSilkTouch(block, GCBlocks.MARS_COBBLESTONE.get()));
        final var fortune = this.registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        add(GCBlocks.DESH_ORE.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(GCItems.RAW_DESH.get()).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1), true)
                        .when(net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition.bonusLevelFlatChance(fortune,
                                0.0F, 0.04F, 0.33F, 0.62F, 0.91F))))));
        // Asteroids (1.12.2 BlockBasicAsteroids#getDrops): ilmenite gives an Iron Shard and a Titanium Shard, each with the
        // same Fortune chance of a second one as desh. Dense Ice drops only with Silk Touch (BlockIceAsteroids).
        add(GCBlocks.ILMENITE_ORE.get(), block -> {
            final LootTable.Builder table = LootTable.lootTable();
            for (net.minecraft.world.item.Item shard : new net.minecraft.world.item.Item[]{GCItems.IRON_SHARD.get(), GCItems.TITANIUM_SHARD.get()}) {
                table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(
                        net.minecraft.world.level.storage.loot.entries.AlternativesEntry.alternatives(
                                LootItem.lootTableItem(block).when(hasSilkTouch()),
                                applyExplosionDecay(block, LootItem.lootTableItem(shard).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1), true)
                                        .when(net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition.bonusLevelFlatChance(fortune,
                                                0.0F, 0.04F, 0.33F, 0.62F, 0.91F)))))));
            }
            return table;
        });
        add(GCBlocks.DENSE_ICE.get(), createSilkTouchOnlyTable(GCBlocks.DENSE_ICE.get()));
        // 1.12.2 BlockCavernousVine dropped nothing unless sheared.
        add(GCBlocks.CAVERNOUS_VINES.get(), createShearsOnlyDrop(GCBlocks.CAVERNOUS_VINES.get()));
        // 1.12.2 BlockFluidPipe#breakBlock: a dyed pipe also gives its dye back.
        add(GCBlocks.FLUID_PIPE.get(), block -> {
            final LootPool.Builder dye = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
            for (DyeColor color : DyeColor.values()) {
                if (color != DyeColor.WHITE) {
                    dye.add(LootItem.lootTableItem(DyeItem.byColor(color)).when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FluidPipeBlock.COLOR, color))));
                }
            }
            return createSingleItemTable(block).withPool(applyExplosionCondition(block, dye));
        });
        // Cheese is eaten, not mined (same as cake).
        add(GCBlocks.CHEESE_BLOCK.get(), noDrop());
        // 1.12.2: 1 + (int)(rand + 0.75) raw meteoric iron, i.e. 1 plus a 75% chance of a second.
        add(GCBlocks.FALLEN_METEOR.get(), block -> LootTable.lootTable().withPool(applyExplosionCondition(block,
                LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(GCItems.METEORIC_IRON_RAW.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                        .apply(SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(1, 0.75F), true))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return GCBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).map(Block.class::cast)::iterator;
    }
}
