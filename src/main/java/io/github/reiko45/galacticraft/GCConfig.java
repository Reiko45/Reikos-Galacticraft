package io.github.reiko45.galacticraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config (energy display units). Ported from the relevant parts of 1.12.2 EnergyConfigHandler. */
public final class GCConfig {
    public enum EnergyUnit {
        GJ,
        FE
    }

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.EnumValue<EnergyUnit> ENERGY_DISPLAY_UNIT;

    static {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENERGY_DISPLAY_UNIT = builder
                .comment("Unit used to display energy in GUIs and tooltips. 1 FE = 1.6 gJ.")
                .translation("config.galacticraftcore.energy_display_unit")
                .defineEnum("energyDisplayUnit", EnergyUnit.GJ);
        CLIENT_SPEC = builder.build();
    }

    private GCConfig() {
    }
}
