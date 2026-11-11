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
        SERVER_SPEC = server.build();
    }

    private GCConfig() {
    }
}
