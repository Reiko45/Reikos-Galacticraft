package io.github.reiko45.galacticraft;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Galacticraft's settings, from 1.12.2 ConfigManagerCore, ConfigManagerMars, ConfigManagerAsteroids, ConfigManagerVenus
 * and EnergyConfigHandler, in three files:
 * <ul>
 *     <li>client ({@code galacticraftcore-client.toml}): what this player sees</li>
 *     <li>common ({@code galacticraftcore-common.toml}): read at start-up, for what recipes and world generation need
 *     to know (quick mode, oil, ore and village generation)</li>
 *     <li>server ({@code serverconfig/galacticraftcore-server.toml}, per world, sent to players): the rules of play,
 *     hard mode included (players' own games need it for drifting in zero gravity)</li>
 * </ul>
 * 1.12.2's ids (dimensions, biomes, schematics, achievements), static dimension loading, key overrides, old fluid
 * names, other mods' world generation and the update check have no counterpart in 1.21.
 */
public final class GCConfig {
    public enum EnergyUnit {
        GJ,
        FE
    }

    // ------------------------------------------------------------------ client
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.EnumValue<EnergyUnit> ENERGY_DISPLAY_UNIT;
    public static final ModConfigSpec.BooleanValue MORE_STARS;
    public static final ModConfigSpec.BooleanValue DISABLE_SPACESHIP_PARTICLES;
    public static final ModConfigSpec.BooleanValue DISABLE_VEHICLE_CAMERA_CHANGES;
    public static final ModConfigSpec.BooleanValue OXYGEN_INDICATOR_LEFT;
    public static final ModConfigSpec.BooleanValue OXYGEN_INDICATOR_BOTTOM;

    // ------------------------------------------------------------------ common
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec.BooleanValue QUICK_MODE;
    public static final ModConfigSpec.BooleanValue HARD_MODE;
    public static final ModConfigSpec.DoubleValue OIL_GEN_FACTOR;
    public static final ModConfigSpec.BooleanValue ENABLE_TIN_ORE_GEN;
    public static final ModConfigSpec.BooleanValue ENABLE_ALUMINUM_ORE_GEN;
    public static final ModConfigSpec.BooleanValue ENABLE_SILICON_ORE_GEN;
    public static final ModConfigSpec.BooleanValue DISABLE_CHEESE_MOON;
    public static final ModConfigSpec.BooleanValue DISABLE_TIN_MOON;
    public static final ModConfigSpec.BooleanValue DISABLE_COPPER_MOON;
    public static final ModConfigSpec.BooleanValue DISABLE_SAPPHIRE_MOON;
    public static final ModConfigSpec.BooleanValue DISABLE_MOON_VILLAGE_GEN;
    public static final ModConfigSpec.BooleanValue DISABLE_DESH_MARS;
    public static final ModConfigSpec.BooleanValue DISABLE_TIN_MARS;
    public static final ModConfigSpec.BooleanValue DISABLE_COPPER_MARS;
    public static final ModConfigSpec.BooleanValue DISABLE_IRON_MARS;
    public static final ModConfigSpec.BooleanValue DISABLE_IRON_ASTEROIDS;
    public static final ModConfigSpec.BooleanValue DISABLE_ALUMINUM_ASTEROIDS;
    public static final ModConfigSpec.BooleanValue DISABLE_ILMENITE_ASTEROIDS;
    public static final ModConfigSpec.BooleanValue DISABLE_ALUMINUM_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_COPPER_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_GALENA_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_QUARTZ_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_SILICON_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_TIN_VENUS;
    public static final ModConfigSpec.BooleanValue DISABLE_SOLAR_VENUS;

    // ------------------------------------------------------------------ server
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue METEOR_SPAWN_MOD;
    public static final ModConfigSpec.BooleanValue METEOR_BLOCK_DAMAGE;
    public static final ModConfigSpec.DoubleValue DUNGEON_BOSS_HEALTH_MOD;
    public static final ModConfigSpec.DoubleValue SPACE_STATION_ENERGY_SCALAR;
    public static final ModConfigSpec.BooleanValue DISABLE_SPACE_STATION_CREATION;
    public static final ModConfigSpec.BooleanValue ENABLE_SPACE_RACE_MANAGER_POPUP;
    public static final ModConfigSpec.BooleanValue DISABLE_SMALL_ASTEROIDS;
    public static final ModConfigSpec.IntValue ASTRO_MINER_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DISABLE_ROCKET_LAUNCH_DIMENSIONS;
    public static final ModConfigSpec.BooleanValue DISABLE_ROCKETS_TO_OVERWORLD;
    public static final ModConfigSpec.IntValue OTHER_PLANET_WORLD_BORDERS;
    public static final ModConfigSpec.BooleanValue FORCE_OVERWORLD_RESPAWN;
    public static final ModConfigSpec.BooleanValue DISABLE_LANDER;
    public static final ModConfigSpec.BooleanValue DISABLE_SPACESHIP_GRIEF;
    public static final ModConfigSpec.BooleanValue SPACE_STATIONS_REQUIRE_PERMISSION;
    public static final ModConfigSpec.IntValue SUFFOCATION_DAMAGE;
    public static final ModConfigSpec.IntValue SUFFOCATION_COOLDOWN;
    public static final ModConfigSpec.BooleanValue ENABLE_SEALER_EDGE_CHECKS;
    public static final ModConfigSpec.IntValue ROCKET_FUEL_FACTOR;
    public static final ModConfigSpec.BooleanValue ALLOW_LIQUID_GRATINGS;
    public static final ModConfigSpec.BooleanValue LAUNCH_CONTROLLER_CHUNK_LOAD;
    public static final ModConfigSpec.BooleanValue DISABLE_AMBIENT_LIGHTNING;

    /**
     * The common on/off settings by name, for the {@code galacticraftcore:config} recipe condition, placement filter and
     * loot condition ({@link io.github.reiko45.galacticraft.world.ConfigChecks}).
     */
    private static final java.util.Map<String, ModConfigSpec.BooleanValue> COMMON_FLAGS = new java.util.LinkedHashMap<>();

    static {
        final ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        ENERGY_DISPLAY_UNIT = client
                .comment("Unit used to display energy in GUIs and tooltips. 1 FE = 1.6 gJ.")
                .translation("config.galacticraftcore.energy_display_unit")
                .defineEnum("energyDisplayUnit", EnergyUnit.GJ);
        MORE_STARS = client
                .comment("Setting this to false will revert night skies back to default minecraft star count")
                .translation("config.galacticraftcore.more_stars")
                .define("moreStars", true);
        DISABLE_SPACESHIP_PARTICLES = client
                .comment("If you have FPS problems, setting this to true will help if rocket particles are in your sights")
                .translation("config.galacticraftcore.disable_spaceship_particles")
                .define("disableSpaceshipParticles", false);
        DISABLE_VEHICLE_CAMERA_CHANGES = client
                .comment("If you're using this mod in virtual reality, or if you don't want the camera changes when entering a Galacticraft vehicle, set this to true.")
                .translation("config.galacticraftcore.disable_vehicle_camera_changes")
                .define("disableVehicleCameraChanges", false);
        OXYGEN_INDICATOR_LEFT = client
                .comment("If true, this will move the Oxygen Indicator to the left side. You can combine this with oxygenIndicatorBottom")
                .translation("config.galacticraftcore.oxygen_indicator_left")
                .define("oxygenIndicatorLeft", false);
        OXYGEN_INDICATOR_BOTTOM = client
                .comment("If true, this will move the Oxygen Indicator to the bottom. You can combine this with oxygenIndicatorLeft")
                .translation("config.galacticraftcore.oxygen_indicator_bottom")
                .define("oxygenIndicatorBottom", false);
        CLIENT_SPEC = client.build();

        final ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.translation("config.galacticraftcore.difficulty").push("difficulty");
        QUICK_MODE = common
                .comment("Set this to true for less metal use in Galacticraft recipes (makes the game easier).")
                .translation("config.galacticraftcore.quick_mode")
                .worldRestart().define("quickMode", false);
        COMMON_FLAGS.put("quickMode", QUICK_MODE);
        common.pop().translation("config.galacticraftcore.worldgen").push("worldgen");
        OIL_GEN_FACTOR = common
                .comment("Increasing this will increase amount of oil that will generate in each chunk.")
                .translation("config.galacticraftcore.oil_gen_factor")
                .defineInRange("oilGenFactor", 1.8D, 0.0D, 20.0D);
        ENABLE_TIN_ORE_GEN = worldgen(common, "enableTinOreGen", true, "If this is enabled, tin ore will generate on the overworld.");
        ENABLE_ALUMINUM_ORE_GEN = worldgen(common, "enableAluminumOreGen", true, "If this is enabled, aluminum ore will generate on the overworld.");
        ENABLE_SILICON_ORE_GEN = worldgen(common, "enableSiliconOreGen", true, "If this is enabled, silicon ore will generate on the overworld.");
        DISABLE_CHEESE_MOON = worldgen(common, "disableCheeseMoon", false, "Disable Cheese Ore Gen on Moon.");
        DISABLE_TIN_MOON = worldgen(common, "disableTinMoon", false, "Disable Tin Ore Gen on Moon.");
        DISABLE_COPPER_MOON = worldgen(common, "disableCopperMoon", false, "Disable Copper Ore Gen on Moon.");
        DISABLE_SAPPHIRE_MOON = worldgen(common, "disableSapphireMoon", false, "Disable Sapphire Ore Gen on Moon.");
        DISABLE_MOON_VILLAGE_GEN = worldgen(common, "disableMoonVillageGen", false, "If true, moon villages will not generate.");
        DISABLE_DESH_MARS = worldgen(common, "disableDeshMars", false, "Disable Desh Ore Gen on Mars.");
        DISABLE_TIN_MARS = worldgen(common, "disableTinMars", false, "Disable Tin Ore Gen on Mars.");
        DISABLE_COPPER_MARS = worldgen(common, "disableCopperMars", false, "Disable Copper Ore Gen on Mars.");
        DISABLE_IRON_MARS = worldgen(common, "disableIronMars", false, "Disable Iron Ore Gen on Mars.");
        DISABLE_IRON_ASTEROIDS = worldgen(common, "disableIronAsteroids", false, "Disable Iron Ore Gen on Asteroids.");
        DISABLE_ALUMINUM_ASTEROIDS = worldgen(common, "disableAluminumAsteroids", false, "Disable Aluminum Ore Gen on Asteroids.");
        DISABLE_ILMENITE_ASTEROIDS = worldgen(common, "disableIlmeniteAsteroids", false, "Disable Ilmenite Ore Gen on Asteroids.");
        DISABLE_ALUMINUM_VENUS = worldgen(common, "disableAluminumVenus", false, "Disable Aluminum Ore Gen on Venus.");
        DISABLE_COPPER_VENUS = worldgen(common, "disableCopperVenus", false, "Disable Copper Ore Gen on Venus.");
        DISABLE_GALENA_VENUS = worldgen(common, "disableGalenaVenus", false, "Disable Galena Ore Gen on Venus.");
        DISABLE_QUARTZ_VENUS = worldgen(common, "disableQuartzVenus", false, "Disable Quartz Ore Gen on Venus.");
        DISABLE_SILICON_VENUS = worldgen(common, "disableSiliconVenus", false, "Disable Silicon Ore Gen on Venus.");
        DISABLE_TIN_VENUS = worldgen(common, "disableTinVenus", false, "Disable Tin Ore Gen on Venus.");
        DISABLE_SOLAR_VENUS = worldgen(common, "disableSolarVenus", false, "Disable Solar Ore Gen on Venus.");
        common.pop();
        COMMON_SPEC = common.build();

        final ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        HARD_MODE = server
                .comment("Set this to true for increased difficulty in modpacks: machines use more power, meteors hit harder,"
                        + " drifting and mining in zero gravity are slower and suffocation damage builds up.")
                .translation("config.galacticraftcore.hard_mode")
                .define("hardMode", false);
        METEOR_SPAWN_MOD = server
                .comment("Set to a value between 0.0 and 1.0 to decrease meteor spawn chance (all dimensions).")
                .translation("config.galacticraftcore.meteor_spawn_mod")
                .defineInRange("meteorSpawnMod", 1.0D, 0.0D, 1.0D);
        METEOR_BLOCK_DAMAGE = server
                .comment("Set to false to stop meteors from breaking blocks on contact.")
                .translation("config.galacticraftcore.meteor_block_damage")
                .define("meteorBlockDamage", true);
        DUNGEON_BOSS_HEALTH_MOD = server
                .comment("Change this if you wish to balance the mod (if you have more powerful weapon mods).")
                .translation("config.galacticraftcore.dungeon_boss_health_mod")
                .defineInRange("dungeonBossHealthMod", 1.0D, 0.25D, 10.0D);
        SPACE_STATION_ENERGY_SCALAR = server
                .comment("Solar panels will work (default 2x) more effective on space stations.")
                .translation("config.galacticraftcore.space_station_energy_scalar")
                .defineInRange("spaceStationEnergyScalar", 2.0D, 0.0D, 100.0D);
        DISABLE_SPACE_STATION_CREATION = server
                .comment("If set to true on a server, players will be unable to create space stations.")
                .translation("config.galacticraftcore.disable_space_station_creation")
                .define("disableSpaceStationCreation", false);
        ENABLE_SPACE_RACE_MANAGER_POPUP = server
                .comment("Enable this if you want the Space Race Manager to pop up when a player first logs in.")
                .translation("config.galacticraftcore.enable_space_race_manager_popup")
                .define("enableSpaceRaceManagerPopup", false);
        DISABLE_SMALL_ASTEROIDS = server
                .comment("Disable Small Asteroid spawning (Asteroids)")
                .translation("config.galacticraftcore.disable_small_asteroids")
                .define("disableSmallAsteroids", false);
        ASTRO_MINER_MAX = server
                .comment("Maximum number of Astro Miners per player")
                .translation("config.galacticraftcore.astro_miners_max")
                .defineInRange("maximumAstroMiners", 6, 0, 1000);
        DISABLE_ROCKET_LAUNCH_DIMENSIONS = server
                .comment("Worlds where rockets should not launch - this should always include the Nether. \"*\" stops launches from"
                        + " every world that isn't the Overworld or a Galacticraft one (rockets always launch from those).")
                .translation("config.galacticraftcore.disable_rocket_launch_dimensions")
                .defineListAllowEmpty("disableRocketLaunchDimensions", List.of("minecraft:the_nether", "minecraft:the_end"),
                        () -> "minecraft:the_nether", entry -> entry instanceof String text
                                && ("*".equals(text) || net.minecraft.resources.ResourceLocation.tryParse(text) != null));
        DISABLE_ROCKETS_TO_OVERWORLD = server
                .comment("If true, rockets will be unable to reach the Overworld (only use this in special modpacks!)")
                .translation("config.galacticraftcore.disable_rockets_to_overworld")
                .define("disableRocketsToOverworld", false);
        OTHER_PLANET_WORLD_BORDERS = server
                .comment("Set this to 0 for no borders (default). If set to e.g. 2000, players will land on the Moon inside the x,z range -2000 to 2000.")
                .translation("config.galacticraftcore.other_planet_world_borders")
                .defineInRange("otherPlanetWorldBorders", 0, 0, 30_000_000);
        FORCE_OVERWORLD_RESPAWN = server
                .comment("By default, you will respawn on Galacticraft dimensions if you die. If you are dying over and over on a planet,"
                        + " set this to true, and you will respawn back on the Overworld.")
                .translation("config.galacticraftcore.force_overworld_respawn")
                .define("forceOverworldRespawn", false);
        DISABLE_LANDER = server
                .comment("If this is true, the player will parachute onto the Moon instead - use only in debug situations.")
                .translation("config.galacticraftcore.disable_lander")
                .define("disableLander", false);
        DISABLE_SPACESHIP_GRIEF = server
                .comment("Spaceships will not explode on contact if set to true.")
                .translation("config.galacticraftcore.disable_spaceship_grief")
                .define("disableSpaceshipGrief", false);
        SPACE_STATIONS_REQUIRE_PERMISSION = server
                .comment("While true, space stations require you to invite other players using /ssinvite <playername>")
                .translation("config.galacticraftcore.space_stations_require_permission")
                .define("spaceStationsRequirePermission", true);
        SUFFOCATION_DAMAGE = server
                .comment("Change this value to modify the damage taken per suffocation tick")
                .translation("config.galacticraftcore.suffocation_damage")
                .defineInRange("suffocationDamage", 2, 0, 50);
        SUFFOCATION_COOLDOWN = server
                .comment("Lower/Raise this value to change time between suffocation damage ticks (allowed range 50-200)")
                .translation("config.galacticraftcore.suffocation_cooldown")
                .defineInRange("suffocationCooldown", 100, 50, 200);
        ENABLE_SEALER_EDGE_CHECKS = server
                .comment("If this is enabled, areas sealed by Oxygen Sealers will run a seal check when the player breaks or places a block"
                        + " (or on block updates). This should be enabled for a 100% accurate sealed status, but can be disabled on servers"
                        + " for performance reasons.")
                .translation("config.galacticraftcore.enable_sealer_edge_checks")
                .define("enableSealerEdgeChecks", true);
        ROCKET_FUEL_FACTOR = server
                .comment("The normal factor is 1. Increase this to 2 - 5 if other mods with a lot of oil are installed to increase GC rocket fuel requirement.")
                .translation("config.galacticraftcore.rocket_fuel_factor")
                .defineInRange("rocketFuelFactor", 1, 1, 10);
        ALLOW_LIQUID_GRATINGS = server
                .comment("Liquids will not flow into Grating block if this is set to false.")
                .translation("config.galacticraftcore.allow_liquid_gratings")
                .define("allowLiquidGratings", true);
        LAUNCH_CONTROLLER_CHUNK_LOAD = server
                .comment("Whether or not the launch controller acts as a chunk loader. Will cause issues if disabled!")
                .translation("config.galacticraftcore.launch_controller_chunk_load")
                .define("launchControllerChunkLoad", true);
        DISABLE_AMBIENT_LIGHTNING = server
                .comment("Disables background thunder and lightning (Venus).")
                .translation("config.galacticraftcore.disable_ambient_lightning")
                .define("disableAmbientLightning", false);
        SERVER_SPEC = server.build();
    }

    private static ModConfigSpec.BooleanValue worldgen(ModConfigSpec.Builder builder, String name, boolean value, String comment) {
        final ModConfigSpec.BooleanValue option = builder.comment(comment)
                .translation("config.galacticraftcore." + name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT))
                .worldRestart().define(name, value);
        COMMON_FLAGS.put(name, option);
        return option;
    }

    /** A common on/off setting by its name in the file, or empty if there is none. */
    public static java.util.Optional<Boolean> commonFlag(String name) {
        return java.util.Optional.ofNullable(COMMON_FLAGS.get(name)).map(GCConfig::common);
    }

    public static java.util.Set<String> commonFlagNames() {
        return java.util.Collections.unmodifiableSet(COMMON_FLAGS.keySet());
    }

    /** A server setting, or its default where no world is loaded (tests of pieces outside a server, the title screen). */
    public static <T> T server(ModConfigSpec.ConfigValue<T> value) {
        return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** A common setting, or its default before the file is read. */
    public static <T> T common(ModConfigSpec.ConfigValue<T> value) {
        return COMMON_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** A client setting, or its default before the file is read. */
    public static <T> T client(ModConfigSpec.ConfigValue<T> value) {
        return CLIENT_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /**
     * 1.12.2 EntityAutoRocket#onLaunch: rockets launch from the Overworld and Galacticraft worlds, and from any other world
     * not on the disableRocketLaunchDimensions list ("*" for all of them).
     */
    public static boolean rocketsLaunchFrom(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        if (dimension.equals(net.minecraft.world.level.Level.OVERWORLD)
                || dimension.location().getNamespace().equals(GalacticraftCore.MOD_ID)) {
            return true;
        }
        final List<? extends String> blocked = server(DISABLE_ROCKET_LAUNCH_DIMENSIONS);
        return !blocked.contains("*") && !blocked.contains(dimension.location().toString());
    }

    public static boolean hardMode() {
        return server(HARD_MODE);
    }

    public static boolean quickMode() {
        return common(QUICK_MODE);
    }

    private GCConfig() {
    }
}
