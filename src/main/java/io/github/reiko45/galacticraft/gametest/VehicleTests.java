package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.entity.BuggyEntity;
import io.github.reiko45.galacticraft.item.BuggyItem;
import io.github.reiko45.galacticraft.menu.BuggyBenchMenu;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 5b: schematics and NASA Workbench pages, the Moon Buggy and the Buggy Fueling Pad. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VehicleTests {
    private static final String AIR = "air_room";

    private VehicleTests() {
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void pad(GameTestHelper helper, BlockPos centre, Block block) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), block);
            }
        }
    }

    /** 1.12.2 SchematicRegistry: rocket and "add schematic" pages always; a schematic unlocks its page once, for good. */
    @GameTest(template = "empty")
    public static void schematicsUnlockWorkbenchPages(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertValueEqual(Schematics.unlocked(player), List.of(Schematics.ROCKET_T1, Schematics.ADD_SCHEMATIC), "default pages");
        helper.assertValueEqual(Schematics.neighbours(player, Schematics.ROCKET_T1), 2, "rocket page: next only");
        final ItemStack buggy = new ItemStack(GCItems.BUGGY_SCHEMATIC.get(), 2);
        helper.assertTrue(Schematics.unlock(player, buggy), "buggy schematic unlocks");
        helper.assertValueEqual(buggy.getCount(), 1, "one schematic used");
        helper.assertValueEqual(Schematics.unlocked(player), List.of(Schematics.ROCKET_T1, Schematics.MOON_BUGGY, Schematics.ADD_SCHEMATIC),
                "buggy page between rocket and add-schematic");
        helper.assertValueEqual(Schematics.neighbours(player, Schematics.MOON_BUGGY), 3, "buggy page: back and next");
        helper.assertFalse(Schematics.unlock(player, buggy), "already unlocked");
        helper.assertValueEqual(buggy.getCount(), 1, "nothing used the second time");
        final ItemStack t2 = new ItemStack(GCItems.TIER_2_ROCKET_SCHEMATIC.get());
        helper.assertTrue(Schematics.unlock(player, t2), "the Tier 2 Rocket schematic unlocks its page");
        helper.assertValueEqual(Schematics.unlocked(player),
                List.of(Schematics.ROCKET_T1, Schematics.MOON_BUGGY, Schematics.ROCKET_T2, Schematics.ADD_SCHEMATIC), "pages in 1.12.2 id order");
        final GCPlayerData copy = new GCPlayerData();
        copy.deserializeNBT(player.registryAccess(), GCPlayerData.get(player).serializeNBT(player.registryAccess()));
        helper.assertTrue(copy.hasSchematic(Schematics.MOON_BUGGY.id()), "unlocked pages are saved");
        helper.succeed();
    }

    /** 1.12.2 buggy recipes: 11 plates, seat in slot 6, 4 wheels; each storage box adds 18 slots. */
    @GameTest(template = "empty")
    public static void buggyBenchMakesBuggies(GameTestHelper helper) {
        final SimpleContainer matrix = new SimpleContainer(BuggyBenchMenu.MATRIX_SIZE);
        for (int i = 1; i <= 12; i++) {
            matrix.setItem(i, new ItemStack(i == BuggyBenchMenu.SEAT ? GCItems.BUGGY_SEAT.get() : GCItems.HEAVY_PLATING.get()));
        }
        for (int i = 13; i <= 16; i++) {
            matrix.setItem(i, new ItemStack(GCItems.BUGGY_WHEEL.get()));
        }
        ItemStack result = BuggyBenchMenu.craft(matrix);
        helper.assertTrue(result.is(GCItems.BUGGY.get()) && BuggyItem.storage(result) == 0, "plain buggy");
        matrix.setItem(17, new ItemStack(GCItems.BUGGY_STORAGE_BOX.get()));
        matrix.setItem(19, new ItemStack(GCItems.BUGGY_STORAGE_BOX.get()));
        helper.assertValueEqual(BuggyItem.storage(BuggyBenchMenu.craft(matrix)), 2, "two storage boxes");
        helper.assertFalse(BuggyBenchMenu.isPart(BuggyBenchMenu.SEAT, new ItemStack(GCItems.HEAVY_PLATING.get())), "the seat slot wants a seat");
        matrix.setItem(14, ItemStack.EMPTY);
        helper.assertTrue(BuggyBenchMenu.craft(matrix).isEmpty(), "three wheels make nothing");
        helper.succeed();
    }

    /** EntityBuggy: falls with the body's gravity, one hit breaks it into its item (fuel and storage kept, cargo dropped). */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void buggyLandsAndBreaksIntoItem(GameTestHelper helper) {
        floor(helper);
        final BuggyEntity buggy = BuggyEntity.create(helper.getLevel(), 0, 0, 0, 2, 300);
        buggy.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5D, 4.0D, 6.5D)));
        helper.getLevel().addFreshEntity(buggy);
        helper.assertValueEqual(buggy.cargo().getContainerSize(), 36, "two boxes, 36 slots");
        buggy.cargo().setItem(5, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        helper.succeedWhen(() -> helper.assertTrue(buggy.onGround(), "buggy landed"));
        helper.runAfterDelay(60, () -> {
            final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            buggy.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(buggy.isRemoved(), "one hit breaks it");
            final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16.0D));
            final ItemEntity item = drops.stream().filter(drop -> drop.getItem().is(GCItems.BUGGY.get())).findFirst().orElse(null);
            helper.assertTrue(item != null && BuggyItem.storage(item.getItem()) == 2 && BuggyItem.fuel(item.getItem()) == 300, "buggy item keeps storage and fuel");
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(net.minecraft.world.item.Items.DIAMOND)), "cargo dropped");
        });
    }

    /** Fueling pads join like launch pads but never with them; rockets can't use them; a Fuel Loader fills a parked buggy. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void fuelingPadRefuelsBuggy(GameTestHelper helper) {
        floor(helper);
        final BlockPos centre = new BlockPos(4, 1, 6);
        pad(helper, centre, GCBlocks.BUGGY_FUELING_PAD.get());
        helper.assertBlockProperty(centre, LandingPadBlock.PAD, LandingPadBlock.Pad.CENTRE);
        helper.setBlock(centre.offset(2, 0, 0), GCBlocks.LANDING_PAD.get());
        helper.assertBlockProperty(centre.offset(2, 0, 0), LandingPadBlock.PAD, LandingPadBlock.Pad.SINGLE);
        helper.assertTrue(LandingPadBlock.findCentre(helper.getLevel(), helper.absolutePos(centre.offset(1, 0, 1))) != null, "fueling pad joined");
        helper.assertTrue(LandingPadBlock.findLaunchPadCentre(helper.getLevel(), helper.absolutePos(centre)) == null, "not a launch pad");

        final BlockPos loaderPos = centre.offset(-2, 0, 0);
        helper.setBlock(loaderPos, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final FuelLoaderBlockEntity loader = helper.getBlockEntity(loaderPos);
        loader.energy().setStored(16_000.0F);
        loader.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 5_000), IFluidHandler.FluidAction.EXECUTE);
        final BuggyEntity buggy = BuggyEntity.create(helper.getLevel(), 0, 0, 0, 0, 0);
        buggy.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(centre.getX() + 0.5D, centre.getY() + 0.2D, centre.getZ() + 0.5D)));
        helper.getLevel().addFreshEntity(buggy);
        helper.succeedWhen(() -> {
            helper.assertTrue(buggy.fuel() >= 40, "buggy fuel " + buggy.fuel());
            helper.assertValueEqual(buggy.fuel() + loader.fuelTank().getFluidAmount(), 5_000, "fuel conserved");
        });
    }

    /** ItemBuggy: sets a buggy down on the block looked at; the item keeps the storage and fuel. */
    @GameTest(template = AIR)
    public static void buggyItemPlacesBuggy(GameTestHelper helper) {
        floor(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5D, 1.0D, 3.5D)), 0.0F, 45.0F);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, BuggyItem.create(3, 120));
        player.getMainHandItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        final List<BuggyEntity> buggies = helper.getEntities(GCEntities.BUGGY.get());
        helper.assertValueEqual(buggies.size(), 1, "one buggy");
        helper.assertTrue(buggies.get(0).storage() == 3 && buggies.get(0).fuel() == 120, "storage and fuel carried over");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "item used up");
        helper.succeed();
    }

    /**
     * 1.12.2 EntityLander#pressKey: Jump brakes the falling lander to 1 block a tick (0.15 below y 90); let go, gravity
     * (0.008 a tick each tick) speeds it up again; Sneak dives, to at least 1 block a tick.
     */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void landerBrakesAndSpeedsUpAgain(GameTestHelper helper) {
        final net.minecraft.world.phys.Vec3 above = helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5D, 0.0D, 6.5D));
        final io.github.reiko45.galacticraft.entity.LanderEntity lander = io.github.reiko45.galacticraft.entity.LanderEntity.create(helper.getLevel(),
                above.x, 280.0D, above.z, net.minecraft.core.NonNullList.create(), 0);
        helper.getLevel().addFreshEntity(lander);
        final net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(above.x, 282.0D, above.z);
        player.startRiding(lander, true);
        lander.setKeys(io.github.reiko45.galacticraft.entity.LanderEntity.KEY_SLOW);
        final double[] released = {Double.NaN};
        final int[] at = {0};
        helper.succeedWhen(() -> {
            final double speed = lander.getDeltaMovement().y;
            if (Double.isNaN(released[0])) {
                // braking: from the 2.5 blocks a tick plunge down to the 1 block a tick it holds above y 90
                helper.assertTrue(lander.getY() < 270.0D && Math.abs(speed + 1.0D) < 1.0E-6, "braked to 1 block a tick: " + speed);
                lander.setKeys(0);
                released[0] = speed;
                at[0] = lander.tickCount;
                throw new net.minecraft.gametest.framework.GameTestAssertException("released");
            }
            final int ticks = lander.tickCount - at[0];
            if (ticks < 20) {
                throw new net.minecraft.gametest.framework.GameTestAssertException("falling freely");
            }
            helper.assertTrue(speed < released[0] - 0.008D * (ticks - 2), "speeds up again once let go: " + released[0] + " then " + speed);
            lander.setKeys(io.github.reiko45.galacticraft.entity.LanderEntity.KEY_FAST);
            lander.tick();
            helper.assertTrue(lander.getDeltaMovement().y <= -1.0D && lander.getDeltaMovement().y < speed,
                    "Sneak dives: " + lander.getDeltaMovement().y);
            player.stopRiding();
            lander.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }
}
