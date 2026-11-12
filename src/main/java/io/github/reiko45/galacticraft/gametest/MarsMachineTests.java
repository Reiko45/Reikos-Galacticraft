package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.CryogenicChamberBlock;
import io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock;
import io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity;
import io.github.reiko45.galacticraft.event.CryogenicSleep;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.Atmosphere;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7c: the Mars gas machines, canisters, the Terraformer and the Cryogenic Chamber. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MarsMachineTests {
    private static final String AIR = "air_room";

    private MarsMachineTests() {
    }

    // --- gases and canisters ---

    /** 1.12.2 atmospheres: Mars air is carbon dioxide, argon and nitrogen; the Moon and orbit have none. */
    @GameTest(template = "empty")
    public static void atmospheres(GameTestHelper helper) {
        helper.assertValueEqual(Atmosphere.of(GCDimensions.MARS), List.of(Atmosphere.Gas.CO2, Atmosphere.Gas.ARGON, Atmosphere.Gas.NITROGEN), "Mars");
        helper.assertTrue(Atmosphere.of(GCDimensions.MOON).isEmpty(), "Moon has no air");
        helper.assertTrue(Atmosphere.of(net.minecraft.world.level.Level.OVERWORLD) == null, "not a Galacticraft world");
        helper.succeed();
    }

    /** An empty canister takes any canister fluid and becomes that canister; drained, it is an empty canister again. */
    @GameTest(template = "empty")
    public static void canistersChangeWithTheirContents(GameTestHelper helper) {
        final ItemStack empty = new ItemStack(GCItems.OIL_CANISTER.get());
        final IFluidHandlerItem handler = empty.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(handler != null, "canisters hold fluid");
        helper.assertValueEqual(handler.fill(new FluidStack(GCFluids.LIQUID_OXYGEN.get(), 600), IFluidHandler.FluidAction.EXECUTE), 600, "filled");
        helper.assertTrue(handler.getContainer().is(GCItems.LIQUID_OXYGEN_CANISTER.get()), "now a liquid oxygen canister");
        helper.assertValueEqual(handler.fill(new FluidStack(GCFluids.FUEL.get(), 100), IFluidHandler.FluidAction.EXECUTE), 0, "no mixing");
        helper.assertValueEqual(handler.fill(new FluidStack(GCFluids.ARGON.get(), 100), IFluidHandler.FluidAction.EXECUTE), 0, "no argon canister");
        helper.assertValueEqual(handler.drain(600, IFluidHandler.FluidAction.EXECUTE).getAmount(), 600, "drained");
        helper.assertTrue(handler.getContainer().is(GCItems.OIL_CANISTER.get()) && LiquidCanisterItem.isEmpty(handler.getContainer()), "empty again");
        helper.assertTrue(LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE).is(GCItems.METHANE_CANISTER.get()), "methane canister");
        helper.succeed();
    }

    // --- Water Electrolyzer, Methane Synthesizer, Gas Liquefier ---

    /** 1.12.2 TileEntityElectrolyzer: each mB of water makes 2 of oxygen and 4 of hydrogen. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void electrolyzerSplitsWater(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.ELECTROLYZER.get());
        final ElectrolyzerBlockEntity machine = (ElectrolyzerBlockEntity) helper.getBlockEntity(pos);
        helper.assertValueEqual(machine.status(), MachineStatus.LOW_ENERGY, "no power");
        machine.energy().setStored(16_000.0F);
        helper.assertValueEqual(machine.status(), MachineStatus.NO_WATER, "no water");
        machine.inventory().setStackInSlot(1, new ItemStack(Items.WATER_BUCKET));
        helper.runAfterDelay(1, () -> helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.BUCKET), "bucket emptied"));
        helper.succeedWhen(() -> {
            final int oxygen = machine.tanks()[1].getFluidAmount();
            final int hydrogen = machine.tanks()[2].getFluidAmount();
            helper.assertTrue(oxygen >= 20, "oxygen " + oxygen);
            helper.assertValueEqual(hydrogen, oxygen * 2, "twice as much hydrogen");
            helper.assertValueEqual(machine.tanks()[0].getFluidAmount() + oxygen / 2, 1000, "1 water per 2 oxygen");
            helper.assertTrue(machine.tanks()[1].getFluid().is(GCFluids.OXYGEN.get()) && machine.tanks()[2].getFluid().is(GCFluids.HYDROGEN.get()),
                    "oxygen and hydrogen");
        });
    }

    /** The electrolyzer's hydrogen goes out its right side into a synthesizer, which makes methane with carbon. */
    @GameTest(template = AIR, timeoutTicks = 300)
    public static void synthesizerMakesMethaneFromHydrogenAndCarbon(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.METHANE_SYNTHESIZER.get());
        final MethaneSynthesizerBlockEntity machine = (MethaneSynthesizerBlockEntity) helper.getBlockEntity(pos);
        machine.energy().setStored(16_000.0F);
        helper.assertFalse(machine.airHasCarbonDioxide(), "no CO2 in this world's air");
        machine.tanks()[0].fill(new FluidStack(GCFluids.HYDROGEN.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(machine.status(), MachineStatus.NEEDS_CARBON, "needs carbon");
        machine.inventory().setStackInSlot(3, new ItemStack(GCItems.CARBON_FRAGMENTS.get(), 2));
        helper.succeedWhen(() -> {
            final int methane = machine.tanks()[2].getFluidAmount();
            helper.assertTrue(methane >= 20 && machine.tanks()[2].getFluid().is(GCFluids.METHANE.get()), "methane " + methane);
            helper.assertValueEqual(machine.tanks()[0].getFluidAmount(), 4000 - methane * 8, "8 hydrogen per methane");
            helper.assertValueEqual(machine.inventory().getStackInSlot(3).getCount(), 1, "one fragment lasts 40 uses");
        });
    }

    /** 1.12.2 TileEntityGasLiquefier: methane becomes fuel, oxygen liquid oxygen; overworld air is nitrogen and oxygen. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void liquefierTurnsMethaneIntoFuel(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.GAS_LIQUEFIER.get());
        final GasLiquefierBlockEntity machine = (GasLiquefierBlockEntity) helper.getBlockEntity(pos);
        helper.assertValueEqual(machine.airProducts(), List.of(GasLiquefierBlockEntity.Gas.NITROGEN, GasLiquefierBlockEntity.Gas.OXYGEN), "air");
        machine.energy().setStored(16_000.0F);
        machine.inventory().setStackInSlot(1, LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE));
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.tanks()[1].getFluid().is(GCFluids.FUEL.get()) && machine.tanks()[1].getFluidAmount() >= 20,
                    "fuel " + machine.tanks()[1].getFluid().getAmount());
            helper.assertTrue(machine.inventory().getStackInSlot(1).getItem() instanceof LiquidCanisterItem, "the canister stays");
        });
    }

    // --- Terraformer ---

    @GameTest(template = AIR)
    public static void terraformableBlocks(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, GCBlocks.MARS_FINE_REGOLITH.get());
        helper.setBlock(3, 1, 1, GCBlocks.MARS_FINE_REGOLITH.get());
        helper.setBlock(3, 2, 1, Blocks.STONE);
        helper.setBlock(1, 1, 3, GCBlocks.MOON_TURF.get());
        helper.setBlock(3, 1, 3, GCBlocks.MARS_REGOLITH.get());
        helper.assertTrue(TerraformerBlockEntity.isTerraformable(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))), "open fine regolith");
        helper.assertFalse(TerraformerBlockEntity.isTerraformable(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 1))), "covered");
        helper.assertTrue(TerraformerBlockEntity.isTerraformable(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 3))), "Moon turf");
        helper.assertFalse(TerraformerBlockEntity.isTerraformable(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3))), "plain regolith");
        helper.assertTrue(TerraformerBlockEntity.isPlant(new ItemStack(Items.OAK_SAPLING)) && TerraformerBlockEntity.isPlant(new ItemStack(Items.DANDELION))
                && !TerraformerBlockEntity.isPlant(new ItemStack(Items.STONE)), "plants");
        helper.succeed();
    }

    /**
     * A full-size bubble with power, water, bone meal and seeds turns the regolith inside into grass, a block every 15
     * ticks, then plants on the grass. Run alone: the 15-block bubble reaches into neighbouring tests.
     */
    @GameTest(template = AIR, timeoutTicks = 400, batch = "terraformer")
    public static void terraformerGreensRegolith(GameTestHelper helper) {
        for (int x = 1; x < 12; x++) {
            for (int z = 1; z < 12; z++) {
                helper.setBlock(x, 1, z, GCBlocks.MARS_FINE_REGOLITH.get());
            }
        }
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.TERRAFORMER.get());
        final TerraformerBlockEntity machine = (TerraformerBlockEntity) helper.getBlockEntity(pos);
        machine.energy().setStored(16_000.0F);
        machine.waterTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        machine.inventory().setStackInSlot(2, new ItemStack(Items.BONE_MEAL, 16));
        machine.inventory().setStackInSlot(10, new ItemStack(Items.WHEAT_SEEDS, 16));
        machine.inventory().setStackInSlot(6, new ItemStack(Items.DANDELION, 4));
        machine.setBubbleSize(TerraformerBlockEntity.MAX_SIZE);
        helper.succeedWhen(() -> {
            int grass = 0;
            for (int x = 1; x < 12; x++) {
                for (int z = 1; z < 12; z++) {
                    grass += helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK) ? 1 : 0;
                }
            }
            // Planted on any grass in the bubble, the test world's own grass outside the room included
            int flowers = 0;
            for (BlockPos at : BlockPos.betweenClosed(helper.absolutePos(pos).offset(-15, -15, -15), helper.absolutePos(pos).offset(15, 15, 15))) {
                flowers += helper.getLevel().getBlockState(at).is(Blocks.DANDELION)
                        && helper.getLevel().getBlockState(at.below()).is(Blocks.GRASS_BLOCK) ? 1 : 0;
            }
            helper.assertTrue(machine.isActive(), "terraforming");
            helper.assertTrue(grass >= 6, "grass blocks: " + grass);
            helper.assertTrue(flowers >= 1, "planted: " + flowers);
            helper.assertTrue(machine.waterTank().getFluidAmount() < 2000 - 50, "used water");
            helper.assertTrue(machine.inventory().getStackInSlot(6).getCount() < 4, "used a plant");
        });
    }

    /** Without power the bubble shrinks and nothing is terraformed. */
    @GameTest(template = AIR, timeoutTicks = 100, batch = "terraformer")
    public static void terraformerBubbleNeedsPower(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.TERRAFORMER.get());
        final TerraformerBlockEntity machine = (TerraformerBlockEntity) helper.getBlockEntity(pos);
        machine.setBubbleSize(5.0F);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(machine.bubbleSize() < 5.0F, "shrinking: " + machine.bubbleSize());
            helper.assertFalse(machine.isActive(), "idle");
            machine.energy().setStored(16_000.0F);
            final float before = machine.bubbleSize();
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(machine.bubbleSize() > before, "growing with power");
                helper.succeed();
            });
        });
    }

    // --- Cryogenic Chamber ---

    private static BlockPos placeChamber(GameTestHelper helper, BlockPos relative, Direction facing) {
        final BlockPos pos = helper.absolutePos(relative);
        final BlockState state = GCBlocks.CRYOGENIC_CHAMBER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        helper.getLevel().setBlockAndUpdate(pos, state);
        state.getBlock().setPlacedBy(helper.getLevel(), pos, state, null, new ItemStack(GCBlocks.CRYOGENIC_CHAMBER.get()));
        return pos;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        return player;
    }

    /** Three blocks tall; breaking any part breaks it all and drops the chamber. */
    @GameTest(template = AIR)
    public static void cryogenicChamberIsThreeBlocksTall(GameTestHelper helper) {
        placeChamber(helper, new BlockPos(6, 1, 6), Direction.SOUTH);
        helper.assertBlockState(new BlockPos(6, 2, 6), state -> state.is(GCBlocks.CRYOGENIC_CHAMBER_PART.get())
                && !state.getValue(CryogenicChamberPartBlock.TOP) && state.getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH, () -> "middle");
        helper.assertBlockState(new BlockPos(6, 3, 6), state -> state.is(GCBlocks.CRYOGENIC_CHAMBER_PART.get())
                && state.getValue(CryogenicChamberPartBlock.TOP), () -> "top");
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(6, 3, 6)), true);
        helper.assertBlockNotPresent(GCBlocks.CRYOGENIC_CHAMBER.get(), new BlockPos(6, 1, 6));
        helper.assertBlockNotPresent(GCBlocks.CRYOGENIC_CHAMBER_PART.get(), new BlockPos(6, 2, 6));
        final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(15.0D),
                item -> item.getItem().is(GCBlocks.CRYOGENIC_CHAMBER.get().asItem()));
        helper.assertValueEqual(drops.size(), 1, "one chamber dropped");
        helper.succeed();
    }

    /**
     * 1.12.2 sleepInBedAt and onPlayerWakeUp: sleep any time, standing up; sleeping the night through heals 5 and
     * starts a 6000 tick cooldown; getting out early doesn't.
     */
    @GameTest(template = AIR)
    public static void cryogenicChamberSleepAndCooldown(GameTestHelper helper) {
        final BlockPos pos = placeChamber(helper, new BlockPos(6, 1, 6), Direction.EAST);
        final ServerPlayer player = player(helper);
        player.setHealth(10.0F);
        helper.assertTrue(CryogenicChamberBlock.enter(player, pos), "got in");
        helper.assertTrue(player.isSleeping() && pos.equals(player.getSleepingPos().orElse(null)), "asleep in the chamber");
        helper.assertTrue(player.getBbHeight() > 1.7F && Math.abs(player.getEyeHeight() - CryogenicSleep.IN_CHAMBER.eyeHeight()) < 0.01F,
                "standing size, height " + player.getBbHeight());
        helper.assertTrue(EventHooks.canEntityContinueSleeping(player, Player.BedSleepingProblem.NOT_POSSIBLE_NOW), "may sleep by day");
        helper.assertValueEqual(player.getBedOrientation(), Direction.WEST, "camera looks out of the opening");
        player.stopSleepInBed(false, false); // the night skipped
        helper.assertFalse(player.isSleeping(), "awake");
        helper.assertValueEqual(player.getHealth(), 15.0F, "healed 5");
        helper.assertValueEqual(GCPlayerData.get(player).cryogenicChamberCooldown, CryogenicChamberBlock.COOLDOWN, "cooldown");
        helper.assertFalse(CryogenicChamberBlock.enter(player, pos), "cooling down");
        helper.assertFalse(player.isSleeping(), "not asleep again");

        final ServerPlayer other = player(helper);
        helper.assertTrue(CryogenicChamberBlock.enter(other, pos), "another player gets in");
        other.stopSleepInBed(true, true); // left early
        helper.assertValueEqual(GCPlayerData.get(other).cryogenicChamberCooldown, 0, "no cooldown for leaving");
        helper.succeed();
    }
}
