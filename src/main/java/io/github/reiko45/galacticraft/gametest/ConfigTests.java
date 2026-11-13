package io.github.reiko45.galacticraft.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.machine.CompressorGrid;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import io.github.reiko45.galacticraft.world.ConfigChecks;
import io.github.reiko45.galacticraft.world.GCDimensions;
import io.github.reiko45.galacticraft.world.station.SpaceStations;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 9 part 3: the 1.12.2 settings. The first tests read the defaults; the "changed_settings" batch runs on its own
 * with the rocket, travel, station, block and hard mode settings changed beforehand and put back afterwards (no other
 * test runs while they are changed). Settings read at start-up (quick mode, ores) are checked through the pieces that
 * read them.
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigTests {
    private static final String AIR = "air_room";
    private static final String CHANGED = "changed_settings";
    private static final ResourceKey<Level> OTHER_WORLD = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("othermod", "world"));

    private ConfigTests() {
    }

    // ------------------------------------------------------------------ defaults

    /**
     * Quick mode is off: the wire and battery recipes make 6 and 2 (their quick mode twins, 9 and 3, are not loaded), and
     * compressors make one plate; with it on they would make two "compressed" items, and only those.
     */
    @GameTest(template = AIR)
    public static void quickModeRecipesAndCompressors(GameTestHelper helper) {
        final var recipes = helper.getLevel().getRecipeManager();
        final var access = helper.getLevel().registryAccess();
        helper.assertValueEqual(recipes.byKey(GalacticraftCore.id("aluminum_wire")).map(RecipeHolder::value)
                .map(r -> r.getResultItem(access).getCount()).orElse(-1), 6, "aluminum wire");
        helper.assertValueEqual(recipes.byKey(GalacticraftCore.id("battery")).map(RecipeHolder::value)
                .map(r -> r.getResultItem(access).getCount()).orElse(-1), 2, "battery");
        helper.assertTrue(recipes.byKey(GalacticraftCore.id("aluminum_wire_quick_mode")).isEmpty(), "no quick wire recipe");
        helper.assertTrue(recipes.byKey(GalacticraftCore.id("battery_quick_mode")).isEmpty(), "no quick battery recipe");
        helper.assertTrue(new ConfigChecks.RecipeCondition("quickMode", false).test(null), "the normal recipes' condition");
        helper.assertFalse(new ConfigChecks.RecipeCondition("quickMode", true).test(null), "the quick recipes' condition");
        final JsonObject unknown = new JsonObject();
        unknown.addProperty("option", "noSuchSetting");
        helper.assertTrue(ConfigChecks.RecipeCondition.CODEC.codec().parse(JsonOps.INSTANCE, unknown).isError(), "an unknown setting is refused");
        final ItemStack plate = new ItemStack(GCItems.COMPRESSED_COPPER.get());
        helper.assertValueEqual(CompressorGrid.quickMode(plate, false).getCount(), 1, "one plate");
        helper.assertValueEqual(CompressorGrid.quickMode(plate, true).getCount(), 2, "quick mode: two plates");
        helper.assertValueEqual(plate.getCount(), 1, "the recipe's own stack is left alone");
        helper.assertValueEqual(CompressorGrid.quickMode(new ItemStack(GCItems.HEAVY_PLATING.get()), true).getCount(), 1,
                "heavy plating isn't \"compressed\"");
        helper.assertFalse(new ConfigChecks.LootCondition("quickMode", true).test(null), "no extra raw silicon");
        helper.succeed();
    }

    /** Every Galacticraft ore checks its on/off setting before placing; all are on by default. */
    @GameTest(template = AIR)
    public static void oreSettingsFilterPlacement(GameTestHelper helper) {
        final var placed = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        final String[] ores = {"ore_tin", "ore_aluminum", "ore_silicon", "moon_ore_copper", "moon_ore_tin", "moon_ore_cheese",
                "moon_ore_sapphire", "mars_ore_desh", "mars_ore_copper", "mars_ore_tin", "mars_ore_iron", "venus_ore_aluminum",
                "venus_ore_copper", "venus_ore_galena", "venus_ore_quartz", "venus_ore_silicon", "venus_ore_tin", "venus_ore_solar"};
        for (String ore : ores) {
            final PlacedFeature feature = placed.get(GalacticraftCore.id(ore));
            helper.assertTrue(feature != null && feature.placement().get(0) instanceof ConfigChecks.Filter, ore + " checks its setting");
        }
        helper.assertTrue(ConfigChecks.holds("enableTinOreGen", true), "overworld tin on");
        helper.assertTrue(ConfigChecks.holds("disableCheeseMoon", false), "moon cheese on");
        helper.assertTrue(ConfigChecks.holds("disableSolarVenus", false), "venus solar on");
        helper.assertFalse(ConfigChecks.holds("noSuchSetting", false), "an unknown setting never holds");
        helper.assertFalse(GCConfig.common(GCConfig.DISABLE_MOON_VILLAGE_GEN), "moon villages on");
        helper.succeed();
    }

    /**
     * By default rockets launch from anywhere but the Nether and the End, reach the Overworld, hold their tier's fuel and
     * land where they took off; stations need an invitation; machines use their normal power.
     */
    @GameTest(template = AIR)
    public static void defaultRulesOfPlay(GameTestHelper helper) {
        helper.assertTrue(GCConfig.rocketsLaunchFrom(Level.OVERWORLD), "the Overworld");
        helper.assertTrue(GCConfig.rocketsLaunchFrom(GCDimensions.MOON), "the Moon");
        helper.assertTrue(GCConfig.rocketsLaunchFrom(OTHER_WORLD), "another mod's world");
        helper.assertFalse(GCConfig.rocketsLaunchFrom(Level.NETHER), "not the Nether");
        helper.assertFalse(GCConfig.rocketsLaunchFrom(Level.END), "not the End");
        helper.assertTrue(SpaceTravel.reachable(Level.OVERWORLD), "the Overworld can be reached");
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), 0, 0, 0, 0, 0);
        helper.assertValueEqual(rocket.tankCapacity(), Tier1RocketEntity.FUEL_CAPACITY, "fuel factor 1");
        final double[] spot = SpaceTravel.landingSpot(helper.getLevel(), 5000.0D, -2500.0D);
        helper.assertTrue(spot[0] == 5000.0D && spot[1] == -2500.0D, "no landing border");
        final SpaceStations.Station station = SpaceStations.get(helper.getLevel().getServer()).add(java.util.UUID.randomUUID(), "someone-else");
        helper.assertFalse(station.canVisit(helper.makeMockServerPlayerInLevel()), "a station needs an invitation");
        helper.assertFalse(GCConfig.hardMode(), "hard mode off");
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.CIRCUIT_FABRICATOR.get());
        final CircuitFabricatorBlockEntity fabricator = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertValueEqual(fabricator.energyUsePerTick(), 20.0F, "20 gJ a tick");
        helper.assertTrue(GCBlocks.GRATING.get().canPlaceLiquid(null, helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                GCBlocks.GRATING.get().defaultBlockState(), Fluids.WATER), "gratings take water");
        helper.succeed();
    }

    /** With edge checks on, breathable air that no sealer feeds clears as soon as a block beside it changes. */
    @GameTest(template = AIR, timeoutTicks = 40)
    public static void edgeCheckClearsStrayAir(GameTestHelper helper) {
        final BlockPos air = strayAir(helper);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(GCBlocks.BREATHABLE_AIR.get(), air));
    }

    /** A cell of breathable air walled in with stone, then one wall changed to glass (its neighbour update). */
    private static BlockPos strayAir(GameTestHelper helper) {
        final BlockPos centre = new BlockPos(3, 3, 3);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            helper.setBlock(pos, Blocks.STONE);
        }
        helper.setBlock(centre, GCBlocks.BREATHABLE_AIR.get());
        helper.setBlock(centre.east(), Blocks.GLASS);
        return centre;
    }

    // ------------------------------------------------------------------ changed settings

    @BeforeBatch(batch = CHANGED)
    public static void changeSettings(ServerLevel level) {
        GCConfig.ROCKET_FUEL_FACTOR.set(3);
        GCConfig.DISABLE_ROCKET_LAUNCH_DIMENSIONS.set(List.of("*"));
        GCConfig.DISABLE_SPACESHIP_GRIEF.set(true);
        GCConfig.DISABLE_ROCKETS_TO_OVERWORLD.set(true);
        GCConfig.OTHER_PLANET_WORLD_BORDERS.set(1000);
        GCConfig.SPACE_STATIONS_REQUIRE_PERMISSION.set(false);
        GCConfig.ALLOW_LIQUID_GRATINGS.set(false);
        GCConfig.ENABLE_SEALER_EDGE_CHECKS.set(false);
        GCConfig.HARD_MODE.set(true);
    }

    @AfterBatch(batch = CHANGED)
    public static void restoreSettings(ServerLevel level) {
        GCConfig.ROCKET_FUEL_FACTOR.set(GCConfig.ROCKET_FUEL_FACTOR.getDefault());
        GCConfig.DISABLE_ROCKET_LAUNCH_DIMENSIONS.set(GCConfig.DISABLE_ROCKET_LAUNCH_DIMENSIONS.getDefault());
        GCConfig.DISABLE_SPACESHIP_GRIEF.set(GCConfig.DISABLE_SPACESHIP_GRIEF.getDefault());
        GCConfig.DISABLE_ROCKETS_TO_OVERWORLD.set(GCConfig.DISABLE_ROCKETS_TO_OVERWORLD.getDefault());
        GCConfig.OTHER_PLANET_WORLD_BORDERS.set(GCConfig.OTHER_PLANET_WORLD_BORDERS.getDefault());
        GCConfig.SPACE_STATIONS_REQUIRE_PERMISSION.set(GCConfig.SPACE_STATIONS_REQUIRE_PERMISSION.getDefault());
        GCConfig.ALLOW_LIQUID_GRATINGS.set(GCConfig.ALLOW_LIQUID_GRATINGS.getDefault());
        GCConfig.ENABLE_SEALER_EDGE_CHECKS.set(GCConfig.ENABLE_SEALER_EDGE_CHECKS.getDefault());
        GCConfig.HARD_MODE.set(GCConfig.HARD_MODE.getDefault());
    }

    /**
     * rocketFuelFactor 3: the Tier 1's tank holds 3,000 mB and each burn takes 3; the creative pre-fuelled rocket still
     * comes full.
     */
    @GameTest(template = AIR, batch = CHANGED)
    public static void fuelFactorTriplesTheTank(GameTestHelper helper) {
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), 0, 0, 0, 1, 0);
        helper.assertValueEqual(rocket.tankCapacity(), 3 * Tier1RocketEntity.FUEL_CAPACITY, "three times the tank");
        helper.assertValueEqual(rocket.addFuel(5000, true), 3000, "fills to 3,000");
        helper.assertValueEqual(RocketEntity.fuelFactor(), 3, "three mB a burn");
        final Tier1RocketEntity creative = Tier1RocketEntity.create(helper.getLevel(), 0, 0, 0, 0, Tier1RocketEntity.FUEL_CAPACITY);
        helper.assertValueEqual(creative.fuel(), 3000, "the pre-fuelled rocket is full");
        helper.succeed();
    }

    /** "*" stops launches from every other mod's world, never from the Overworld or Galacticraft's. */
    @GameTest(template = AIR, batch = CHANGED)
    public static void launchBlockedEverywhereElse(GameTestHelper helper) {
        helper.assertFalse(GCConfig.rocketsLaunchFrom(OTHER_WORLD), "not another mod's world");
        helper.assertTrue(GCConfig.rocketsLaunchFrom(Level.OVERWORLD), "the Overworld");
        helper.assertTrue(GCConfig.rocketsLaunchFrom(GCDimensions.MARS), "Mars");
        helper.assertFalse(SpaceTravel.reachable(Level.OVERWORLD), "no way back to the Overworld");
        helper.assertTrue(SpaceTravel.reachable(GCDimensions.MOON), "the Moon still");
        // a world border of 1,000: from (5000, -2500) the far x comes in to 998 and z keeps the direction
        final double[] spot = SpaceTravel.landingSpot(helper.getLevel(), 5000.0D, -2500.0D);
        helper.assertTrue(Math.abs(spot[0] - 998.0D) < 1.0E-6D && Math.abs(spot[1] + 499.0D) < 1.0E-6D, "landing at " + spot[0] + ", " + spot[1]);
        helper.succeed();
    }

    /** disableSpaceshipGrief: a crashing rocket still hurts its riders but leaves the ground alone. */
    @GameTest(template = AIR, batch = CHANGED)
    public static void crashWithoutGrief(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        final Vec3 at = helper.absoluteVec(new Vec3(3.5D, 2.0D, 3.5D));
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), at.x, at.y, at.z, 0, 0);
        helper.getLevel().addFreshEntity(rocket);
        rocket.crash();
        helper.assertFalse(rocket.isAlive(), "the rocket is gone");
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
            }
        }
        helper.succeed();
    }

    /** Stations open to all without permission; gratings keep water out; hard mode doubles the fabricator's power. */
    @GameTest(template = AIR, batch = CHANGED)
    public static void stationsGratingsAndHardMode(GameTestHelper helper) {
        final ServerPlayer guest = helper.makeMockServerPlayerInLevel();
        final SpaceStations.Station station = SpaceStations.get(helper.getLevel().getServer()).add(java.util.UUID.randomUUID(), "someone-else");
        helper.assertTrue(station.canVisit(guest), "anyone may visit");
        helper.assertFalse(GCBlocks.GRATING.get().canPlaceLiquid(null, helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                GCBlocks.GRATING.get().defaultBlockState(), Fluids.WATER), "no water in gratings");
        helper.assertTrue(GCConfig.hardMode(), "hard mode on");
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.CIRCUIT_FABRICATOR.get());
        final CircuitFabricatorBlockEntity fabricator = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.assertValueEqual(fabricator.energyUsePerTick(), 40.0F, "40 gJ a tick");
        helper.succeed();
    }

    /** enableSealerEdgeChecks off: stray breathable air stays until a sealer's own check finds it. */
    @GameTest(template = AIR, batch = CHANGED, timeoutTicks = 40)
    public static void noEdgeChecks(GameTestHelper helper) {
        final BlockPos air = strayAir(helper);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(helper.getBlockState(air).getBlock() instanceof BreathableAirBlock, "still breathable");
            helper.succeed();
        });
    }
}
