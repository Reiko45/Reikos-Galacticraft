package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPanelBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 2 machines and energy network. A machine facing NORTH has its left face (energy port) on the EAST;
 * one facing SOUTH has it on the WEST.
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineTests {
    private static final String ROOM = "machine_room";

    private MachineTests() {
    }

    @SuppressWarnings("unchecked")
    private static <T extends MachineBlockEntity> T place(GameTestHelper helper, Block block, int x, Direction facing) {
        final BlockPos pos = new BlockPos(x, 1, 1);
        helper.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
        return (T) helper.getBlockEntity(pos);
    }

    private static void wires(GameTestHelper helper, Block wire, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), wire);
        }
    }

    /** Storage Module -> 2 aluminum wires -> Electric Furnace smelts raw iron (network, sided I/O, consumption). */
    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void storageModulePowersFurnaceOverWire(GameTestHelper helper) {
        final EnergyStorageBlockEntity module = place(helper, GCBlocks.ENERGY_STORAGE_MODULE.get(), 0, Direction.NORTH);
        module.energy().setStored(100_000.0F);
        wires(helper, GCBlocks.ALUMINUM_WIRE.get(), 1, 2);
        final ElectricFurnaceBlockEntity furnace = place(helper, GCBlocks.ELECTRIC_FURNACE.get(), 3, Direction.SOUTH);
        furnace.inventory().setStackInSlot(1, new ItemStack(Items.RAW_IRON, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(furnace.inventory().getStackInSlot(2).is(Items.IRON_INGOT), "No iron ingot yet");
            helper.assertTrue(furnace.processTime() == 130, "Tier-1 electric furnace should take 130 ticks");
            helper.assertTrue(module.energy().getStoredGJ() < 100_000.0F, "Module was not drained");
        });
    }

    /** Cluster (tier 2) over heavy wire powers the Arc Furnace at tier 2: 32 ticks and doubled ore smelting. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void clusterOverHeavyWireGivesTier2(GameTestHelper helper) {
        final EnergyStorageBlockEntity cluster = place(helper, GCBlocks.ENERGY_STORAGE_CLUSTER.get(), 0, Direction.NORTH);
        cluster.energy().setStored(100_000.0F);
        wires(helper, GCBlocks.HEAVY_ALUMINUM_WIRE.get(), 1, 2);
        final ElectricFurnaceBlockEntity arc = place(helper, GCBlocks.ELECTRIC_ARC_FURNACE.get(), 3, Direction.SOUTH);
        arc.inventory().setStackInSlot(1, new ItemStack(Items.RAW_IRON));
        helper.succeedWhen(() -> {
            final int ingots = arc.inventory().getStackInSlot(2).getCount() + arc.inventory().getStackInSlot(3).getCount();
            helper.assertTrue(ingots == 2, "Arc furnace should turn 1 raw iron into 2 ingots, has " + ingots);
            helper.assertTrue(arc.poweredTier() == 2 && arc.processTime() == 32, "Expected tier 2 / 32 ticks, got tier "
                    + arc.poweredTier() + " / " + arc.processTime());
        });
    }

    /** A thin wire anywhere on the network limits delivery to tier 1. */
    @GameTest(template = ROOM, timeoutTicks = 100)
    public static void thinWireLimitsTier(GameTestHelper helper) {
        final EnergyStorageBlockEntity cluster = place(helper, GCBlocks.ENERGY_STORAGE_CLUSTER.get(), 0, Direction.NORTH);
        cluster.energy().setStored(100_000.0F);
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.HEAVY_ALUMINUM_WIRE.get());
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.ALUMINUM_WIRE.get());
        final ElectricFurnaceBlockEntity arc = place(helper, GCBlocks.ELECTRIC_ARC_FURNACE.get(), 3, Direction.SOUTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(arc.energy().getStoredGJ() > 0.0F, "Arc furnace received no energy");
            helper.assertTrue(arc.poweredTier() == 1 && arc.processTime() == 65, "Expected tier 1 / 65 ticks");
        });
    }

    /**
     * A Storage Module directly against an Electric Compressor (no wire) powers it; 4 tin ingots in two grid slots
     * compress into 2 Compressed Tin in one cycle (both output slots fill).
     */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void directlyPoweredElectricCompressor(GameTestHelper helper) {
        final EnergyStorageBlockEntity module = place(helper, GCBlocks.ENERGY_STORAGE_MODULE.get(), 0, Direction.NORTH);
        module.energy().setStored(100_000.0F);
        final ElectricCompressorBlockEntity compressor = place(helper, GCBlocks.ELECTRIC_COMPRESSOR.get(), 1, Direction.SOUTH);
        compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START, new ItemStack(GCItems.TIN_INGOT.get(), 2));
        compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START + 1, new ItemStack(GCItems.TIN_INGOT.get(), 2));
        helper.succeedWhen(() -> {
            final int made = compressor.inventory().getStackInSlot(1).getCount() + compressor.inventory().getStackInSlot(2).getCount();
            helper.assertTrue(made == 2, "Expected 2 compressed tin, got " + made);
            helper.assertTrue(compressor.inventory().getStackInSlot(1).is(GCItems.COMPRESSED_TIN.get()), "Wrong output");
        });
    }

    /** Fuel heats the Coal Generator past 30 gJ/t before it outputs anything (1.12.2 hull heat). */
    @GameTest(template = ROOM, timeoutTicks = 300)
    public static void coalGeneratorWarmsUpThenGenerates(GameTestHelper helper) {
        final CoalGeneratorBlockEntity generator = place(helper, GCBlocks.COAL_GENERATOR.get(), 0, Direction.NORTH);
        generator.inventory().setStackInSlot(0, new ItemStack(Items.COAL));
        helper.runAfterDelay(50, () -> helper.assertTrue(generator.energy().getStoredGJ() == 0.0F,
                "Generator produced energy before warming up"));
        helper.succeedWhen(() -> helper.assertTrue(generator.energy().getStoredGJ() > 0.0F, "Generator never produced energy"));
    }

    /** Coal-burning Compressor: 2 iron ingots + coal -> Compressed Iron after 200 ticks. */
    @GameTest(template = ROOM, timeoutTicks = 300)
    public static void fuelCompressorCompressesIron(GameTestHelper helper) {
        final CompressorBlockEntity compressor = place(helper, GCBlocks.COMPRESSOR.get(), 0, Direction.NORTH);
        compressor.inventory().setStackInSlot(0, new ItemStack(Items.COAL));
        compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START, new ItemStack(Items.IRON_INGOT));
        compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START + 4, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> helper.assertTrue(compressor.inventory().getStackInSlot(1).is(GCItems.COMPRESSED_IRON.get()),
                "No compressed iron yet"));
    }

    /** Circuit Fabricator running from a charged battery makes 3 Basic Wafers. */
    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void batteryPoweredCircuitFabricator(GameTestHelper helper) {
        final CircuitFabricatorBlockEntity fabricator = place(helper, GCBlocks.CIRCUIT_FABRICATOR.get(), 0, Direction.NORTH);
        fabricator.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
        fabricator.inventory().setStackInSlot(1, new ItemStack(Items.DIAMOND));
        fabricator.inventory().setStackInSlot(2, new ItemStack(GCItems.RAW_SILICON.get()));
        fabricator.inventory().setStackInSlot(3, new ItemStack(GCItems.RAW_SILICON.get()));
        fabricator.inventory().setStackInSlot(4, new ItemStack(Items.REDSTONE));
        fabricator.inventory().setStackInSlot(5, new ItemStack(Items.REDSTONE_TORCH));
        helper.succeedWhen(() -> {
            final ItemStack out = fabricator.inventory().getStackInSlot(CircuitFabricatorBlockEntity.OUTPUT);
            helper.assertTrue(out.is(GCItems.BASIC_WAFER.get()) && out.getCount() == 3, "Expected 3 basic wafers, got " + out);
            helper.assertTrue(fabricator.processTime() == 300, "Battery power is tier 1: 300 ticks");
        });
    }

    /** Sneak + wrench cycling: storage (R,L) -> (R,B) moves the output to the back; a consumer's input LEFT -> BACK. */
    @GameTest(template = ROOM)
    public static void sideConfigurationMovesEnergyFaces(GameTestHelper helper) {
        final EnergyStorageBlockEntity module = place(helper, GCBlocks.ENERGY_STORAGE_MODULE.get(), 0, Direction.NORTH);
        final BlockPos modulePos = helper.absolutePos(new BlockPos(0, 1, 1));
        helper.assertTrue(feCanExtract(helper, modulePos, Direction.EAST), "Default output should be the left (east) face");
        ((MachineBlock) module.getBlockState().getBlock()).cycleSides(helper.getLevel(), modulePos, module.getBlockState());
        helper.assertTrue(feCanExtract(helper, modulePos, Direction.SOUTH), "Output should move to the back (south)");
        helper.assertTrue(fe(helper, modulePos, Direction.EAST) == null, "Left face should be unused after cycling");
        helper.assertTrue(fe(helper, modulePos, Direction.WEST) != null && fe(helper, modulePos, Direction.WEST).canReceive(),
                "Input should stay on the right (west)");

        final ElectricFurnaceBlockEntity furnace = place(helper, GCBlocks.ELECTRIC_FURNACE.get(), 3, Direction.NORTH);
        final BlockPos furnacePos = helper.absolutePos(new BlockPos(3, 1, 1));
        ((MachineBlock) furnace.getBlockState().getBlock()).cycleSides(helper.getLevel(), furnacePos, furnace.getBlockState());
        helper.assertTrue(fe(helper, furnacePos, Direction.EAST) == null, "Furnace input should have left the east face");
        helper.assertTrue(fe(helper, furnacePos, Direction.SOUTH) != null, "Furnace input should now be the back (south)");
        helper.succeed();
    }

    /** A redstone-powered Switchable Aluminum Wire cuts the network; removing the signal reconnects it. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void switchWireCutsPower(GameTestHelper helper) {
        final EnergyStorageBlockEntity module = place(helper, GCBlocks.ENERGY_STORAGE_MODULE.get(), 0, Direction.NORTH);
        module.energy().setStored(100_000.0F);
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.ALUMINUM_WIRE.get());
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.ALUMINUM_WIRE_SWITCH.get());
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.REDSTONE_BLOCK);
        final ElectricFurnaceBlockEntity furnace = place(helper, GCBlocks.ELECTRIC_FURNACE.get(), 3, Direction.SOUTH);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(furnace.energy().getStoredGJ() == 0.0F, "Power crossed a switched-off wire");
            helper.setBlock(new BlockPos(2, 2, 1), Blocks.AIR);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 40, "Waiting for the switch to be turned back on");
            helper.assertTrue(furnace.energy().getStoredGJ() > 0.0F, "No power after the switch was turned back on");
        });
    }

    private static SolarPanelBlockEntity placeSolar(GameTestHelper helper, Block block, BlockPos rel) {
        final BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
        helper.setBlock(rel, state);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(rel), state, null, new ItemStack(block));
        return (SolarPanelBlockEntity) helper.getBlockEntity(rel);
    }

    /** Placing a solar panel adds a pole and a 3x3 panel; breaking any part removes it all and drops the panel item. */
    @GameTest(template = ROOM)
    public static void solarPanelIsOneMultiblock(GameTestHelper helper) {
        final BlockPos base = new BlockPos(3, 0, 1);
        placeSolar(helper, GCBlocks.BASIC_SOLAR_PANEL.get(), base);
        helper.assertBlockPresent(GCBlocks.SOLAR_PART.get(), base.above());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.assertBlockPresent(GCBlocks.SOLAR_PART.get(), base.offset(x, 2, z));
            }
        }
        helper.getLevel().destroyBlock(helper.absolutePos(base.offset(1, 2, 1)), true);
        helper.assertBlockNotPresent(GCBlocks.BASIC_SOLAR_PANEL.get(), base);
        helper.assertBlockNotPresent(GCBlocks.SOLAR_PART.get(), base.above());
        helper.assertBlockNotPresent(GCBlocks.SOLAR_PART.get(), base.offset(-1, 2, -1));
        helper.assertItemEntityPresent(GCBlocks.BASIC_SOLAR_PANEL.get().asItem(), base, 3.0D);
        helper.succeed();
    }

    /** In clear daylight a basic panel generates power. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void solarPanelGeneratesInDaylight(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        final SolarPanelBlockEntity panel = placeSolar(helper, GCBlocks.BASIC_SOLAR_PANEL.get(), new BlockPos(3, 0, 1));
        helper.succeedWhen(() -> helper.assertTrue(panel.energy().getStoredGJ() > 0.0F, "Solar panel generated nothing at noon"));
    }

    private static IEnergyStorage fe(GameTestHelper helper, BlockPos absolute, Direction side) {
        return helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, absolute, side);
    }

    private static boolean feCanExtract(GameTestHelper helper, BlockPos absolute, Direction side) {
        final IEnergyStorage storage = fe(helper, absolute, side);
        return storage != null && storage.canExtract();
    }

    /** FE interop: 1 FE = 1.6 gJ, and the furnace only takes 2.5 x 45 = 112.5 gJ per call, i.e. 70 whole FE. */
    @GameTest(template = ROOM)
    public static void feConversion(GameTestHelper helper) {
        final ElectricFurnaceBlockEntity furnace = place(helper, GCBlocks.ELECTRIC_FURNACE.get(), 0, Direction.NORTH);
        final IEnergyStorage fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(new BlockPos(0, 1, 1)), Direction.EAST);
        helper.assertTrue(fe != null, "No FE capability on the furnace's input face");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(new BlockPos(0, 1, 1)), Direction.WEST) == null, "The furnace's right face should not take energy");
        final int accepted = fe.receiveEnergy(1000, false);
        helper.assertTrue(accepted == 70, "Expected 70 FE accepted, got " + accepted);
        helper.assertTrue(Math.abs(furnace.energy().getStoredGJ() - 112.0F) < 0.01F, "Expected 112 gJ stored, got " + furnace.energy().getStoredGJ());
        helper.succeed();
    }
}
