package io.github.reiko45.galacticraft.energy;

import io.github.reiko45.galacticraft.GCConfig;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Port of 1.12.2 {@code EnergyDisplayHelper}: "12,345 gJ" (or FE when configured). */
public final class EnergyDisplay {
    private EnergyDisplay() {
    }

    public static String format(float gJ) {
        if (GCConfig.CLIENT_SPEC.isLoaded() && GCConfig.ENERGY_DISPLAY_UNIT.get() == GCConfig.EnergyUnit.FE) {
            return String.format(Locale.ROOT, "%,d FE", GCEnergy.toFE(gJ));
        }
        return String.format(Locale.ROOT, "%,d gJ", (int) Math.floor(gJ));
    }

    public static Component energy(float gJ) {
        return Component.translatable("gui.galacticraftcore.energy").append(": " + format(gJ)).withStyle(ChatFormatting.GREEN);
    }

    public static Component maxEnergy(float gJ) {
        return Component.translatable("gui.galacticraftcore.max_energy").append(": " + format(gJ)).withStyle(ChatFormatting.RED);
    }

    public static Component perTick(float gJ) {
        if (GCConfig.CLIENT_SPEC.isLoaded() && GCConfig.ENERGY_DISPLAY_UNIT.get() == GCConfig.EnergyUnit.FE) {
            return Component.literal(String.format(Locale.ROOT, "%.1f FE/t", gJ / GCEnergy.GJ_PER_FE));
        }
        return Component.literal(String.format(Locale.ROOT, "%.1f gJ/t", gJ));
    }
}
