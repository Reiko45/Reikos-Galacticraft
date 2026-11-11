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
    REFINING("gui.galacticraftcore.status.refining", ChatFormatting.DARK_GREEN),
    // Oxygen machines (1.12.2 TileBaseElectricBlock#getGUIstatus and the oxygen GUIs' getStatus)
    ACTIVE("gui.galacticraftcore.status.active", ChatFormatting.DARK_GREEN),
    READY("gui.galacticraftcore.status.ready", ChatFormatting.GOLD),
    MISSING_POWER("gui.galacticraftcore.status.missing_power", ChatFormatting.DARK_RED),
    LOW_MISSING_POWER("gui.galacticraftcore.status.missing_power", ChatFormatting.GOLD),
    DISABLED("gui.galacticraftcore.status.disabled", ChatFormatting.GOLD),
    MISSING_OXYGEN("gui.galacticraftcore.status.missing_oxygen", ChatFormatting.DARK_RED),
    MISSING_LEAVES("gui.galacticraftcore.status.missing_leaves", ChatFormatting.DARK_RED),
    MISSING_TANK("gui.galacticraftcore.status.missing_tank", ChatFormatting.DARK_RED),
    FULL_TANK("gui.galacticraftcore.status.full_tank", ChatFormatting.DARK_RED),
    TANK_EMPTY("gui.galacticraftcore.status.tank_empty", ChatFormatting.DARK_RED),
    SEALER_BLOCKED("gui.galacticraftcore.status.sealer_blocked", ChatFormatting.DARK_RED),
    CHECKING_SEAL("gui.galacticraftcore.status.checking_seal", ChatFormatting.GOLD),
    CHECK_STARTING("gui.galacticraftcore.status.check_starting", ChatFormatting.GOLD),
    CHECK_PENDING("gui.galacticraftcore.status.check_pending", ChatFormatting.GOLD),
    UNSEALED("gui.galacticraftcore.status.unsealed", ChatFormatting.DARK_RED),
    SEALED("gui.galacticraftcore.status.sealed", ChatFormatting.DARK_GREEN),
    // Fuel Loader (1.12.2 GuiFuelLoader#getStatus)
    NO_FUEL("gui.galacticraftcore.status.no_fuel", ChatFormatting.DARK_RED);

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
