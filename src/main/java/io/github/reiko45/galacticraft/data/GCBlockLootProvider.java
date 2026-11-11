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
            if (block == GCBlocks.GLOWSTONE_WALL_TORCH.get() || block == GCBlocks.UNLIT_WALL_TORCH.get() || block.getLootTable() == BuiltInLootTables.EMPTY) {
                continue; // wall torches use the standing torch's table; solar parts, liquids and breathable air drop nothing
            }
            if (block instanceof net.minecraft.world.level.block.SlabBlock) {
                add(block, createSlabItemTable(block));
            } else {
                dropSelf(block);
            }
        }
        // Ores that drop items (fortune applies, like 1.12.2 quantityDropped).
        add(GCBlocks.SILICON_ORE.get(), block -> createOreDrop(block, GCItems.RAW_SILICON.get()));
        add(GCBlocks.MOON_CHEESE_ORE.get(), block -> createOreDrop(block, GCItems.CHEESE_CURD.get()));
        add(GCBlocks.MOON_SAPPHIRE_ORE.get(), block -> createOreDrop(block, GCItems.LUNAR_SAPPHIRE.get()));
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
