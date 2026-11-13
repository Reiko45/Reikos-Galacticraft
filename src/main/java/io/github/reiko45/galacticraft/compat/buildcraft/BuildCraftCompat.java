package io.github.reiko45.galacticraft.compat.buildcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * BuildCraft Reborn support (1.12.2's BuildCraft compatibility: CompatibilityManager, EnergyConfigHandler's MJ, the
 * universal wrench). The classes that use BuildCraft's API are only loaded when it is installed.
 */
public final class BuildCraftCompat {
    public static final String MOD_ID = "buildcraftreborn";

    private BuildCraftCompat() {
    }

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded(MOD_ID);
    }

    public static void init(IEventBus modBus) {
        if (loaded()) {
            BuildCraftMj.init(modBus);
        }
    }
}
