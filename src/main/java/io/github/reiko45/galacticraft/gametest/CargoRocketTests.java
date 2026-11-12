package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.CargoLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CargoUnloaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import io.github.reiko45.galacticraft.entity.CargoCarrier;
import io.github.reiko45.galacticraft.entity.CargoRocketEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.item.RocketItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7c: Cargo Loader / Unloader, Launch Controller and the Cargo Rocket. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CargoRocketTests {
    private static final String AIR = "air_room";
    private static final BlockPos PAD_A = new BlockPos(3, 1, 3);
    private static final BlockPos PAD_B = new BlockPos(9, 1, 9);

    private CargoRocketTests() {
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void pad(GameTestHelper helper, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), GCBlocks.LANDING_PAD.get());
            }
        }
    }

    private static CargoRocketEntity rocketOn(GameTestHelper helper, BlockPos centre, int storage, int fuel) {
        final BlockPos abs = helper.absolutePos(centre);
        final CargoRocketEntity rocket = CargoRocketEntity.create(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 0.4D + CargoRocketEntity.PAD_OFFSET,
                abs.getZ() + 0.5D, storage, fuel);
        helper.getLevel().addFreshEntity(rocket);
        return rocket;
    }

    private static LaunchControllerBlockEntity controller(GameTestHelper helper, BlockPos pos, int frequency, int target) {
        helper.setBlock(pos, GCBlocks.LAUNCH_CONTROLLER.get());
        final LaunchControllerBlockEntity controller = (LaunchControllerBlockEntity) helper.getBlockEntity(pos);
        controller.energy().setStored(16_000.0F);
        controller.setFrequency(frequency);
        controller.setDestFrequency(target);
        return controller;
    }

    // --- cargo ---

    /** 1.12.2 EntityAutoRocket#addCargo / removeCargo: one item at a time, never into the two reserved slots. */
    @GameTest(template = "empty")
    public static void rocketsTakeCargoOneItemAtATime(GameTestHelper helper) {
        final CargoRocketEntity rocket = new CargoRocketEntity(io.github.reiko45.galacticraft.registry.GCEntities.CARGO_ROCKET.get(), helper.getLevel());
        helper.assertValueEqual(rocket.addCargo(new ItemStack(Items.STONE), true), CargoCarrier.LoadState.NO_INVENTORY, "no storage");
        rocket.setStorage(1);
        for (int i = 0; i < 18; i++) {
            rocket.cargo().setItem(i, new ItemStack(Items.DIRT, 64));
        }
        helper.assertValueEqual(rocket.addCargo(new ItemStack(Items.STONE), true), CargoCarrier.LoadState.FULL, "the last two slots are not cargo");
        rocket.cargo().setItem(5, new ItemStack(Items.STONE, 63));
        helper.assertValueEqual(rocket.addCargo(new ItemStack(Items.STONE), true), CargoCarrier.LoadState.SUCCESS, "tops up a stack");
        helper.assertValueEqual(rocket.cargo().getItem(5).getCount(), 64, "64 stone");
        final CargoCarrier.Removal removal = rocket.removeCargo(true);
        helper.assertTrue(removal.stack().is(Items.DIRT) && removal.stack().getCount() == 1 && rocket.cargo().getItem(0).getCount() == 63, "one dirt out");
        helper.assertTrue(rocket.weight() > 2.7D && rocket.weight() < 2.9D, "1 + 0.1 per filled slot: " + rocket.weight());
        helper.succeed();
    }

    /** A Cargo Loader beside the pad moves its items into the rocket on it, one every 15 ticks. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void cargoLoaderFillsTheRocket(GameTestHelper helper) {
        floor(helper);
        pad(helper, PAD_A);
        final BlockPos loaderPos = PAD_A.east(2);
        helper.setBlock(loaderPos, GCBlocks.CARGO_LOADER.get());
        final CargoLoaderBlockEntity loader = (CargoLoaderBlockEntity) helper.getBlockEntity(loaderPos);
        loader.energy().setStored(16_000.0F);
        loader.inventory().setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 10));
        final CargoRocketEntity rocket = rocketOn(helper, PAD_A, 1, 0);
        helper.succeedWhen(() -> {
            int carried = 0;
            for (int i = 0; i < rocket.cargo().getContainerSize(); i++) {
                carried += rocket.cargo().getItem(i).is(Items.COBBLESTONE) ? rocket.cargo().getItem(i).getCount() : 0;
            }
            helper.assertTrue(carried >= 4, "loaded " + carried);
            helper.assertValueEqual(loader.inventory().getStackInSlot(1).getCount() + carried, 10, "nothing lost");
            helper.assertValueEqual(loader.status(), MachineStatus.ACTIVE, "loading");
        });
    }

    /** A Cargo Unloader empties the rocket into its own slots; with nothing left it says the target is empty. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void cargoUnloaderEmptiesTheRocket(GameTestHelper helper) {
        floor(helper);
        pad(helper, PAD_A);
        final BlockPos unloaderPos = PAD_A.south(2);
        helper.setBlock(unloaderPos, GCBlocks.CARGO_UNLOADER.get());
        final CargoUnloaderBlockEntity unloader = (CargoUnloaderBlockEntity) helper.getBlockEntity(unloaderPos);
        unloader.energy().setStored(16_000.0F);
        final CargoRocketEntity rocket = rocketOn(helper, PAD_A, 1, 0);
        rocket.cargo().setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.cargo().getItem(0).isEmpty(), "rocket emptied");
            helper.assertTrue(unloader.inventory().getStackInSlot(1).is(Items.IRON_INGOT) && unloader.inventory().getStackInSlot(1).getCount() == 3,
                    "three ingots in the unloader");
            helper.assertValueEqual(unloader.status(), MachineStatus.TARGET_EMPTY, "target empty");
        });
    }

    @GameTest(template = AIR)
    public static void cargoLoaderWithoutTarget(GameTestHelper helper) {
        helper.setBlock(new BlockPos(6, 1, 6), GCBlocks.CARGO_LOADER.get());
        final CargoLoaderBlockEntity loader = (CargoLoaderBlockEntity) helper.getBlockEntity(new BlockPos(6, 1, 6));
        helper.runAfterDelay(3, () -> {
            helper.assertValueEqual(loader.status(), MachineStatus.NO_TARGET_LOAD, "no pad");
            helper.succeed();
        });
    }

    // --- Launch Controller ---

    /** 1.12.2 setFrequency: two controllers can't share a frequency; the target must be another controller's. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void launchControllerFrequencies(GameTestHelper helper) {
        floor(helper);
        final LaunchControllerBlockEntity a = controller(helper, new BlockPos(2, 1, 2), 7101, 7102);
        final LaunchControllerBlockEntity b = controller(helper, new BlockPos(10, 1, 10), 7101, -1);
        helper.runAfterDelay(2, () -> {
            helper.assertValueEqual(a.status(), MachineStatus.INVALID_FREQUENCY, "shared frequency");
            helper.assertFalse(a.validFrequency(), "not valid");
            b.setFrequency(7102);
            helper.assertTrue(a.validFrequency(), "valid once the target exists and the frequency is free (target found: "
                    + (LaunchControllerBlockEntity.withFrequency(7102) == b) + ", own found: " + (LaunchControllerBlockEntity.withFrequency(7101) == a)
                    + ", energy " + a.energy().getStoredGJ() + ", disabled " + a.isDisabled() + ")");
            helper.assertValueEqual(a.status(), MachineStatus.CONTROLLER_ACTIVE, "active");
            helper.assertTrue(LaunchControllerBlockEntity.withFrequency(7102) == b, "found by frequency");
            a.toggleDisabled();
            helper.assertFalse(a.validFrequency(), "disabled");
            helper.assertValueEqual(a.status(), MachineStatus.CONTROLLER_DISABLED, "disabled status");
            helper.succeed();
        });
    }

    /** 1.12.2 onLandingPadRemoved: a powered controller beside the pad keeps it when a rocket launches. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void launchControllerKeepsThePad(GameTestHelper helper) {
        floor(helper);
        pad(helper, PAD_A);
        final LaunchControllerBlockEntity controller = controller(helper, PAD_A.east(2), 7201, -1);
        final BlockPos centre = helper.absolutePos(PAD_A);
        helper.assertTrue(LaunchControllerBlockEntity.attachedTo(helper.getLevel(), centre) == controller, "attached to the pad");
        helper.assertTrue(LaunchControllerBlockEntity.keepsPad(helper.getLevel(), centre), "keeps the pad by default");
        controller.togglePadRemoval();
        helper.assertFalse(LaunchControllerBlockEntity.keepsPad(helper.getLevel(), centre), "set to let it go");
        helper.assertTrue(helper.absolutePos(PAD_A).equals(controller.landingPad()), "its landing pad is that pad");
        helper.succeed();
    }

    /** The Launch button without a controller: "Frequency Not Set"; without fuel: "Not Enough Fuel". */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void cargoRocketLaunchNeedsATarget(GameTestHelper helper) {
        floor(helper);
        pad(helper, PAD_A);
        final CargoRocketEntity empty = rocketOn(helper, PAD_A, 1, 0);
        empty.pressLaunch();
        helper.assertValueEqual(empty.statusCode(), CargoRocketEntity.STATUS_NOT_ENOUGH_FUEL, "no fuel");
        empty.setFuel(CargoRocketEntity.FUEL_CAPACITY);
        empty.pressLaunch();
        helper.assertValueEqual(empty.statusCode(), CargoRocketEntity.STATUS_FREQUENCY_NOT_SET, "no controller");
        helper.assertFalse(empty.statusValid(), "won't launch");
        helper.runAfterDelay(50, () -> {
            helper.assertValueEqual(empty.phase(), RocketEntity.Phase.UNIGNITED, "still on the pad");
            helper.succeed();
        });
    }

    /**
     * A full flight: the controller beside pad A, set to launch at once towards pad B's controller, sends the rocket up;
     * it comes back down 800 blocks above pad B and lands on it with its cargo, leaving pad A in place.
     */
    @GameTest(template = AIR, timeoutTicks = 1800, batch = "cargo_flight", skyAccess = true)
    public static void cargoRocketFliesToTheTargetPad(GameTestHelper helper) {
        floor(helper);
        pad(helper, PAD_A);
        pad(helper, PAD_B);
        final LaunchControllerBlockEntity home = controller(helper, PAD_A.east(2), 7301, 7302);
        controller(helper, PAD_B.north(2), 7302, 7301);
        home.setLaunchSelection(LaunchControllerBlockEntity.AutoLaunch.INSTANT);
        home.setLaunchSchedulingEnabled(true);
        final CargoRocketEntity rocket = rocketOn(helper, PAD_A, 1, CargoRocketEntity.FUEL_CAPACITY);
        rocket.cargo().setItem(0, new ItemStack(Items.DIAMOND, 5));
        final boolean[] launched = {false};
        final boolean[] landing = {false};
        helper.onEachTick(() -> {
            launched[0] |= rocket.launched();
            landing[0] |= rocket.phase() == RocketEntity.Phase.LANDING;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(launched[0] && landing[0], "flew and came down");
            helper.assertValueEqual(rocket.phase(), RocketEntity.Phase.UNIGNITED, "landed");
            final BlockPos b = helper.absolutePos(PAD_B);
            helper.assertTrue(Math.abs(rocket.getX() - b.getX() - 0.5D) < 0.01D && Math.abs(rocket.getZ() - b.getZ() - 0.5D) < 0.01D,
                    "on pad B: " + rocket.position());
            helper.assertTrue(rocket.cargo().getItem(0).is(Items.DIAMOND) && rocket.cargo().getItem(0).getCount() == 5, "cargo intact");
            helper.assertTrue(rocket.fuel() < CargoRocketEntity.FUEL_CAPACITY && rocket.fuel() > 0, "burned some fuel: " + rocket.fuel());
            helper.assertBlockPresent(GCBlocks.LANDING_PAD.get(), PAD_A);
        });
    }

    // --- NASA Workbench ---

    /** 1.12.2 cargo rocket recipes: every part and 1-3 chests (18 slots each); no chest, no rocket. */
    @GameTest(template = "empty")
    public static void cargoRocketWorkbenchPage(GameTestHelper helper) {
        final SimpleContainer matrix = new SimpleContainer(CargoRocketBenchMenu.MATRIX_SIZE);
        matrix.setItem(1, new ItemStack(GCItems.NOSE_CONE.get()));
        matrix.setItem(2, new ItemStack(GCItems.ADVANCED_WAFER.get()));
        for (int i = 3; i <= 8; i++) {
            matrix.setItem(i, new ItemStack(GCItems.HEAVY_DUTY_PLATE.get()));
        }
        for (int i : new int[]{9, 10, 12, 13}) {
            matrix.setItem(i, new ItemStack(GCItems.ROCKET_FINS.get()));
        }
        matrix.setItem(11, new ItemStack(GCItems.TIER_1_ROCKET_ENGINE.get()));
        helper.assertTrue(CargoRocketBenchMenu.craft(matrix).isEmpty(), "needs a chest");
        matrix.setItem(15, new ItemStack(Items.CHEST));
        matrix.setItem(16, new ItemStack(Items.CHEST));
        final ItemStack result = CargoRocketBenchMenu.craft(matrix);
        helper.assertTrue(result.is(GCItems.CARGO_ROCKET.get()) && RocketItem.storage(result) == 2, "36-slot cargo rocket");
        helper.succeed();
    }
}
