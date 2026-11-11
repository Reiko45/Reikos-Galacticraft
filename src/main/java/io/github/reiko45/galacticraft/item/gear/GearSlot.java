package io.github.reiko45.galacticraft.item.gear;

/**
 * The Galacticraft inventory slots (1.12.2 InventoryExtended / EnumExtendedInventorySlot), in the 1.12.2 order so saved
 * slot numbers keep their meaning.
 */
public enum GearSlot {
    MASK, GEAR, LEFT_TANK, RIGHT_TANK, PARACHUTE, FREQUENCY_MODULE,
    THERMAL_HELMET, THERMAL_CHESTPLATE, THERMAL_LEGGINGS, THERMAL_BOOTS, SHIELD_CONTROLLER;

    public static final int COUNT = values().length;

    public int index() {
        return ordinal();
    }
}
