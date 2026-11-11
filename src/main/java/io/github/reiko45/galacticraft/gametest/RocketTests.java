package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.NasaWorkbenchBlock;
import io.github.reiko45.galacticraft.block.ParachestBlock;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ParachestBlockEntity;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 4: launch pads, the NASA Workbench, the Fuel Loader, rockets, travel and the Para-chest. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketTests {
    private static final String AIR = "air_room";
    private static final BlockPos CENTRE = new BlockPos(6, 1, 6);

    private RocketTests() {
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

    private static Tier1RocketEntity rocketOnPad(GameTestHelper helper, int storage, int fuel) {
        final BlockPos abs = helper.absolutePos(CENTRE);
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 0.4D, abs.getZ() + 0.5D,
                storage, fuel);
        helper.getLevel().addFreshEntity(rocket);
        return rocket;
    }

    private static List<Tier1RocketEntity> rockets(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Tier1RocketEntity.class, new AABB(helper.absolutePos(CENTRE)).inflate(3.0D, 6.0D, 3.0D));
    }

    /** Nine pads in a square join into one launch pad; removing any of them splits it again. */
    @GameTest(template = AIR)
    public static void launchPadJoinsAndSplits(GameTestHelper helper) {
        pad(helper, CENTRE);
        helper.assertBlockProperty(CENTRE, LandingPadBlock.PAD, LandingPadBlock.Pad.CENTRE);
        helper.assertBlockProperty(CENTRE.offset(1, 0, 1), LandingPadBlock.PAD, LandingPadBlock.Pad.PART);
        helper.assertValueEqual(LandingPadBlock.findCentre(helper.getLevel(), helper.absolutePos(CENTRE.offset(-1, 0, 1))),
                helper.absolutePos(CENTRE), "a part finds its centre");
        helper.setBlock(CENTRE.offset(1, 0, 0), Blocks.AIR);
        helper.assertBlockProperty(CENTRE, LandingPadBlock.PAD, LandingPadBlock.Pad.SINGLE);
        helper.assertBlockProperty(CENTRE.offset(-1, 0, -1), LandingPadBlock.PAD, LandingPadBlock.Pad.SINGLE);
        helper.succeed();
    }

    /** Using the rocket item on a joined pad stands a rocket with the item's storage on the centre. */
    @GameTest(template = AIR)
    public static void rocketItemPlacesRocketOnPad(GameTestHelper helper) {
        pad(helper, CENTRE);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, Tier1RocketItem.create(2, 0));
        final BlockPos target = helper.absolutePos(CENTRE.offset(1, 0, 0));
        final UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(context);
        final List<Tier1RocketEntity> rockets = rockets(helper);
        helper.assertValueEqual(rockets.size(), 1, "one rocket");
        helper.assertValueEqual(rockets.get(0).storage(), 2, "storage from the item");
        helper.assertValueEqual(rockets.get(0).cargo().getContainerSize(), 38, "36 cargo slots + 2");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "item used up");
        helper.assertValueEqual(rockets.get(0).blockPosition(), helper.absolutePos(CENTRE), "on the centre");
        helper.succeed();
    }

    /** A powered Fuel Loader next to the pad pumps 2 mB per tick into the rocket. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void fuelLoaderFillsRocket(GameTestHelper helper) {
        pad(helper, CENTRE);
        final BlockPos loaderPos = CENTRE.offset(2, 0, 0);
        helper.setBlock(loaderPos, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final FuelLoaderBlockEntity loader = helper.getBlockEntity(loaderPos);
        loader.energy().setStored(16_000.0F);
        loader.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 5_000), IFluidHandler.FluidAction.EXECUTE);
        final Tier1RocketEntity rocket = rocketOnPad(helper, 0, 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(rocket.fuel() >= 40, "rocket fuel " + rocket.fuel());
            helper.assertValueEqual(rocket.fuel() + loader.fuelTank().getFluidAmount(), 5_000, "fuel conserved");
            helper.assertTrue(loader.energy().getStoredGJ() < 16_000.0F, "loading uses power");
        });
    }

    /** 1.12.2 S_IGNITE_ROCKET: no fuel, no launch; without a parachute the first press only warns. */
    @GameTest(template = AIR)
    public static void ignitionNeedsFuelAndConfirmation(GameTestHelper helper) {
        pad(helper, CENTRE);
        final Tier1RocketEntity rocket = rocketOnPad(helper, 0, 0);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.startRiding(rocket, true);
        SpaceTravel.ignite(player);
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.UNIGNITED, "no fuel");
        rocket.setFuel(500);
        SpaceTravel.ignite(player);
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.UNIGNITED, "first press without a parachute warns");
        SpaceTravel.ignite(player);
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.IGNITED, "second press ignites");
        helper.assertValueEqual(rocket.countdown(), Tier1RocketEntity.PRE_LAUNCH_WAIT, "400 tick countdown");
        helper.assertTrue(rocket.keepsRider(), "can't get out once ignited");
        player.stopRiding();
        helper.assertTrue(player.getVehicle() == rocket, "dismount refused");
        rocket.discard();
        helper.succeed();
    }

    /** After the countdown the rocket lifts off, taking the launch pad with it, and burns fuel. */
    @GameTest(template = AIR, timeoutTicks = 600)
    public static void rocketLaunchesAfterCountdown(GameTestHelper helper) {
        floor(helper);
        pad(helper, CENTRE);
        final Tier1RocketEntity rocket = rocketOnPad(helper, 0, Tier1RocketEntity.FUEL_CAPACITY);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        GCPlayerData.get(player).setGear(GearSlot.PARACHUTE, new ItemStack(GCItems.PARACHUTES.get(0).get()));
        player.startRiding(rocket, true);
        SpaceTravel.ignite(player);
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.IGNITED, "a parachute means no warning");
        final double startY = rocket.getY();
        helper.succeedWhen(() -> {
            helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.LAUNCHED, "launched");
            helper.assertBlockNotPresent(GCBlocks.LANDING_PAD.get(), CENTRE);
            helper.assertBlockNotPresent(GCBlocks.LANDING_PAD.get(), CENTRE.offset(1, 0, -1));
            helper.assertValueEqual(GCPlayerData.get(player).launchpadCount, 9, "pads go with the rider");
            helper.assertTrue(rocket.getY() > startY + 1.0D, "climbing: " + (rocket.getY() - startY));
            helper.assertTrue(rocket.fuel() < Tier1RocketEntity.FUEL_CAPACITY, "burning fuel");
            rocket.discard();
        });
    }

    /** The Tier 1 page: nose cone, 8 plates, 4 fins and an engine; each wooden chest adds 18 slots. */
    @GameTest(template = "empty")
    public static void workbenchMakesRockets(GameTestHelper helper) {
        final SimpleContainer matrix = new SimpleContainer(NasaWorkbenchMenu.MATRIX_SIZE);
        matrix.setItem(1, new ItemStack(GCItems.NOSE_CONE.get()));
        for (int i = 2; i < 10; i++) {
            matrix.setItem(i, new ItemStack(GCItems.HEAVY_PLATING.get()));
        }
        for (int i : new int[]{10, 11, 13, 14}) {
            matrix.setItem(i, new ItemStack(GCItems.ROCKET_FINS.get()));
        }
        matrix.setItem(12, new ItemStack(GCItems.TIER_1_ROCKET_ENGINE.get()));
        ItemStack result = NasaWorkbenchMenu.craft(matrix);
        helper.assertTrue(result.is(GCItems.TIER_1_ROCKET.get()), "rocket");
        helper.assertValueEqual(Tier1RocketItem.storage(result), 0, "no chests");
        matrix.setItem(15, new ItemStack(Items.CHEST));
        matrix.setItem(17, new ItemStack(Items.CHEST));
        helper.assertValueEqual(Tier1RocketItem.storage(NasaWorkbenchMenu.craft(matrix)), 2, "two chests");
        helper.assertFalse(NasaWorkbenchMenu.isPart(12, new ItemStack(GCItems.ROCKET_FINS.get())), "fins don't go in the engine slot");
        matrix.setItem(12, ItemStack.EMPTY);
        helper.assertTrue(NasaWorkbenchMenu.craft(matrix).isEmpty(), "no engine, no rocket");
        helper.succeed();
    }

    /** The workbench fills its 3x3x4 space with parts; breaking one breaks it all. */
    @GameTest(template = AIR)
    public static void workbenchMultiblock(GameTestHelper helper) {
        final BlockPos base = new BlockPos(6, 1, 6);
        helper.setBlock(base, GCBlocks.NASA_WORKBENCH.get());
        final BlockPos abs = helper.absolutePos(base);
        GCBlocks.NASA_WORKBENCH.get().setPlacedBy(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null,
                new ItemStack(GCBlocks.NASA_WORKBENCH.get()));
        helper.assertValueEqual(NasaWorkbenchBlock.partPositions(abs).size(), 11, "11 parts");
        helper.assertBlockPresent(GCBlocks.NASA_WORKBENCH_PART.get(), base.above(3));
        helper.assertBlockPresent(GCBlocks.NASA_WORKBENCH_PART.get(), base.offset(1, 2, 0));
        helper.assertBlockNotPresent(GCBlocks.NASA_WORKBENCH_PART.get(), base.offset(1, 1, 1));
        helper.getLevel().destroyBlock(helper.absolutePos(base.offset(0, 2, -1)), false);
        helper.assertBlockNotPresent(GCBlocks.NASA_WORKBENCH.get(), base);
        helper.assertBlockNotPresent(GCBlocks.NASA_WORKBENCH_PART.get(), base.above(3));
        helper.assertBlockNotPresent(GCBlocks.NASA_WORKBENCH_PART.get(), base.offset(-1, 1, 0));
        helper.succeed();
    }

    /** A falling Para-chest becomes a Para-chest block holding the cargo, the rocket and the leftover fuel. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void parachestLandsAsChest(GameTestHelper helper) {
        floor(helper);
        final NonNullList<ItemStack> cargo = NonNullList.withSize(20, ItemStack.EMPTY);
        cargo.set(0, new ItemStack(Items.DIAMOND, 3));
        cargo.set(19, Tier1RocketItem.create(1, 0));
        final BlockPos abs = helper.absolutePos(new BlockPos(6, 6, 6));
        final ParachestEntity entity = ParachestEntity.create(helper.getLevel(), abs.getX() + 0.5D, abs.getY(), abs.getZ() + 0.5D, cargo, 300, 13);
        helper.getLevel().addFreshEntity(entity);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(GCBlocks.PARACHEST.get(), new BlockPos(6, 1, 6));
            helper.assertBlockProperty(new BlockPos(6, 1, 6), ParachestBlock.COLOR, 13);
            final ParachestBlockEntity chest = helper.getBlockEntity(new BlockPos(6, 1, 6));
            helper.assertValueEqual(chest.cargo().getContainerSize(), 21, "18 + 2 + canister slot");
            helper.assertValueEqual(chest.cargo().getItem(0).getCount(), 3, "cargo kept");
            helper.assertTrue(chest.cargo().getItem(19).is(GCItems.TIER_1_ROCKET.get()), "rocket kept");
            helper.assertValueEqual(chest.fuelTank().getFluidAmount(), 300, "fuel kept");
            helper.assertFalse(entity.isAlive(), "entity gone");
        });
    }

    /** Back to Earth: arrive at y 250, and 20 ticks later a Para-chest with the rocket starts falling from y 230. */
    @GameTest(template = "empty")
    public static void returnToOverworld(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final GCPlayerData data = GCPlayerData.get(player);
        data.inSpace = true;
        data.spacePos = player.position();
        data.rocketCargo = NonNullList.withSize(20, ItemStack.EMPTY);
        data.rocketCargo.set(0, new ItemStack(Items.DIAMOND));
        data.rocketStorage = 1;
        data.rocketFuel = 120;
        data.launchpadCount = 9;
        data.launchedFromX = player.getX();
        data.launchedFromZ = player.getZ();
        SpaceTravel.choose(player, Level.OVERWORLD);
        helper.assertFalse(data.inSpace, "left space");
        helper.assertTrue(Math.abs(player.getY() - 250.0D) < 0.01D, "arrived at y 250: " + player.getY());
        helper.assertValueEqual(data.chestSpawnCooldown, 200, "chest on its way");
        helper.assertValueEqual(data.rocketCargo.get(18).getCount(), 9, "launch pads in the cargo");
        helper.assertValueEqual(Tier1RocketItem.storage(data.rocketCargo.get(19)), 1, "rocket in the cargo");
        for (int i = 0; i < 20; i++) {
            SpaceTravel.tick(player, data);
        }
        final List<ParachestEntity> chests = helper.getLevel().getEntitiesOfClass(ParachestEntity.class,
                new AABB(player.getX() - 6, 225, player.getZ() - 6, player.getX() + 6, 235, player.getZ() + 6));
        helper.assertValueEqual(chests.size(), 1, "one para-chest");
        chests.forEach(ParachestEntity::discard);
        helper.succeed();
    }
}
