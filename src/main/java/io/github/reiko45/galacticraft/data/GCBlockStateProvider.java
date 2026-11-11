package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Block states, block models and block item models. Texture names follow tools/legacy_mapping.json. */
public class GCBlockStateProvider extends BlockStateProvider {
    public GCBlockStateProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        for (DeferredBlock<Block> block : java.util.List.of(
                GCBlocks.TIN_ORE, GCBlocks.ALUMINUM_ORE, GCBlocks.SILICON_ORE,
                GCBlocks.TIN_BLOCK, GCBlocks.ALUMINUM_BLOCK, GCBlocks.METEORIC_IRON_BLOCK, GCBlocks.SILICON_BLOCK,
                GCBlocks.TIN_DECORATION_BLOCK_2,
                GCBlocks.MOON_DIRT, GCBlocks.MOON_ROCK, GCBlocks.MOON_DUNGEON_BRICK,
                GCBlocks.MOON_COPPER_ORE, GCBlocks.MOON_TIN_ORE, GCBlocks.MOON_CHEESE_ORE, GCBlocks.MOON_SAPPHIRE_ORE)) {
            simpleBlockWithItem(block.get(), cubeAll(block.get()));
        }

        final ResourceLocation tin1Side = tex("tin_decoration_block_1_side");
        final ResourceLocation tin1Bottom = tex("tin_decoration_block_1_bottom");
        final ResourceLocation tin1Top = tex("tin_decoration_block_1_top");
        simpleBlockWithItem(GCBlocks.TIN_DECORATION_BLOCK_1.get(),
                models().cubeBottomTop(GCBlocks.TIN_DECORATION_BLOCK_1.getId().getPath(), tin1Side, tin1Bottom, tin1Top));
        simpleBlockWithItem(GCBlocks.MOON_TURF.get(),
                models().cubeBottomTop("moon_turf", tex("moon_turf_side"), tex("moon_dirt"), tex("moon_turf_top")));

        // Stairs / slabs / walls
        stairs(GCBlocks.TIN_DECORATION_STAIRS_1, tin1Side, tin1Bottom, tin1Top);
        stairs(GCBlocks.TIN_DECORATION_STAIRS_2, tex("tin_decoration_block_2"), tex("tin_decoration_block_2"), tex("tin_decoration_block_2"));
        stairs(GCBlocks.MOON_ROCK_STAIRS, tex("moon_rock"), tex("moon_rock"), tex("moon_rock"));
        stairs(GCBlocks.MOON_DUNGEON_BRICK_STAIRS, tex("moon_dungeon_brick"), tex("moon_dungeon_brick"), tex("moon_dungeon_brick"));

        slab(GCBlocks.TIN_DECORATION_SLAB_1, GCBlocks.TIN_DECORATION_BLOCK_1, tin1Side, tin1Bottom, tin1Top);
        slab(GCBlocks.TIN_DECORATION_SLAB_2, GCBlocks.TIN_DECORATION_BLOCK_2, tex("tin_decoration_block_2"), tex("tin_decoration_block_2"), tex("tin_decoration_block_2"));
        slab(GCBlocks.MOON_ROCK_SLAB, GCBlocks.MOON_ROCK, tex("moon_rock"), tex("moon_rock"), tex("moon_rock"));
        slab(GCBlocks.MOON_DUNGEON_BRICK_SLAB, GCBlocks.MOON_DUNGEON_BRICK, tex("moon_dungeon_brick"), tex("moon_dungeon_brick"), tex("moon_dungeon_brick"));

        wall(GCBlocks.TIN_DECORATION_WALL_1, tin1Side);
        wall(GCBlocks.TIN_DECORATION_WALL_2, tex("tin_decoration_block_2"));
        wall(GCBlocks.MOON_ROCK_WALL, tex("moon_rock"));
        wall(GCBlocks.MOON_DUNGEON_BRICK_WALL, tex("moon_dungeon_brick"));

        cheese();

        simpleBlockWithItem(GCBlocks.FALLEN_METEOR.get(), models().getExistingFile(modLoc("block/fallen_meteor")));

        final ResourceLocation torchTexture = tex("glowstone_torch");
        simpleBlock(GCBlocks.GLOWSTONE_TORCH.get(), models().torch("glowstone_torch", torchTexture).renderType("cutout"));
        horizontalBlock(GCBlocks.GLOWSTONE_WALL_TORCH.get(), models().torchWall("glowstone_wall_torch", torchTexture).renderType("cutout"), 90);
        itemModels().withExistingParent("glowstone_torch", mcLoc("item/generated")).texture("layer0", torchTexture);
    }

    private ResourceLocation tex(String name) {
        return modLoc("block/" + name);
    }

    private void stairs(DeferredBlock<StairBlock> block, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        stairsBlock(block.get(), side, bottom, top);
        blockItem(block);
    }

    private void slab(DeferredBlock<SlabBlock> block, DeferredBlock<Block> base, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        slabBlock(block.get(), modLoc("block/" + base.getId().getPath()), side, bottom, top);
        blockItem(block);
    }

    private void wall(DeferredBlock<WallBlock> block, ResourceLocation texture) {
        wallBlock(block.get(), texture);
        itemModels().wallInventory(block.getId().getPath(), texture);
    }

    private void blockItem(DeferredBlock<?> block) {
        itemModels().withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }

    /** Vanilla cake models with the 1.12.2 cheese textures (same slots: top/bottom/side/inside). */
    private void cheese() {
        final ResourceLocation top = tex("cheese_block_top");
        final ResourceLocation side = tex("cheese_block_side");
        final ResourceLocation inner = tex("cheese_block_inner");
        getVariantBuilder(GCBlocks.CHEESE_BLOCK.get()).forAllStates(state -> {
            final int bites = state.getValue(CakeBlock.BITES);
            final BlockModelBuilder model = bites == 0
                    ? models().withExistingParent("cheese_block", mcLoc("block/cake"))
                    : models().withExistingParent("cheese_block_slice" + bites, mcLoc("block/cake_slice" + bites)).texture("inside", inner);
            model.texture("particle", side).texture("bottom", top).texture("top", top).texture("side", side);
            return ConfiguredModel.builder().modelFile(model).build();
        });
        itemModels().getBuilder("cheese_block").parent(new ModelFile.UncheckedModelFile(modLoc("block/cheese_block")));
    }
}
