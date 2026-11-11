package io.github.reiko45.galacticraft.energy;

/**
 * Galacticraft energy in gJ, as floats so the 1.12.2 machine numbers (fractions of a gJ per tick
 * included) port unchanged. Exposed on blocks through {@link GCEnergy#BLOCK}; every Galacticraft
 * block that has this also exposes NeoForge's FE {@code IEnergyStorage} for other mods.
 */
public interface IGCEnergyHandler {
    /** @return gJ accepted */
    float receiveGJ(float amount, boolean simulate);

    /** @return gJ removed */
    float extractGJ(float amount, boolean simulate);

    float getStoredGJ();

    float getCapacityGJ();

    boolean canReceive();

    boolean canExtract();

    /** Power tier this handler supplies (1.12.2 tierGC): 1 normal, 2 for Energy Storage Clusters and similar. */
    default int tier() {
        return 1;
    }

    /**
     * Called after energy arrives with the tier it was delivered at (1.12.2 poweredByTierGC: the lower of the
     * source tier and the weakest wire on the way). Tier-2 power makes some machines faster.
     */
    default void setPoweredTier(int tier) {
    }
}
