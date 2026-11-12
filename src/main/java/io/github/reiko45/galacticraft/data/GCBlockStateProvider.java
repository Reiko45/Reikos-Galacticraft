package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.AluminumWireBlock;
import io.github.reiko45.galacticraft.block.ConfigurableMachineBlock;
import io.github.reiko45.galacticraft.block.EnergyStorageBlock;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.FluidTankBlock;
import io.github.reiko45.galacticraft.block.OxygenStorageModuleBlock;
import io.github.reiko45.galacticraft.block.SpaceTorchBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.machine.InputSide;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.machine.StorageSides;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.ParachestBlock;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder;
import net.minecraft.world.item.DyeColor;
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
                GCBlocks.MOON_COPPER_ORE, GCBlocks.MOON_TIN_ORE, GCBlocks.MOON_CHEESE_ORE, GCBlocks.MOON_SAPPHIRE_ORE,
                GCBlocks.MARS_COPPER_ORE, GCBlocks.MARS_TIN_ORE, GCBlocks.MARS_IRON_ORE,
                GCBlocks.MARS_FINE_REGOLITH, GCBlocks.MARS_REGOLITH, GCBlocks.MARS_STONE, GCBlocks.MARS_COBBLESTONE,
                GCBlocks.MARS_DUNGEON_BRICK, GCBlocks.DESH_BLOCK)) {
            simpleBlockWithItem(block.get(), cubeAll(block.get()));
        }
        simpleBlockWithItem(GCBlocks.DESH_ORE.get(), cubeAll(GCBlocks.DESH_ORE.get()));
        asteroids();
        venus();
        walkways();
        // 1.12.2 vine_0..2 are cross models (vine_model); the item shows the first.
        getVariantBuilder(GCBlocks.CAVERNOUS_VINES.get()).forAllStates(state -> {
            final int type = state.getValue(io.github.reiko45.galacticraft.block.CavernousVinesBlock.TYPE);
            return net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder().modelFile(models()
                    .cross("cavernous_vines_" + type, tex("cavernous_vines_" + type)).renderType("cutout")).build();
        });
        itemModels().withExistingParent("cavernous_vines", mcLoc("item/generated")).texture("layer0", tex("cavernous_vines_0"));

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
        stairs(GCBlocks.MARS_COBBLESTONE_STAIRS, tex("mars_cobblestone"), tex("mars_cobblestone"), tex("mars_cobblestone"));
        stairs(GCBlocks.MARS_DUNGEON_BRICK_STAIRS, tex("mars_dungeon_brick"), tex("mars_dungeon_brick"), tex("mars_dungeon_brick"));

        slab(GCBlocks.TIN_DECORATION_SLAB_1, GCBlocks.TIN_DECORATION_BLOCK_1, tin1Side, tin1Bottom, tin1Top);
        slab(GCBlocks.TIN_DECORATION_SLAB_2, GCBlocks.TIN_DECORATION_BLOCK_2, tex("tin_decoration_block_2"), tex("tin_decoration_block_2"), tex("tin_decoration_block_2"));
        slab(GCBlocks.MOON_ROCK_SLAB, GCBlocks.MOON_ROCK, tex("moon_rock"), tex("moon_rock"), tex("moon_rock"));
        slab(GCBlocks.MOON_DUNGEON_BRICK_SLAB, GCBlocks.MOON_DUNGEON_BRICK, tex("moon_dungeon_brick"), tex("moon_dungeon_brick"), tex("moon_dungeon_brick"));
        slab(GCBlocks.MARS_COBBLESTONE_SLAB, GCBlocks.MARS_COBBLESTONE, tex("mars_cobblestone"), tex("mars_cobblestone"), tex("mars_cobblestone"));
        slab(GCBlocks.MARS_DUNGEON_BRICK_SLAB, GCBlocks.MARS_DUNGEON_BRICK, tex("mars_dungeon_brick"), tex("mars_dungeon_brick"), tex("mars_dungeon_brick"));

        wall(GCBlocks.TIN_DECORATION_WALL_1, tin1Side);
        wall(GCBlocks.TIN_DECORATION_WALL_2, tex("tin_decoration_block_2"));
        wall(GCBlocks.MOON_ROCK_WALL, tex("moon_rock"));
        wall(GCBlocks.MOON_DUNGEON_BRICK_WALL, tex("moon_dungeon_brick"));
        wall(GCBlocks.MARS_COBBLESTONE_WALL, tex("mars_cobblestone"));
        wall(GCBlocks.MARS_DUNGEON_BRICK_WALL, tex("mars_dungeon_brick"));

        cheese();
        // Machines: 1.12.2 models put the front on north and the energy port on east (the left side seen from the front).
        machine(GCBlocks.COAL_GENERATOR.get(), "", "coal_generator", "machine_output", "machine_side", "machine_side");
        machine(GCBlocks.COMPRESSOR.get(), "", "compressor", "machine_side", "machine_side", "machine_side");
        configurableMachine(GCBlocks.ELECTRIC_COMPRESSOR.get(), "", "electric_compressor");
        configurableMachine(GCBlocks.ADVANCED_COMPRESSOR.get(), "advanced/", "electric_compressor");
        configurableMachine(GCBlocks.ELECTRIC_FURNACE.get(), "", "electric_furnace");
        configurableMachine(GCBlocks.ELECTRIC_ARC_FURNACE.get(), "advanced/", "electric_furnace");
        configurableMachine(GCBlocks.CIRCUIT_FABRICATOR.get(), "", "circuit_fabricator");
        solarBase(GCBlocks.BASIC_SOLAR_PANEL.get(), "solar_basic");
        solarBase(GCBlocks.ADVANCED_SOLAR_PANEL.get(), "solar_advanced");
        // Parts are invisible (the base block's renderer draws the panel); the model only supplies break particles.
        simpleBlock(GCBlocks.SOLAR_PART.get(), models().getBuilder("solar_panel_part").texture("particle", tex("machine")));
        // 1.12.2 refinery_model: oil in on the left (east), fuel out on the right (west), power in on top.
        horizontalBlock(GCBlocks.REFINERY.get(), models().cube("refinery", tex("machine"), tex("machine_input"), tex("refinery_front"),
                tex("refinery_side"), tex("machine_oil_input"), tex("machine_fuel_input")).texture("particle", tex("machine")));
        itemModels().withExistingParent("refinery", modLoc("block/refinery"));
        // 1.12.2 gas_liquefier_model, methane_synthesizer_model, electrolyzer_model: power in at the bottom; gas in on the
        // left (east), out on the right (west); the electrolyzer's water in on the left, oxygen out the back, hydrogen right.
        gasMachine(GCBlocks.GAS_LIQUEFIER.get(), "gas_liquefier", "machine_side_warning", "machine_oxygen_input_warning", "machine_oxygen_output_warning");
        gasMachine(GCBlocks.METHANE_SYNTHESIZER.get(), "methane_synthesizer", "machine_side_warning", "machine_oxygen_input_warning",
                "machine_oxygen_output_warning");
        gasMachine(GCBlocks.ELECTROLYZER.get(), "electrolyzer", "machine_oxygen_output_warning", "machine_water_input_warning",
                "machine_oxygen_output_warning");
        // 1.12.2 terraformer_model: the panel front and back, power in on the left (east).
        horizontalBlock(GCBlocks.TERRAFORMER.get(), models().cube("terraformer", tex("machine"), tex("machine"), tex("terraformer"),
                tex("terraformer"), tex("machine_input"), tex("machine")).texture("particle", tex("machine")));
        itemModels().withExistingParent("terraformer", modLoc("block/terraformer"));
        // 1.12.2 launch_controller_model: the panel front and back, power in on the left (east).
        horizontalBlock(GCBlocks.LAUNCH_CONTROLLER.get(), models().cube("launch_controller", tex("machine"), tex("machine"), tex("launch_controller"),
                tex("launch_controller"), tex("machine_input"), tex("machine")).texture("particle", tex("machine")));
        itemModels().withExistingParent("launch_controller", modLoc("block/launch_controller"));
        cryogenicChamber();
        fluidTank();
        fluidPipe();
        liquid(GCFluids.OIL_BLOCK.get(), "oil");
        liquid(GCFluids.FUEL_BLOCK.get(), "fuel");
        liquid(GCFluids.BACTERIAL_SLUDGE_BLOCK.get(), "bacterial_sludge");
        oxygenMachines();
        energyStorage(GCBlocks.ENERGY_STORAGE_MODULE.get(), "");
        energyStorage(GCBlocks.ENERGY_STORAGE_CLUSTER.get(), "advanced/");

        wire(GCBlocks.ALUMINUM_WIRE.get(), "aluminum_wire", "aluminum_wire");
        wire(GCBlocks.HEAVY_ALUMINUM_WIRE.get(), "heavy_aluminum_wire", "heavy_aluminum_wire");
        // Switch variants reuse the wire geometry with the switch texture (1.12.2 blockstate texture overrides).
        switchWireParts("aluminum_wire");
        switchWireParts("heavy_aluminum_wire");
        wire(GCBlocks.ALUMINUM_WIRE_SWITCH.get(), "aluminum_wire_switch", "aluminum_wire_switch");
        wire(GCBlocks.HEAVY_ALUMINUM_WIRE_SWITCH.get(), "heavy_aluminum_wire_switch", "heavy_aluminum_wire_switch");

        simpleBlockWithItem(GCBlocks.FALLEN_METEOR.get(), models().getExistingFile(modLoc("block/fallen_meteor")));
        rockets();
        dungeon();
        stations();

        final ResourceLocation torchTexture = tex("glowstone_torch");
        simpleBlock(GCBlocks.GLOWSTONE_TORCH.get(), models().torch("glowstone_torch", torchTexture).renderType("cutout"));
        horizontalBlock(GCBlocks.GLOWSTONE_WALL_TORCH.get(), models().torchWall("glowstone_wall_torch", torchTexture).renderType("cutout"), 90);
        itemModels().withExistingParent("glowstone_torch", mcLoc("item/generated")).texture("layer0", torchTexture);
    }

    /** Launch pad, Fuel Loader, NASA Workbench (1.12.2 OBJ model) and Para-chest (one model per parachute colour). */
    private void rockets() {
        final ModelFile pad = models().getExistingFile(modLoc("block/landing_pad"));
        final ModelFile fullPad = models().getExistingFile(modLoc("block/landing_pad_full"));
        getVariantBuilder(GCBlocks.LANDING_PAD.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(LandingPadBlock.PAD) == LandingPadBlock.Pad.SINGLE ? pad : fullPad).build());
        itemModels().withExistingParent("landing_pad", modLoc("block/landing_pad"));
        final ModelFile buggyPad = models().getExistingFile(modLoc("block/buggy_fueling_pad"));
        final ModelFile fullBuggyPad = models().getExistingFile(modLoc("block/buggy_fueling_pad_full"));
        getVariantBuilder(GCBlocks.BUGGY_FUELING_PAD.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(LandingPadBlock.PAD) == LandingPadBlock.Pad.SINGLE ? buggyPad : fullBuggyPad).build());
        itemModels().withExistingParent("buggy_fueling_pad", modLoc("block/buggy_fueling_pad"));
        buggy();
        // 1.12.2 fuel_loader_model: loader panels front and back, power in on the left (east), fuel in on the right (west)
        machine(GCBlocks.FUEL_LOADER.get(), "", "machine_fuelloader", "machine_input", "machine_fuelloader", "machine_fuel_input");
        // 1.12.2 cargo_loader_model / cargo_unloader_model: power in on the left (east), items in / out on the right
        machine(GCBlocks.CARGO_LOADER.get(), "", "machine_cargoloader", "machine_input", "machine_cargoloader", "machine_item_input");
        machine(GCBlocks.CARGO_UNLOADER.get(), "", "machine_cargounloader", "machine_input", "machine_cargounloader", "machine_item_output");
        final ModelFile workbench = models().getBuilder("nasa_workbench").customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/nasa_workbench.obj")).flipV(true).end()
                .texture("particle", tex("assembly"));
        simpleBlock(GCBlocks.NASA_WORKBENCH.get(), workbench);
        itemModels().withExistingParent("nasa_workbench", modLoc("block/nasa_workbench")).transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -3.5F, 0).scale(0.18F).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 1, 0).scale(0.12F).end()
                .transform(ItemDisplayContext.FIXED).translation(0, -3.5F, 0).scale(0.18F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 1.5F, 0).scale(0.15F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, -2, 0).scale(0.15F).end()
                .end();
        simpleBlock(GCBlocks.NASA_WORKBENCH_PART.get(), models().getBuilder("nasa_workbench_part").texture("particle", tex("assembly")));
        getVariantBuilder(GCBlocks.PARACHEST.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models().withExistingParent("parachest_" + ParachuteItem.LEGACY_COLORS[state.getValue(ParachestBlock.COLOR)],
                                modLoc("block/parachest"))
                        .texture("chute", modLoc("model/parachute/" + ParachuteItem.LEGACY_COLORS[state.getValue(ParachestBlock.COLOR)])))
                .rotationY(((int) state.getValue(ParachestBlock.FACING).toYRot()) % 360)
                .build());
        itemModels().withExistingParent("parachest", modLoc("block/parachest"));
    }

    /** Space Station Arrival Block (a plain cube) and the Spin Thruster (1.12.2 blockstate: rev turns it upside down). */
    private void stations() {
        simpleBlock(GCBlocks.SPACE_STATION_BASE.get(), models().cubeBottomTop("space_station_base", tex("space_station_side"),
                tex("space_station_side"), tex("space_station_top")));
        final ModelFile thruster = models().getExistingFile(modLoc("block/spin_thruster"));
        getVariantBuilder(GCBlocks.SPIN_THRUSTER.get()).forAllStates(state -> {
            final boolean reversed = state.getValue(io.github.reiko45.galacticraft.block.SpinThrusterBlock.REVERSED);
            final int y = switch (state.getValue(io.github.reiko45.galacticraft.block.SpinThrusterBlock.FACING)) {
                case NORTH -> reversed ? 0 : 180;
                case EAST -> reversed ? 90 : 270;
                case WEST -> reversed ? 270 : 90;
                default -> reversed ? 180 : 0;
            };
            return ConfiguredModel.builder().modelFile(thruster).rotationX(reversed ? 180 : 0).rotationY(y).build();
        });
        itemModels().withExistingParent("spin_thruster", modLoc("block/spin_thruster"));
    }

    /** One standalone model per part of the 1.12.2 buggy OBJ, drawn by BuggyRenderer (1.12.2 RenderBuggy baked them the same way). */
    public static final String[] BUGGY_PARTS = {"MainBody", "RadarDish_Dish", "Wheel_Left", "Wheel_Right", "Wheel_Left_Cover", "Wheel_Right_Cover",
            "CargoLeft", "CargoMid", "CargoRight"};

    private void buggy() {
        // The Tier 2 Rocket and Landing Balloons are single-object OBJs (1.12.2 RenderTier2Rocket, RenderLandingBalloons).
        for (String entity : new String[]{"tier_2_rocket", "cargo_rocket", "landing_balloons", "entry_pod", "grapple"}) {
            models().getBuilder("entity/" + entity).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/entity/" + entity + ".obj")).flipV(true).end();
        }
        // 1.12.2 RenderSpiderQueen: the web cocoon sphere
        models().getBuilder("entity/web_cocoon").customLoader(ObjModelBuilder::begin).modelLocation(modLoc("models/entity/web_cocoon.obj")).flipV(true).end();
        // 1.12.2 RenderEntryPodVenus baked pod_flame's PodBody and Flame_Sphere apart
        for (String[] part : new String[][]{{"body", "PodBody"}, {"flame", "Flame_Sphere"}}) {
            final var loader = models().getBuilder("entity/venus_entry_pod/" + part[0]).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/entity/venus_entry_pod.obj")).flipV(true);
            loader.visibility("PodBody", part[1].equals("PodBody"));
            loader.visibility("Flame_Sphere", part[1].equals("Flame_Sphere"));
            loader.end();
        }
        // 1.12.2 RenderTier3Rocket baked the body and boosters, the nose cone and the blinking light ("Cube") apart
        final String[][] tier3 = {{"body", "Boosters", "Rocket"}, {"nose_cone", "NoseCone"}, {"light", "Cube"}};
        for (String[] part : tier3) {
            final var loader = models().getBuilder("entity/tier_3_rocket/" + part[0]).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/entity/tier_3_rocket.obj")).flipV(true);
            for (String group : new String[]{"Boosters", "Rocket", "NoseCone", "Cube"}) {
                loader.visibility(group, java.util.Arrays.asList(part).subList(1, part.length).contains(group));
            }
            loader.end();
        }
        for (String part : BUGGY_PARTS) {
            final var loader = models().getBuilder("entity/buggy/" + part.toLowerCase(java.util.Locale.ROOT)).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/entity/buggy.obj")).flipV(true);
            for (String other : BUGGY_PARTS) {
                loader.visibility(other, other.equals(part));
            }
            loader.end();
        }
    }

    /**
     * Boss spawners (invisible) and the Treasure Chests, drawn by TreasureChestRenderer (block and item); the Creeper
     * Egg and the Slimeling Eggs (1.12.2 models), cracked eggs with their cracked texture.
     */
    private void dungeon() {
        simpleBlock(GCBlocks.BOSS_SPAWNER.get(), models().getBuilder("boss_spawner").texture("particle", tex("moon_dungeon_brick")));
        simpleBlock(GCBlocks.MARS_BOSS_SPAWNER.get(), models().getBuilder("mars_boss_spawner").texture("particle", tex("mars_dungeon_brick")));
        simpleBlockWithItem(GCBlocks.CREEPER_EGG.get(), models().getExistingFile(modLoc("block/creeper_egg")));
        for (DeferredBlock<io.github.reiko45.galacticraft.block.SlimelingEggBlock> egg : java.util.List.of(
                GCBlocks.RED_SLIMELING_EGG, GCBlocks.BLUE_SLIMELING_EGG, GCBlocks.YELLOW_SLIMELING_EGG)) {
            final String name = egg.getId().getPath();
            final ModelFile whole = models().getExistingFile(modLoc("block/" + name));
            final ModelFile cracked = models().withExistingParent(name + "_broken", modLoc("block/" + name))
                    .texture("particle", tex(name + "_broken")).texture("texture", tex(name + "_broken"));
            getVariantBuilder(egg.get()).forAllStates(state -> net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(state.getValue(io.github.reiko45.galacticraft.block.SlimelingEggBlock.BROKEN) ? cracked : whole).build());
            simpleBlockItem(egg.get(), whole);
        }
        treasureChest(GCBlocks.TREASURE_CHEST_TIER_1.get(), "treasure_chest_tier_1", tex("treasure_chest"));
        treasureChest(GCBlocks.TREASURE_CHEST_TIER_2.get(), "treasure_chest_tier_2", tex("treasure_chest_tier_2"));
        treasureChest(GCBlocks.TREASURE_CHEST_TIER_3.get(), "treasure_chest_tier_3", tex("treasure_chest_tier_3"));
        simpleBlock(GCBlocks.VENUS_BOSS_SPAWNER.get(), models().getBuilder("venus_boss_spawner").texture("particle", tex("venus_dungeon_brick_1")));
    }

    private void treasureChest(Block block, String name, ResourceLocation particle) {
        simpleBlock(block, models().getBuilder(name).texture("particle", particle));
        itemModels().getBuilder(name).parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .texture("particle", particle).transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 45, 0).scale(0.625F).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.25F).end()
                .transform(ItemDisplayContext.HEAD).rotation(0, 180, 0).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 315, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 315, 0).scale(0.4F).end()
                .end();
    }

    /** 1.12.2 mars_machine cryogenic_chamber (cryo_0) and block_multi cryo_1 / cryo_2: open to the south unrotated. */
    private void cryogenicChamber() {
        final ModelFile bottom = models().getExistingFile(modLoc("block/cryogenic_chamber"));
        final ModelFile middle = models().getExistingFile(modLoc("block/cryogenic_chamber_middle"));
        final ModelFile top = models().getExistingFile(modLoc("block/cryogenic_chamber_top"));
        horizontalBlock(GCBlocks.CRYOGENIC_CHAMBER.get(), state -> bottom, 0);
        getVariantBuilder(GCBlocks.CRYOGENIC_CHAMBER_PART.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock.TOP) ? top : middle)
                .rotationY((int) state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).toYRot()).build());
        itemModels().withExistingParent("cryogenic_chamber", modLoc("block/cryogenic_chamber"));
    }

    /**
     * 1.12.2 venus blockstate: the four rocks turned at random (venus_rock_N_model with y 0/90/180/270), the rest plain
     * cubes; the spout's top is the vent; web string and the web torch are cross models (vine_model).
     */
    private void venus() {
        for (DeferredBlock<? extends Block> rock : java.util.List.of(GCBlocks.VENUS_SOFT_ROCK, GCBlocks.VENUS_HARD_ROCK, GCBlocks.VENUS_VOLCANIC_ROCK,
                GCBlocks.PUMICE)) {
            final ModelFile model = cubeAll(rock.get());
            getVariantBuilder(rock.get()).partialState().setModels(ConfiguredModel.allYRotations(model, 0, false));
            simpleBlockItem(rock.get(), model);
        }
        for (DeferredBlock<? extends Block> block : java.util.List.of(GCBlocks.VENUS_DUNGEON_BRICK_1, GCBlocks.VENUS_DUNGEON_BRICK_2,
                GCBlocks.VENUS_ALUMINUM_ORE, GCBlocks.VENUS_COPPER_ORE, GCBlocks.GALENA_ORE, GCBlocks.VENUS_QUARTZ_ORE, GCBlocks.VENUS_SILICON_ORE,
                GCBlocks.VENUS_TIN_ORE, GCBlocks.LEAD_BLOCK, GCBlocks.SOLAR_ORE, GCBlocks.SCORCHED_VENUS_ROCK)) {
            simpleBlockWithItem(block.get(), cubeAll(block.get()));
        }
        final ResourceLocation soft = tex("venus_soft_rock");
        simpleBlockWithItem(GCBlocks.VAPOR_SPOUT.get(), models().cube("vapor_spout", soft, tex("vapor_spout"), soft, soft, soft, soft)
                .texture("particle", soft));
        for (DeferredBlock<? extends Block> web : java.util.List.of(GCBlocks.WEB_STRING, GCBlocks.WEB_TORCH)) {
            final String name = web.getId().getPath();
            simpleBlock(web.get(), models().cross(name, tex(name)).renderType("cutout"));
            itemModels().withExistingParent(name, mcLoc("item/generated")).texture("layer0", tex(name));
        }
        // The probe's model comes from the porter (1.12.2 crashed_probe.json); facing north is unturned.
        final ModelFile probe = models().getExistingFile(modLoc("block/crashed_probe"));
        horizontalBlock(GCBlocks.CRASHED_PROBE.get(), probe);
        simpleBlockItem(GCBlocks.CRASHED_PROBE.get(), probe);
        liquid(GCFluids.SULPHURIC_ACID_BLOCK.get(), "sulphuric_acid");
        venusMachines();
    }

    /**
     * 1.12.2 geothermal_model / geothermal_inactive_model (front north, power out on the left, east; vent on top), and
     * the porter's solar_array_module and solar_array_controller models. The controller model has its port on east,
     * and 1.12.2 turned it to the facing (north: y 270).
     */
    private void venusMachines() {
        final ResourceLocation casing = tex("machine");
        final ModelFile active = models().cube("geothermal_generator", casing, tex("geothermal_vent"), tex("geothermal"), tex("geothermal"),
                tex("machine_output"), tex("machine_side")).texture("particle", casing);
        final ModelFile inactive = models().cube("geothermal_generator_inactive", casing, tex("geothermal_vent"), tex("geothermal_inactive"),
                tex("geothermal_inactive"), tex("machine_output"), tex("machine_side")).texture("particle", casing);
        horizontalBlock(GCBlocks.GEOTHERMAL_GENERATOR.get(),
                state -> state.getValue(io.github.reiko45.galacticraft.block.GeothermalGeneratorBlock.ACTIVE) ? active : inactive);
        itemModels().withExistingParent("geothermal_generator", modLoc("block/geothermal_generator_inactive"));
        final ModelFile module = models().getExistingFile(modLoc("block/solar_array_module"));
        simpleBlock(GCBlocks.SOLAR_ARRAY_MODULE.get(), module);
        simpleBlockItem(GCBlocks.SOLAR_ARRAY_MODULE.get(), module);
        final ModelFile controller = models().getExistingFile(modLoc("block/solar_array_controller"));
        horizontalBlock(GCBlocks.SOLAR_ARRAY_CONTROLLER.get(), controller, 90);
        simpleBlockItem(GCBlocks.SOLAR_ARRAY_CONTROLLER.get(), controller);
    }

    /** 1.12.2 asteroids_block (rock turned at random, like its four weighted variants) and dense_ice. */
    private void asteroids() {
        for (net.neoforged.neoforge.registries.DeferredBlock<Block> rock : java.util.List.of(GCBlocks.ASTEROID_ROCK_0, GCBlocks.ASTEROID_ROCK_1,
                GCBlocks.ASTEROID_ROCK_2)) {
            final ModelFile model = cubeAll(rock.get());
            getVariantBuilder(rock.get()).partialState().setModels(ConfiguredModel.allYRotations(model, 0, false));
            simpleBlockItem(rock.get(), model);
        }
        for (Block block : new Block[]{GCBlocks.ASTEROID_ALUMINUM_ORE.get(), GCBlocks.ILMENITE_ORE.get(), GCBlocks.ASTEROID_IRON_ORE.get(),
                GCBlocks.DARK_DECORATION_BLOCK.get(), GCBlocks.TITANIUM_BLOCK.get()}) {
            simpleBlockWithItem(block, cubeAll(block));
        }
        slab(GCBlocks.DARK_DECORATION_SLAB, GCBlocks.DARK_DECORATION_BLOCK, tex("dark_decoration_block"), tex("dark_decoration_block"),
                tex("dark_decoration_block"));
        simpleBlockWithItem(GCBlocks.DENSE_ICE.get(), models().cubeAll("dense_ice", tex("dense_ice")).renderType("translucent"));
        // Invisible; the model only gives particles a texture.
        simpleBlock(GCBlocks.BRIGHT_AIR.get(), models().getBuilder("bright_air").texture("particle", mcLoc("block/glass")));
    }

    private void gasMachine(Block block, String name, String back, String left, String right) {
        horizontalBlock(block, models().cube(name, tex("advanced/machine_input"), tex("advanced/machine"), tex(name), tex(back), tex(left), tex(right))
                .texture("particle", tex("advanced/machine")));
        itemModels().withExistingParent(name, modLoc("block/" + name));
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

    /** 1.12.2 solar base: panel texture on top, output port on the front, side texture elsewhere, casing below. */
    private void solarBase(Block block, String texture) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        final ResourceLocation side = tex(texture + "_1");
        horizontalBlock(block, models().cube(name, tex("machine"), tex(texture + "_0"), tex("machine_output"), side, side, side)
                .texture("particle", side));
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    /**
     * Electric machine whose input face (machine_input texture) can be left/back/right of the front. 1.12.2 models put the
     * front on north, so left = east, back = south, right = west.
     */
    private void configurableMachine(Block block, String tier, String front) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        final ResourceLocation casing = tex(tier + "machine");
        final ResourceLocation side = tex(tier + "machine_side");
        final ResourceLocation input = tex(tier + "machine_input");
        final Map<InputSide, ModelFile> models = new EnumMap<>(InputSide.class);
        for (InputSide config : InputSide.values()) {
            final String model = config == InputSide.LEFT ? name : name + "_input_" + config.getSerializedName();
            models.put(config, models().cube(model, casing, casing, tex(tier + front),
                    config == InputSide.BACK ? input : side,
                    config == InputSide.LEFT ? input : side,
                    config == InputSide.RIGHT ? input : side).texture("particle", casing));
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models.get(state.getValue(ConfigurableMachineBlock.INPUT)))
                .rotationY(facingRotation(state))
                .build());
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    /**
     * Front and back show the charge level (energy_storage_module_0..16). The input/output faces (left, right or back)
     * show machine_input/machine_output; unused left/right faces show the casing, an unused back the charge level.
     */
    private void energyStorage(Block block, String tier) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        final ResourceLocation casing = tex(tier + "machine");
        final Map<StorageSides, ModelFile[]> models = new EnumMap<>(StorageSides.class);
        for (StorageSides sides : StorageSides.values()) {
            final ModelFile[] levels = new ModelFile[17];
            for (int level = 0; level <= 16; level++) {
                final ResourceLocation charge = tex(tier + "energy_storage_module_" + level);
                final String model = sides == StorageSides.RIGHT_LEFT ? name + "_" + level : name + "_" + sides.getSerializedName() + "_" + level;
                levels[level] = models().cube(model, casing, casing, charge,
                        storageFace(sides, RelativeSide.BACK, tier, charge),
                        storageFace(sides, RelativeSide.LEFT, tier, casing),
                        storageFace(sides, RelativeSide.RIGHT, tier, casing)).texture("particle", casing);
            }
            models.put(sides, levels);
        }
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(models.get(state.getValue(EnergyStorageBlock.SIDES))[state.getValue(EnergyStorageBlock.LEVEL)])
                .rotationY(facingRotation(state))
                .build());
        itemModels().withExistingParent(name, modLoc("block/" + name + "_0"));
    }

    private ResourceLocation storageFace(StorageSides sides, RelativeSide face, String tier, ResourceLocation unused) {
        if (sides.input() == face) {
            return tex(tier + "machine_input");
        }
        return sides.output() == face ? tex(tier + "machine_output") : unused;
    }

    /** Models face north; blockstates rotate them to the block's facing. */
    private static int facingRotation(net.minecraft.world.level.block.state.BlockState state) {
        return ((int) state.getValue(MachineBlock.FACING).toYRot() + 180) % 360;
    }

    private void switchWireParts(String basePrefix) {
        for (String part : new String[]{"_core", "_side"}) {
            models().withExistingParent(basePrefix + "_switch" + part, modLoc("block/" + basePrefix + part))
                    .texture("aluminum_wire", tex("aluminum_wire_switch"))
                    .texture("particle", tex("aluminum_wire_switch"));
        }
    }

    /**
     * Multipart: the core always, plus the 1.12.2 north arm rotated onto each connected side
     * (x=270 points it up, x=90 down, y rotates it around the horizontal sides).
     */
    /**
     * 1.12.2 BlockWalkway: the grating with its centre strut (walkway / walkway_wire / walkway_oxygen), plus the
     * per-side strut, wire or pipe arm of walkway_n and the downward arm of walkway_*_d.
     */
    private void walkways() {
        final String[][] kinds = {{"walkway", null}, {"walkway_wire", "walkway_wire_down"}, {"walkway_pipe", "walkway_pipe_down"}};
        final Block[] blocks = {GCBlocks.WALKWAY.get(), GCBlocks.WALKWAY_WIRE.get(), GCBlocks.WALKWAY_PIPE.get()};
        for (int i = 0; i < blocks.length; i++) {
            final ModelFile base = models().getExistingFile(modLoc("block/" + kinds[i][0]));
            final ModelFile side = models().getExistingFile(modLoc("block/" + kinds[i][0] + "_side"));
            final MultiPartBlockStateBuilder builder = getMultipartBuilder(blocks[i]);
            builder.part().modelFile(base).addModel().end();
            final Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int r = 0; r < sides.length; r++) {
                builder.part().modelFile(side).rotationY(r * 90).addModel()
                        .condition(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(sides[r]), true).end();
            }
            if (kinds[i][1] != null) {
                builder.part().modelFile(models().getExistingFile(modLoc("block/" + kinds[i][1]))).addModel()
                        .condition(net.minecraft.world.level.block.PipeBlock.DOWN, true).end();
            }
            simpleBlockItem(blocks[i], base);
        }
        // 1.12.2 beam_reflector / beam_receiver: drawn by their renderers from OBJ parts; the block model is just a particle
        final String[][][] beams = {
                {{"beam_reflector", "reflector"}, {"base", "Base"}, {"axle", "Axle"}, {"energy_blaster", "EnergyBlaster"}, {"ring", "Ring"}},
                {{"beam_receiver", "receiver"}, {"main", "Main"}, {"receiver", "Receiver"}, {"ring", "Ring"}}};
        for (String[][] beam : beams) {
            final java.util.List<String> groups = new java.util.ArrayList<>();
            for (int i = 1; i < beam.length; i++) {
                groups.add(beam[i][1]);
            }
            for (int i = 1; i < beam.length; i++) {
                final var loader = models().getBuilder("block/" + beam[0][0] + "/" + beam[i][0]).customLoader(ObjModelBuilder::begin)
                        .modelLocation(modLoc("models/block/" + beam[0][1] + ".obj")).flipV(true);
                for (String group : groups) {
                    loader.visibility(group, group.equals(beam[i][1]));
                }
                loader.end();
            }
            final Block block = beam[0][0].equals("beam_reflector") ? GCBlocks.BEAM_REFLECTOR.get() : GCBlocks.BEAM_RECEIVER.get();
            final ModelFile particle = models().getBuilder("block/" + beam[0][0]).texture("particle", tex(beam[0][0]));
            getVariantBuilder(block).partialState().setModels(new ConfiguredModel(particle));
            final var item = itemModels().getBuilder(beam[0][0]).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/block/" + beam[0][1] + ".obj")).flipV(true).end()
                    .texture("particle", tex(beam[0][0]));
            if (beam[0][0].equals("beam_reflector")) {
                item.transforms()
                        .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -4, 0).scale(0.45F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 0, 0).scale(0.25F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).translation(0, -4, 0).scale(0.45F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 0, 0).scale(0.25F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 0, 0).scale(0.25F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.3F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.3F).end()
                        .end();
            } else {
                item.transforms()
                        .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).translation(4, -2, 0).scale(0.6F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.4F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).translation(4, -2, 0).scale(0.6F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2, 0).scale(0.375F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2, 0).scale(0.375F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(2, 2, 0).scale(0.4F).end()
                        .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 45, 0).translation(2, 2, 0).scale(0.4F).end()
                        .end();
            }
        }
        // 1.12.2 telepad_short: drawn by its renderer (the pad, and the canopy with its connector); the parts are invisible
        for (String[] part : new String[][]{{"top", "Top", "Connector"}, {"bottom", "Bottom"}}) {
            final var loader = models().getBuilder("block/short_range_telepad/" + part[0]).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/block/telepad_short.obj")).flipV(true);
            for (String group : new String[]{"Top", "Connector", "Bottom"}) {
                loader.visibility(group, java.util.Arrays.asList(part).subList(1, part.length).contains(group));
            }
            loader.end();
        }
        final ModelFile telepadParticle = models().getBuilder("block/short_range_telepad").texture("particle", tex("telepad_short"));
        getVariantBuilder(GCBlocks.SHORT_RANGE_TELEPAD.get()).partialState().setModels(new ConfiguredModel(telepadParticle));
        getVariantBuilder(GCBlocks.TELEPAD_FAKE.get()).partialState().setModels(new ConfiguredModel(telepadParticle));
        itemModels().getBuilder("short_range_telepad").customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/telepad_short.obj")).flipV(true).end()
                .texture("particle", tex("telepad_short")).transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -3, 0).scale(0.2F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 1, 0).scale(0.12F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).translation(0, -3, 0).scale(0.2F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 1, 0).scale(0.12F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 1, 0).scale(0.12F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.15F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.15F).end()
                .end();
        // 1.12.2 Astro Miner Base: single blocks are cubes of machine frame; the built base is drawn by its master's renderer
        simpleBlockWithItem(GCBlocks.MINER_BASE.get(), models().cubeAll("miner_base", tex("machine_frame")));
        getVariantBuilder(GCBlocks.MINER_BASE_FULL.get()).partialState().setModels(new ConfiguredModel(
                models().getBuilder("block/miner_base_full").texture("particle", tex("machine_frame"))));
        models().getBuilder("block/miner_base_model").customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/block/minerbase.obj")).flipV(true).end();
        // 1.12.2 RenderAstroMiner's parts of astro_miner_full and astro_miner_full_off
        final String[] minerGroups = {"Hull", "Lasers_Main_Laser_Base", "HoverPad", "Glow", "Main_Laser_Front", "Main_Laser_Center", "Main_Laser_Left_Guard"};
        final String[][] minerParts = {{"main", "Hull", "Lasers_Main_Laser_Base"}, {"hover_pad", "HoverPad"}, {"glow", "Glow"},
                {"laser_front", "Main_Laser_Front"}, {"laser_center", "Main_Laser_Center"}, {"laser_guard", "Main_Laser_Left_Guard"}};
        for (String[] part : minerParts) {
            final var loader = models().getBuilder("entity/astro_miner/" + part[0]).customLoader(ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/entity/astro_miner_full.obj")).flipV(true);
            for (String group : minerGroups) {
                loader.visibility(group, java.util.Arrays.asList(part).subList(1, part.length).contains(group));
            }
            loader.end();
        }
        final var inactive = models().getBuilder("entity/astro_miner/inactive").customLoader(ObjModelBuilder::begin)
                .modelLocation(modLoc("models/entity/astro_miner_full_off.obj")).flipV(true);
        for (String group : new String[]{"Hull", "Lasers_Main_Laser_Base", "HoverPad", "Main_Laser_Front", "Main_Laser_Center", "Main_Laser_Left_Guard"}) {
            inactive.visibility(group, group.equals("Hull") || group.equals("Lasers_Main_Laser_Base") || group.equals("HoverPad"));
        }
        inactive.end();
        // 1.12.2 spacewart.json: the vanilla nether wart models (as cutout crops: a modded block that borrows vanilla's
        // models doesn't get vanilla's render type with them)
        final ModelFile[] wartStages = new ModelFile[3];
        for (int stage = 0; stage < 3; stage++) {
            wartStages[stage] = models().crop("space_wart_stage" + stage, mcLoc("block/nether_wart_stage" + stage)).renderType("cutout");
        }
        getVariantBuilder(GCBlocks.SPACE_WART.get()).forAllStates(state -> {
            final int age = state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE);
            return ConfiguredModel.builder().modelFile(wartStages[age == 0 ? 0 : age == 3 ? 2 : 1]).build();
        });
    }

    private void wire(Block block, String modelPrefix, String itemTexture) {
        final ModelFile core = new ModelFile.UncheckedModelFile(modLoc("block/" + modelPrefix + "_core"));
        final ModelFile side = new ModelFile.UncheckedModelFile(modLoc("block/" + modelPrefix + "_side"));
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

    /** 1.12.2 collector/compressor/decompressor/distributor/sealer/detector models (front north, power port east). */
    private void oxygenMachines() {
        final ResourceLocation machine = tex("machine");
        final ResourceLocation power = tex("machine_input");
        final ResourceLocation in = tex("machine_oxygen_input");
        final ResourceLocation out = tex("machine_oxygen_output");
        final ResourceLocation collector = tex("oxygen_collector");
        oxygenMachine(GCBlocks.OXYGEN_COLLECTOR.get(), collector, collector, collector, collector, power, out);
        oxygenMachine(GCBlocks.OXYGEN_COMPRESSOR.get(), machine, machine, tex("oxygen_compressor"), tex("oxygen_compressor_back"), power, in);
        oxygenMachine(GCBlocks.OXYGEN_DECOMPRESSOR.get(), machine, machine, tex("oxygen_decompressor"), tex("oxygen_compressor_back"), power, out);
        oxygenMachine(GCBlocks.OXYGEN_DISTRIBUTOR.get(), machine, machine, tex("oxygen_distributor"), tex("oxygen_distributor"), power, in);
        oxygenMachine(GCBlocks.OXYGEN_SEALER.get(), machine, tex("oxygen_sealer"), machine, machine, power, in);
        final ResourceLocation detector = tex("oxygen_detector");
        simpleBlockWithItem(GCBlocks.OXYGEN_DETECTOR.get(), models().cube("oxygen_detector", machine, machine, detector, detector, detector, detector)
                .texture("particle", machine));

        // Oxygen Storage Module: tank front and back, oxygen in/out on the configured faces, casing elsewhere
        final ResourceLocation tank = tex("oxygen_storage_module");
        final ResourceLocation side = tex("machine_side");
        final Map<StorageSides, ModelFile> storage = new EnumMap<>(StorageSides.class);
        for (StorageSides sides : StorageSides.values()) {
            final String name = sides == StorageSides.LEFT_RIGHT ? "oxygen_storage_module" : "oxygen_storage_module_" + sides.getSerializedName();
            storage.put(sides, models().cube(name, machine, machine, tank,
                    oxygenFace(sides, RelativeSide.BACK, tank), oxygenFace(sides, RelativeSide.LEFT, side), oxygenFace(sides, RelativeSide.RIGHT, side))
                    .texture("particle", machine));
        }
        getVariantBuilder(GCBlocks.OXYGEN_STORAGE_MODULE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(storage.get(state.getValue(OxygenStorageModuleBlock.SIDES))).rotationY(facingRotation(state)).build());
        itemModels().withExistingParent("oxygen_storage_module", modLoc("block/oxygen_storage_module"));

        // Breathable air is invisible; the model only exists for the particle sprite
        simpleBlock(GCBlocks.BREATHABLE_AIR.get(), models().getBuilder("breathable_air").texture("particle", tex("fluids/oxygen_gas")));

        // Space torches: 1.12.2 unlit_torch / unlit_torch_lit textures on the vanilla torch shapes
        final ModelFile[] standing = {models().torch("unlit_torch", tex("unlit_torch")).renderType("cutout"),
                models().torch("unlit_torch_lit", tex("unlit_torch_lit")).renderType("cutout")};
        final ModelFile[] wall = {models().torchWall("unlit_wall_torch", tex("unlit_torch")).renderType("cutout"),
                models().torchWall("unlit_wall_torch_lit", tex("unlit_torch_lit")).renderType("cutout")};
        getVariantBuilder(GCBlocks.UNLIT_TORCH.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(standing[state.getValue(SpaceTorchBlock.LIT) ? 1 : 0]).build());
        getVariantBuilder(GCBlocks.UNLIT_WALL_TORCH.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(wall[state.getValue(SpaceTorchBlock.LIT) ? 1 : 0])
                .rotationY(((int) state.getValue(net.minecraft.world.level.block.WallTorchBlock.FACING).toYRot() + 90) % 360).build());
        itemModels().withExistingParent("unlit_torch", mcLoc("item/generated")).texture("layer0", tex("unlit_torch_lit"));
    }

    private void oxygenMachine(Block block, ResourceLocation down, ResourceLocation up, ResourceLocation north, ResourceLocation south,
                               ResourceLocation east, ResourceLocation west) {
        final String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        horizontalBlock(block, models().cube(name, down, up, north, south, east, west).texture("particle", down));
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    private ResourceLocation oxygenFace(StorageSides sides, RelativeSide face, ResourceLocation unused) {
        if (sides.input() == face) {
            return tex("machine_oxygen_input");
        }
        return sides.output() == face ? tex("machine_oxygen_output") : unused;
    }

    /** 1.12.2 tank / tank_u / tank_d / tank_ud: caps where no tank is stacked above or below. */
    private void fluidTank() {
        final ModelFile single = models().getExistingFile(modLoc("block/fluid_tank_single"));
        final ModelFile top = models().getExistingFile(modLoc("block/fluid_tank_top"));
        final ModelFile bottom = models().getExistingFile(modLoc("block/fluid_tank_bottom"));
        final ModelFile middle = models().getExistingFile(modLoc("block/fluid_tank_middle"));
        getVariantBuilder(GCBlocks.FLUID_TANK.get()).forAllStates(state -> {
            final boolean up = state.getValue(FluidTankBlock.UP);
            final boolean down = state.getValue(FluidTankBlock.DOWN);
            return ConfiguredModel.builder().modelFile(up ? (down ? middle : bottom) : (down ? top : single)).build();
        });
        itemModels().withExistingParent("fluid_tank", modLoc("block/fluid_tank_single"));
    }

    /** Multipart per colour: the 1.12.2 core and north arm with that colour's pipe texture, arms rotated like wires. */
    private void fluidPipe() {
        final MultiPartBlockStateBuilder builder = getMultipartBuilder(GCBlocks.FLUID_PIPE.get());
        for (DyeColor color : DyeColor.values()) {
            final ResourceLocation texture = tex("pipe/" + color.getSerializedName());
            final ModelFile core = color == DyeColor.WHITE ? models().getExistingFile(modLoc("block/fluid_pipe_core"))
                    : models().withExistingParent("fluid_pipe_core_" + color.getSerializedName(), modLoc("block/fluid_pipe_core"))
                    .texture("pipeTex", texture).texture("particle", texture).renderType("cutout");
            final ModelFile side = color == DyeColor.WHITE ? models().getExistingFile(modLoc("block/fluid_pipe_side"))
                    : models().withExistingParent("fluid_pipe_side_" + color.getSerializedName(), modLoc("block/fluid_pipe_side"))
                    .texture("pipeTex", texture).texture("particle", texture).renderType("cutout");
            builder.part().modelFile(core).addModel().condition(FluidPipeBlock.COLOR, color).end();
            builder.part().modelFile(side).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.NORTH), true).end();
            builder.part().modelFile(side).rotationY(90).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.EAST), true).end();
            builder.part().modelFile(side).rotationY(180).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.SOUTH), true).end();
            builder.part().modelFile(side).rotationY(270).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.WEST), true).end();
            builder.part().modelFile(side).rotationX(270).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.UP), true).end();
            builder.part().modelFile(side).rotationX(90).addModel().condition(FluidPipeBlock.COLOR, color)
                    .condition(FluidPipeBlock.CONNECTIONS.get(Direction.DOWN), true).end();
        }
        itemModels().withExistingParent("fluid_pipe", modLoc("block/fluid_pipe_inventory"));
    }

    /** Liquid blocks are drawn by the fluid renderer; the model only gives break/landing particles. */
    private void liquid(Block block, String name) {
        simpleBlock(block, models().getBuilder(name).texture("particle", tex("fluids/" + name + "_still")));
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
