package io.github.reiko45.galacticraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config (energy display units, from 1.12.2 EnergyConfigHandler) and server config (difficulty settings from
 * 1.12.2 ConfigManagerCore).
 */
public final class GCConfig {
    public enum EnergyUnit {
        GJ,
        FE
    }

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.EnumValue<EnergyUnit> ENERGY_DISPLAY_UNIT;

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue METEOR_SPAWN_MOD;
    public static final ModConfigSpec.BooleanValue METEOR_BLOCK_DAMAGE;
    public static final ModConfigSpec.DoubleValue DUNGEON_BOSS_HEALTH_MOD;
    public static final ModConfigSpec.DoubleValue SPACE_STATION_ENERGY_SCALAR;
    public static final ModConfigSpec.BooleanValue DISABLE_SPACE_STATION_CREATION;
    public static final ModConfigSpec.BooleanValue ENABLE_SPACE_RACE_MANAGER_POPUP;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENERGY_DISPLAY_UNIT = builder
                .comment("Unit used to display energy in GUIs and tooltips. 1 FE = 1.6 gJ.")
                .translation("config.galacticraftcore.energy_display_unit")
                .defineEnum("energyDisplayUnit", EnergyUnit.GJ);
        CLIENT_SPEC = builder.build();

        final ModConfigSpec.Builder server = new ModConfigSpec.Builder();
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
        SERVER_SPEC = server.build();
    }

    private GCConfig() {
    }
}
