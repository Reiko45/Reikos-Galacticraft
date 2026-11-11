package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.SpinThrusterBlock;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.station.SpaceStations;
import io.github.reiko45.galacticraft.world.station.StationRecipe;
import io.github.reiko45.galacticraft.world.station.StationSpin;
import io.github.reiko45.galacticraft.world.station.StationStructure;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 6: space stations (the orbit dimension itself isn't loaded by the GameTest server, so these use the test world). */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationTests {
    private static final String ROOM = "station_room";
    /** Arrival block inside the 18 x 10 x 12 room: the hull spans x 2..15, y 1..7, z 2..8. */
    private static final BlockPos BASE = new BlockPos(12, 3, 5);

    private StationTests() {
    }

    /** 1.12.2 orbit: gravity 0.005, fall x0.4, rockets leave at y 750 burning half as much fuel, no meteors, no air. */
    @GameTest(template = "empty")
    public static void orbitPhysics(GameTestHelper helper) {
        final CelestialPhysics orbit = CelestialPhysics.of(GCDimensions.ORBIT);
        helper.assertTrue(orbit != null && Math.abs(0.08D - orbit.gravityReduction() - 0.005D) < 1e-9, "gravity 0.005");
        helper.assertTrue(orbit.fallDamageModifier() == 0.4F && orbit.exitHeight() == 750.0D && orbit.meteorFrequency() == 0.0D
                && !orbit.breathable(), "fall, exit height, meteors, air");
        helper.assertValueEqual((int) Math.floor(3.0D / orbit.fuelUsage()), 6, "a rocket burns 1 mB every 6 ticks");
        helper.assertValueEqual(CelestialPhysics.fuelInterval(helper.getLevel()), 3, "every 3 ticks on Earth");
        helper.succeed();
    }

    /** 1.12.2 WorldGenSpaceStation + ChunkProviderOrbit: the tin hull, its windows and the arrival block. */
    @GameTest(template = ROOM)
    public static void starterStationIsBuilt(GameTestHelper helper) {
        StationStructure.build(helper.getLevel(), helper.absolutePos(BASE));
        helper.assertBlockPresent(GCBlocks.SPACE_STATION_BASE.get(), BASE);
        final BlockPos corner = BASE.offset(StationStructure.HULL_OFFSET);
        helper.assertBlockPresent(GCBlocks.TIN_DECORATION_BLOCK_2.get(), corner);
        helper.assertBlockPresent(GCBlocks.TIN_DECORATION_BLOCK_2.get(), corner.offset(13, 6, 6));
        helper.assertBlockPresent(Blocks.GLASS_PANE, corner.offset(8, 3, 0));
        final BlockPos arrival = BASE.offset(StationStructure.ARRIVAL_OFFSET);
        helper.assertBlockPresent(Blocks.AIR, arrival);
        helper.assertBlockPresent(Blocks.AIR, arrival.above());
        helper.assertBlockPresent(GCBlocks.TIN_DECORATION_BLOCK_2.get(), arrival.below());
        helper.succeed();
    }

    /** 1.12.2 SpinManager: thrusters one way spin the station up; balanced or switched off by redstone, it stays still. */
    @GameTest(template = ROOM)
    public static void spinThrustersSpinTheStation(GameTestHelper helper) {
        final BlockPos base = helper.absolutePos(BASE);
        StationStructure.build(helper.getLevel(), base);
        StationSpin spin = StationSpin.measure(helper.getLevel(), base);
        helper.assertTrue(spin.thrusterCount() == 0 && spin.targetVelocity() == 0.0F, "no thrusters, no spin");
        // On the outside of the room's north wall (hull z 0), pointing away from it
        final BlockPos corner = BASE.offset(StationStructure.HULL_OFFSET);
        final BlockPos thruster = corner.offset(7, 3, -1);
        helper.setBlock(thruster, GCBlocks.SPIN_THRUSTER.get().defaultBlockState().setValue(SpinThrusterBlock.FACING, Direction.NORTH));
        spin = StationSpin.measure(helper.getLevel(), base);
        helper.assertValueEqual(spin.thrusterCount(), 1, "thruster found");
        helper.assertTrue(spin.targetVelocity() > 0.0F && spin.targetVelocity() <= 0.00125F, "spins up to at most 0.00125 rad/tick: " + spin.targetVelocity());
        for (int i = 0; i < 2000 && spin.spinStep(); i++) {
            // spin up
        }
        helper.assertTrue(spin.angularVelocity == spin.targetVelocity(), "reached its target spin");
        final BlockPos reversed = corner.offset(13, 3, -1);
        helper.setBlock(reversed, GCBlocks.SPIN_THRUSTER.get().defaultBlockState().setValue(SpinThrusterBlock.FACING, Direction.NORTH)
                .setValue(SpinThrusterBlock.REVERSED, true));
        spin = StationSpin.measure(helper.getLevel(), base);
        helper.assertTrue(spin.thrusterCount() == 2 && spin.targetVelocity() == 0.0F, "balanced thrusters don't spin it");
        helper.setBlock(thruster.west(), Blocks.REDSTONE_BLOCK);
        spin = StationSpin.measure(helper.getLevel(), base);
        helper.assertValueEqual(spin.thrusterCount(), 1, "a powered thruster is off");
        helper.assertTrue(spin.targetVelocity() < 0.0F, "the reversed one alone spins it the other way");
        helper.succeed();
    }

    /** FreefallHandler#testEntityFreefall: floating means nothing but air in and around the entity. */
    @GameTest(template = ROOM)
    public static void freefallNeedsNothingNearby(GameTestHelper helper) {
        final AABB box = new AABB(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.2D, 4.0D, 6.2D)),
                helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.8D, 5.8D, 6.8D)));
        helper.assertTrue(StationSpin.inFreefall(helper.getLevel(), box), "floating in empty space");
        helper.setBlock(new BlockPos(8, 3, 6), Blocks.STONE);
        helper.assertFalse(StationSpin.inFreefall(helper.getLevel(), box), "a block just below catches you");
        helper.succeed();
    }

    /** SpaceStationRecipe: 32 tin, 16 aluminum, an advanced wafer and 24 iron, all taken. */
    @GameTest(template = "empty")
    public static void stationCostsItsRequirements(GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(GCItems.TIN_INGOT.get(), 32));
        player.getInventory().add(new ItemStack(GCItems.ALUMINUM_INGOT.get(), 16));
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 30));
        helper.assertFalse(StationRecipe.matches(player), "no advanced wafer yet");
        player.getInventory().add(new ItemStack(GCItems.ADVANCED_WAFER.get(), 1));
        helper.assertTrue(StationRecipe.consume(player), "everything there");
        helper.assertValueEqual(player.getInventory().countItem(GCItems.TIN_INGOT.get()), 0, "tin taken");
        helper.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 6, "24 of 30 iron taken");
        helper.assertValueEqual(player.getInventory().countItem(GCItems.ADVANCED_WAFER.get()), 0, "wafer taken");
        helper.succeed();
    }

    /** SpaceStationWorldData: owners, invitations, regions and saving. */
    @GameTest(template = "empty")
    public static void stationDataAndAccess(GameTestHelper helper) {
        final SpaceStations stations = SpaceStations.get(helper.getLevel().getServer());
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final SpaceStations.Station station = stations.add(owner.getUUID(), owner.getGameProfile().getName());
        helper.assertValueEqual(station.name(), "Station: " + owner.getGameProfile().getName(), "1.12.2 default name");
        helper.assertTrue(station.canVisit(owner), "the owner may visit");
        final Player guest = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(station.canVisit(guest), "others may not");
        stations.allow(station, guest.getGameProfile().getName().toUpperCase());
        helper.assertTrue(station.canVisit(guest), "invited (names ignore case)");
        stations.disallow(station, guest.getGameProfile().getName());
        stations.setAllowAll(station, true);
        helper.assertTrue(station.canVisit(guest), "open to all");
        helper.assertTrue(stations.at(station.base().offset(1500, 0, -900)) == station, "its region");
        helper.assertTrue(stations.at(station.base().offset(SpaceStations.SPACING, 0, 0)) != station, "not the next region");
        stations.rename(station, "Orbital One");
        final SpaceStations copy = SpaceStations.load(stations.save(new CompoundTag(), helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        final SpaceStations.Station saved = copy.byId(station.id());
        helper.assertTrue(saved != null && saved.name().equals("Orbital One") && saved.allowAll() && saved.owner().equals(owner.getUUID()), "saved");
        helper.succeed();
    }

    /** GuiCelestialSelection: from a rocket a player without a station may build one; owners see theirs listed. */
    @GameTest(template = "empty")
    public static void selectionScreenOffersStations(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(SpaceTravel.selectionPayload(player).canCreateStation(), "only from a rocket");
        GCPlayerData.get(player).inSpace = true;
        helper.assertTrue(SpaceTravel.selectionPayload(player).canCreateStation(), "from a rocket, with no station yet");
        final SpaceStations.Station station = SpaceStations.get(helper.getLevel().getServer()).add(player.getUUID(), player.getGameProfile().getName());
        final GCNetwork.OpenCelestialSelection payload = SpaceTravel.selectionPayload(player);
        helper.assertFalse(payload.canCreateStation(), "one station per player");
        helper.assertTrue(payload.stations().stream().anyMatch(info -> info.id() == station.id() && info.owned()), "own station listed");
        SpaceTravel.renameStation(player, station.id(), "  Home Base  ");
        helper.assertValueEqual(station.name(), "Home Base", "renamed (trimmed)");
        GCPlayerData.get(player).inSpace = false;
        helper.succeed();
    }

    /** TileEntityCircuitFabricator in zero-g: the bonus only applies in orbit. */
    @GameTest(template = "empty")
    public static void fabricatorBonusOnlyInOrbit(GameTestHelper helper) {
        final ItemStack wafers = io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity.zeroGravityBonus(helper.getLevel(),
                new ItemStack(GCItems.BASIC_WAFER.get(), 3));
        helper.assertValueEqual(wafers.getCount(), 3, "no bonus on Earth");
        helper.succeed();
    }
}
