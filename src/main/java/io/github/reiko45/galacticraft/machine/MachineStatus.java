package io.github.reiko45.galacticraft.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 1.12.2 TileBaseElectricBlock#getGUIstatus in its shortened form (as the machine GUIs call it), synced to the GUI as
 * an ordinal.
 */
public enum MachineStatus {
    RUNNING("gui.galacticraftcore.status.running", ChatFormatting.GREEN),
    IDLE("gui.galacticraftcore.status.idle", ChatFormatting.GOLD),
    OFF("gui.galacticraftcore.status.off", ChatFormatting.DARK_RED),
    NO_POWER("gui.galacticraftcore.status.missing_power_short", ChatFormatting.DARK_RED),
    LOW_POWER("gui.galacticraftcore.status.missing_power_short", ChatFormatting.GOLD),
    COMPRESSING("gui.galacticraftcore.status.compressing", ChatFormatting.DARK_GREEN),
    NO_OIL("gui.galacticraftcore.status.no_oil", ChatFormatting.DARK_RED),
    REFINING("gui.galacticraftcore.status.refining", ChatFormatting.DARK_GREEN);

    private final String key;
    private final ChatFormatting color;

    MachineStatus(String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    public Component text() {
        return Component.translatable(this.key).withStyle(this.color);
    }

    public static MachineStatus byOrdinal(int ordinal) {
        final MachineStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
