package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.EnergyStorageBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
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
        // Machines: 1.12.2 models put the front on north and the energy port on east (the left side seen from the front).
        machine(GCBlocks.COAL_GENERATOR.get(), "", "coal_generator", "machine_output", "machine_side", "machine_side");
        machine(GCBlocks.COMPRESSOR.get(), "", "compressor", "machine_side", "machine_side", "machine_side");
        machine(GCBlocks.ELECTRIC_COMPRESSOR.get(), "", "electric_compressor", "machine_input", "machine_side", "machine_side");
        machine(GCBlocks.ADVANCED_COMPRESSOR.get(), "advanced/", "electric_compressor", "machine_input", "machine_side", "machine_side");
        machine(GCBlocks.ELECTRIC_FURNACE.get(), "", "electric_furnace", "machine_input", "machine_side", "machine_side");
        machine(GCBlocks.ELECTRIC_ARC_FURNACE.get(), "advanced/", "electric_furnace", "machine_input", "machine_side", "machine_side");
        machine(GCBlocks.CIRCUIT_FABRICATOR.get(), "", "circuit_fabricator", "machine_input", "machine_side", "machine_side");
        energyStorage(GCBlocks.ENERGY_STORAGE_MODULE.get(), "");
        energyStorage(GCBlocks.ENERGY_STORAGE_CLUSTER.get(), "advanced/");

        wire(GCBlocks.ALUMINUM_WIRE.get(), "aluminum_wire", "aluminum_wire");
        wire(GCBlocks.HEAVY_ALUMINUM_WIRE.get(), "heavy_aluminum_wire", "heavy_aluminum_wire");

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

    /** Cube machine model + horizontal facing. {@code tier} is "" or "advanced/" (tier-2 textures). */
    private void machine(Block block, String tier, String front, String east, String south, String west) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        final ResourceLocation casing = tex(tier + "machine");
        final ModelFile model = models().cube(name, casing, casing, tex(tier + front), tex(tier + south), tex(tier + east), tex(tier + west))
                .texture("particle", casing);
        horizontalBlock(block, model);
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    /** Front and back show the charge level (energy_storage_module_0..16); output on the left, input on the right. */
    private void energyStorage(Block block, String tier) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        final ResourceLocation casing = tex(tier + "machine");
        final ModelFile[] levels = new ModelFile[17];
        for (int level = 0; level <= 16; level++) {
            final ResourceLocation face = tex(tier + "energy_storage_module_" + level);
            levels[level] = models().cube(name + "_" + level, casing, casing, face, face, tex(tier + "machine_output"), tex(tier + "machine_input"))
                    .texture("particle", casing);
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(levels[state.getValue(EnergyStorageBlock.LEVEL)])
                .rotationY(((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360)
                .build());
        itemModels().withExistingParent(name, modLoc("block/" + name + "_0"));
    }

    /**
     * Multipart: the core always, plus the 1.12.2 north arm rotated onto each connected side
     * (x=270 points it up, x=90 down, y rotates it around the horizontal sides).
     */
    private void wire(Block block, String modelPrefix, String itemTexture) {
        final ModelFile core = models().getExistingFile(modLoc("block/" + modelPrefix + "_core"));
        final ModelFile side = models().getExistingFile(modLoc("block/" + modelPrefix + "_side"));
        final MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(core).addModel().end();
        builder.part().modelFile(side).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.NORTH), true).end();
        builder.part().modelFile(side).rotationY(90).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.EAST), true).end();
        builder.part().modelFile(side).rotationY(180).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.SOUTH), true).end();
        builder.part().modelFile(side).rotationY(270).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.WEST), true).end();
        builder.part().modelFile(side).rotationX(270).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.UP), true).end();
        builder.part().modelFile(side).rotationX(90).addModel().condition(AluminumWireBlock.CONNECTIONS.get(Direction.DOWN), true).end();
        itemModels().withExistingParent(modelPrefix, mcLoc("item/generated")).texture("layer0", modLoc("item/" + itemTexture));
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
