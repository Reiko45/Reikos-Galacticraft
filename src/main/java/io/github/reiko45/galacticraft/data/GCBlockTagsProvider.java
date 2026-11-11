package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mining tags replace 1.12.2 {@code setHarvestLevel} (0 wood, 1 stone, 2 iron, 3 diamond),
 * see {@code GCBlocks#setHarvestLevels} in Galacticraft Legacy.
 */
public class GCBlockTagsProvider extends BlockTagsProvider {
    public GCBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredHolder<Block, ? extends Block> holder : GCBlocks.BLOCKS.getEntries()) {
            final Block block = holder.get();
            if (block instanceof StairBlock) {
                tag(BlockTags.STAIRS).add(block);
            } else if (block instanceof SlabBlock) {
                tag(BlockTags.SLABS).add(block);
            } else if (block instanceof WallBlock) {
                tag(BlockTags.WALLS).add(block);
            }
        }

        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(GCBlocks.MOON_TURF.get(), GCBlocks.MOON_DIRT.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                GCBlocks.TIN_ORE.get(), GCBlocks.ALUMINUM_ORE.get(), GCBlocks.SILICON_ORE.get(),
                GCBlocks.TIN_BLOCK.get(), GCBlocks.ALUMINUM_BLOCK.get(), GCBlocks.METEORIC_IRON_BLOCK.get(), GCBlocks.SILICON_BLOCK.get(),
                GCBlocks.TIN_DECORATION_BLOCK_1.get(), GCBlocks.TIN_DECORATION_BLOCK_2.get(),
                GCBlocks.TIN_DECORATION_STAIRS_1.get(), GCBlocks.TIN_DECORATION_STAIRS_2.get(),
                GCBlocks.TIN_DECORATION_SLAB_1.get(), GCBlocks.TIN_DECORATION_SLAB_2.get(),
                GCBlocks.TIN_DECORATION_WALL_1.get(), GCBlocks.TIN_DECORATION_WALL_2.get(),
                GCBlocks.MOON_ROCK.get(), GCBlocks.MOON_ROCK_STAIRS.get(), GCBlocks.MOON_ROCK_SLAB.get(), GCBlocks.MOON_ROCK_WALL.get(),
                GCBlocks.MOON_DUNGEON_BRICK.get(), GCBlocks.MOON_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MOON_DUNGEON_BRICK_SLAB.get(),
                GCBlocks.MOON_DUNGEON_BRICK_WALL.get(),
                GCBlocks.MOON_COPPER_ORE.get(), GCBlocks.MOON_TIN_ORE.get(), GCBlocks.MOON_CHEESE_ORE.get(), GCBlocks.MOON_SAPPHIRE_ORE.get(),
                GCBlocks.FALLEN_METEOR.get());

        tag(BlockTags.NEEDS_STONE_TOOL).add(
                GCBlocks.TIN_ORE.get(), GCBlocks.ALUMINUM_ORE.get(),
                GCBlocks.MOON_COPPER_ORE.get(), GCBlocks.MOON_TIN_ORE.get(), GCBlocks.MOON_CHEESE_ORE.get(),
                GCBlocks.TIN_BLOCK.get(), GCBlocks.ALUMINUM_BLOCK.get(), GCBlocks.METEORIC_IRON_BLOCK.get(), GCBlocks.SILICON_BLOCK.get(),
                GCBlocks.TIN_DECORATION_BLOCK_1.get(), GCBlocks.TIN_DECORATION_BLOCK_2.get(),
                GCBlocks.TIN_DECORATION_STAIRS_1.get(), GCBlocks.TIN_DECORATION_STAIRS_2.get(),
                GCBlocks.TIN_DECORATION_SLAB_1.get(), GCBlocks.TIN_DECORATION_SLAB_2.get(),
                GCBlocks.TIN_DECORATION_WALL_1.get(), GCBlocks.TIN_DECORATION_WALL_2.get(),
                GCBlocks.MOON_ROCK_STAIRS.get(), GCBlocks.MOON_ROCK_SLAB.get(), GCBlocks.MOON_ROCK_WALL.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(GCBlocks.SILICON_ORE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(GCBlocks.FALLEN_METEOR.get(),
                GCBlocks.MOON_DUNGEON_BRICK.get(), GCBlocks.MOON_DUNGEON_BRICK_STAIRS.get(),
                GCBlocks.MOON_DUNGEON_BRICK_SLAB.get(), GCBlocks.MOON_DUNGEON_BRICK_WALL.get());

        // Convention tags
        tag(GCTags.block("ores/tin")).add(GCBlocks.TIN_ORE.get(), GCBlocks.MOON_TIN_ORE.get());
        tag(GCTags.block("ores/aluminum")).add(GCBlocks.ALUMINUM_ORE.get());
        tag(GCTags.block("ores/silicon")).add(GCBlocks.SILICON_ORE.get());
        tag(GCTags.block("ores/sapphire")).add(GCBlocks.MOON_SAPPHIRE_ORE.get());
        tag(GCTags.block("ores/cheese")).add(GCBlocks.MOON_CHEESE_ORE.get());
        tag(Tags.Blocks.ORES_COPPER).add(GCBlocks.MOON_COPPER_ORE.get());
        tag(Tags.Blocks.ORES).addTags(GCTags.block("ores/tin"), GCTags.block("ores/aluminum"), GCTags.block("ores/silicon"),
                GCTags.block("ores/sapphire"), GCTags.block("ores/cheese")).add(GCBlocks.MOON_COPPER_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(GCBlocks.TIN_ORE.get(), GCBlocks.ALUMINUM_ORE.get(), GCBlocks.SILICON_ORE.get());

        tag(GCTags.block("storage_blocks/tin")).add(GCBlocks.TIN_BLOCK.get());
        tag(GCTags.block("storage_blocks/aluminum")).add(GCBlocks.ALUMINUM_BLOCK.get());
        tag(GCTags.block("storage_blocks/meteoric_iron")).add(GCBlocks.METEORIC_IRON_BLOCK.get());
        tag(GCTags.block("storage_blocks/silicon")).add(GCBlocks.SILICON_BLOCK.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).addTags(GCTags.block("storage_blocks/tin"), GCTags.block("storage_blocks/aluminum"),
                GCTags.block("storage_blocks/meteoric_iron"), GCTags.block("storage_blocks/silicon"));
    }
}
