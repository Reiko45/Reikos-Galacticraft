package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCModTags;
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

        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(GCBlocks.MOON_TURF.get(), GCBlocks.MOON_DIRT.get(),
                GCBlocks.MARS_FINE_REGOLITH.get(), GCBlocks.MARS_REGOLITH.get());
        // Mars (1.12.2 MarsBlocks#setHarvestLevels: ores and stone need stone tools, desh ore and dungeon bricks diamond)
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.MARS_COPPER_ORE.get(), GCBlocks.MARS_TIN_ORE.get(), GCBlocks.MARS_IRON_ORE.get(),
                GCBlocks.DESH_ORE.get(), GCBlocks.MARS_STONE.get(), GCBlocks.MARS_COBBLESTONE.get(), GCBlocks.DESH_BLOCK.get(),
                GCBlocks.MARS_COBBLESTONE_STAIRS.get(), GCBlocks.MARS_COBBLESTONE_SLAB.get(), GCBlocks.MARS_COBBLESTONE_WALL.get(),
                GCBlocks.MARS_DUNGEON_BRICK.get(), GCBlocks.MARS_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MARS_DUNGEON_BRICK_SLAB.get(),
                GCBlocks.MARS_DUNGEON_BRICK_WALL.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(GCBlocks.MARS_COPPER_ORE.get(), GCBlocks.MARS_TIN_ORE.get(), GCBlocks.MARS_IRON_ORE.get(),
                GCBlocks.MARS_STONE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(GCBlocks.DESH_ORE.get(), GCBlocks.MARS_DUNGEON_BRICK.get(),
                GCBlocks.MARS_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MARS_DUNGEON_BRICK_SLAB.get(), GCBlocks.MARS_DUNGEON_BRICK_WALL.get());
        tag(BlockTags.CLIMBABLE).add(GCBlocks.CAVERNOUS_VINES.get());
        // Asteroids (1.12.2 AsteroidBlocks#setHarvestLevels: rock 0, aluminium and iron 2, ilmenite 3)
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.ASTEROID_ROCK_0.get(), GCBlocks.ASTEROID_ROCK_1.get(), GCBlocks.ASTEROID_ROCK_2.get(),
                GCBlocks.ASTEROID_ALUMINUM_ORE.get(), GCBlocks.ILMENITE_ORE.get(), GCBlocks.ASTEROID_IRON_ORE.get(), GCBlocks.DARK_DECORATION_BLOCK.get(),
                GCBlocks.DARK_DECORATION_SLAB.get(), GCBlocks.TITANIUM_BLOCK.get(), GCBlocks.DENSE_ICE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(GCBlocks.ASTEROID_ALUMINUM_ORE.get(), GCBlocks.ASTEROID_IRON_ORE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.WALKWAY.get(), GCBlocks.WALKWAY_WIRE.get(), GCBlocks.WALKWAY_PIPE.get(),
                GCBlocks.MINER_BASE.get(), GCBlocks.MINER_BASE_FULL.get(), GCBlocks.BEAM_REFLECTOR.get(), GCBlocks.BEAM_RECEIVER.get(),
                GCBlocks.SHORT_RANGE_TELEPAD.get(), GCBlocks.TELEPAD_FAKE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(GCBlocks.ILMENITE_ORE.get());
        tag(BlockTags.ICE).add(GCBlocks.DENSE_ICE.get());
        // Venus (1.12.2 VenusBlocks#setHarvestLevels: the four rocks and the spout need stone tools, the rest any pickaxe)
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.VENUS_SOFT_ROCK.get(), GCBlocks.VENUS_HARD_ROCK.get(), GCBlocks.VENUS_VOLCANIC_ROCK.get(),
                GCBlocks.PUMICE.get(), GCBlocks.VENUS_DUNGEON_BRICK_1.get(), GCBlocks.VENUS_DUNGEON_BRICK_2.get(), GCBlocks.VENUS_ALUMINUM_ORE.get(),
                GCBlocks.VENUS_COPPER_ORE.get(), GCBlocks.GALENA_ORE.get(), GCBlocks.VENUS_QUARTZ_ORE.get(), GCBlocks.VENUS_SILICON_ORE.get(),
                GCBlocks.VENUS_TIN_ORE.get(), GCBlocks.LEAD_BLOCK.get(), GCBlocks.SOLAR_ORE.get(), GCBlocks.SCORCHED_VENUS_ROCK.get(),
                GCBlocks.VAPOR_SPOUT.get(), GCBlocks.CRASHED_PROBE.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(GCBlocks.VENUS_SOFT_ROCK.get(), GCBlocks.VENUS_HARD_ROCK.get(), GCBlocks.VENUS_VOLCANIC_ROCK.get(),
                GCBlocks.PUMICE.get(), GCBlocks.VAPOR_SPOUT.get());
        tag(Tags.Blocks.ORES_COPPER).add(GCBlocks.VENUS_COPPER_ORE.get());
        tag(Tags.Blocks.ORES_QUARTZ).add(GCBlocks.VENUS_QUARTZ_ORE.get());
        tag(GCTags.block("ores/aluminum")).add(GCBlocks.VENUS_ALUMINUM_ORE.get());
        tag(GCTags.block("ores/tin")).add(GCBlocks.VENUS_TIN_ORE.get());
        tag(GCTags.block("ores/lead")).add(GCBlocks.GALENA_ORE.get());
        tag(GCTags.block("ores/silicon")).add(GCBlocks.VENUS_SILICON_ORE.get());
        tag(GCTags.block("ores/solar")).add(GCBlocks.SOLAR_ORE.get());
        tag(GCTags.block("storage_blocks/lead")).add(GCBlocks.LEAD_BLOCK.get());
        tag(GCTags.block("ores/aluminum")).add(GCBlocks.ASTEROID_ALUMINUM_ORE.get());
        tag(GCTags.block("ores/ilmenite")).add(GCBlocks.ILMENITE_ORE.get());
        tag(Tags.Blocks.ORES_IRON).add(GCBlocks.ASTEROID_IRON_ORE.get());
        tag(GCTags.block("storage_blocks/titanium")).add(GCBlocks.TITANIUM_BLOCK.get());
        tag(GCTags.block("ores/desh")).add(GCBlocks.DESH_ORE.get());
        tag(Tags.Blocks.ORES_IRON).add(GCBlocks.MARS_IRON_ORE.get());
        tag(GCTags.block("storage_blocks/desh")).add(GCBlocks.DESH_BLOCK.get());
        // 1.12.2 setHarvestLevel(MarsBlocks.rock, "pickaxe", 3): slimeling eggs need a diamond-strength pickaxe to collect
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.TREASURE_CHEST_TIER_2.get(), GCBlocks.RED_SLIMELING_EGG.get(),
                GCBlocks.BLUE_SLIMELING_EGG.get(), GCBlocks.YELLOW_SLIMELING_EGG.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(GCBlocks.RED_SLIMELING_EGG.get(), GCBlocks.BLUE_SLIMELING_EGG.get(), GCBlocks.YELLOW_SLIMELING_EGG.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GCBlocks.TREASURE_CHEST_TIER_1.get(),
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
                GCBlocks.FALLEN_METEOR.get(),
                GCBlocks.COAL_GENERATOR.get(), GCBlocks.ENERGY_STORAGE_MODULE.get(), GCBlocks.ENERGY_STORAGE_CLUSTER.get(),
                GCBlocks.COMPRESSOR.get(), GCBlocks.ELECTRIC_COMPRESSOR.get(), GCBlocks.ADVANCED_COMPRESSOR.get(),
                GCBlocks.ELECTRIC_FURNACE.get(), GCBlocks.ELECTRIC_ARC_FURNACE.get(), GCBlocks.CIRCUIT_FABRICATOR.get(),
                GCBlocks.BASIC_SOLAR_PANEL.get(), GCBlocks.ADVANCED_SOLAR_PANEL.get(), GCBlocks.SOLAR_PART.get(),
                GCBlocks.REFINERY.get(), GCBlocks.FLUID_TANK.get(),
                GCBlocks.GAS_LIQUEFIER.get(), GCBlocks.METHANE_SYNTHESIZER.get(), GCBlocks.ELECTROLYZER.get(), GCBlocks.TERRAFORMER.get(), GCBlocks.LAUNCH_CONTROLLER.get(),
                GCBlocks.CRYOGENIC_CHAMBER.get(), GCBlocks.CRYOGENIC_CHAMBER_PART.get(),
                GCBlocks.OXYGEN_COLLECTOR.get(), GCBlocks.OXYGEN_COMPRESSOR.get(), GCBlocks.OXYGEN_DECOMPRESSOR.get(),
                GCBlocks.OXYGEN_STORAGE_MODULE.get(), GCBlocks.OXYGEN_DISTRIBUTOR.get(), GCBlocks.OXYGEN_SEALER.get(),
                GCBlocks.OXYGEN_DETECTOR.get(),
                GCBlocks.LANDING_PAD.get(), GCBlocks.BUGGY_FUELING_PAD.get(), GCBlocks.FUEL_LOADER.get(), GCBlocks.CARGO_LOADER.get(), GCBlocks.CARGO_UNLOADER.get(),
                GCBlocks.NASA_WORKBENCH.get(),
                GCBlocks.NASA_WORKBENCH_PART.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(GCBlocks.PARACHEST.get());
        // Oxygen sealing (1.12.2 OxygenPressureProtocol): glass panes seal, gravel/wool/sponge let air through
        tag(GCModTags.SEALABLE).addTag(Tags.Blocks.GLASS_PANES);
        tag(GCModTags.AIR_PERMEABLE).add(net.minecraft.world.level.block.Blocks.GRAVEL, net.minecraft.world.level.block.Blocks.SPONGE,
                net.minecraft.world.level.block.Blocks.WET_SPONGE).addTag(BlockTags.WOOL);
        tag(BlockTags.REPLACEABLE).add(GCBlocks.BREATHABLE_AIR.get());
        tag(BlockTags.WALL_POST_OVERRIDE).add(GCBlocks.UNLIT_TORCH.get());

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
        tag(GCTags.block("ores/tin")).add(GCBlocks.TIN_ORE.get(), GCBlocks.MOON_TIN_ORE.get(), GCBlocks.MARS_TIN_ORE.get());
        tag(GCTags.block("ores/aluminum")).add(GCBlocks.ALUMINUM_ORE.get());
        tag(GCTags.block("ores/silicon")).add(GCBlocks.SILICON_ORE.get());
        tag(GCTags.block("ores/sapphire")).add(GCBlocks.MOON_SAPPHIRE_ORE.get());
        tag(GCTags.block("ores/cheese")).add(GCBlocks.MOON_CHEESE_ORE.get());
        tag(Tags.Blocks.ORES_COPPER).add(GCBlocks.MOON_COPPER_ORE.get(), GCBlocks.MARS_COPPER_ORE.get());
        tag(Tags.Blocks.ORES).addTags(GCTags.block("ores/tin"), GCTags.block("ores/aluminum"), GCTags.block("ores/silicon"),
                GCTags.block("ores/sapphire"), GCTags.block("ores/cheese"), GCTags.block("ores/desh"), GCTags.block("ores/ilmenite"))
                .add(GCBlocks.MOON_COPPER_ORE.get(), GCBlocks.MARS_COPPER_ORE.get(), GCBlocks.MARS_IRON_ORE.get(), GCBlocks.ASTEROID_IRON_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(GCBlocks.TIN_ORE.get(), GCBlocks.ALUMINUM_ORE.get(), GCBlocks.SILICON_ORE.get());

        tag(GCTags.block("storage_blocks/tin")).add(GCBlocks.TIN_BLOCK.get());
        tag(GCTags.block("storage_blocks/aluminum")).add(GCBlocks.ALUMINUM_BLOCK.get());
        tag(GCTags.block("storage_blocks/meteoric_iron")).add(GCBlocks.METEORIC_IRON_BLOCK.get());
        tag(GCTags.block("storage_blocks/silicon")).add(GCBlocks.SILICON_BLOCK.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).addTags(GCTags.block("storage_blocks/tin"), GCTags.block("storage_blocks/aluminum"),
                GCTags.block("storage_blocks/meteoric_iron"), GCTags.block("storage_blocks/silicon"), GCTags.block("storage_blocks/desh"),
                GCTags.block("storage_blocks/titanium"));
    }
}
