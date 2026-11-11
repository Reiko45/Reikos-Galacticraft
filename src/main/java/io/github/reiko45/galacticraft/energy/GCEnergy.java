package io.github.reiko45.galacticraft.energy;

import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** Energy capability and unit conversion. */
public final class GCEnergy {
    /** 1.12.2 EnergyConfigHandler.RF_RATIO: 1 RF (= 1 FE) is worth 1.6 gJ. */
    public static final float GJ_PER_FE = 1.6F;

    public static final BlockCapability<IGCEnergyHandler, @Nullable Direction> BLOCK =
            BlockCapability.createSided(GalacticraftCore.id("energy"), IGCEnergyHandler.class);

    private GCEnergy() {
    }

    public static int toFE(float gJ) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(gJ / GJ_PER_FE));
    }

    public static float fromFE(int fe) {
        return fe * GJ_PER_FE;
    }

    /** Presents a Galacticraft handler as FE for other mods' cables and machines. */
    public static IEnergyStorage asFE(IGCEnergyHandler handler) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                // Only accept whole FE so the reported amount matches what was stored.
                final int fe = toFE(handler.receiveGJ(fromFE(toReceive), true) + 1.0E-4F);
                if (!simulate && fe > 0) {
                    handler.receiveGJ(fromFE(fe), false);
                }
                return fe;
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                // Only extract whole FE so no gJ is lost to rounding.
                final int fe = toFE(handler.extractGJ(fromFE(toExtract), true) + 1.0E-4F);
                if (!simulate && fe > 0) {
                    handler.extractGJ(fromFE(fe), false);
                }
                return fe;
            }

            @Override
            public int getEnergyStored() {
                return toFE(handler.getStoredGJ());
            }

            @Override
            public int getMaxEnergyStored() {
                return toFE(handler.getCapacityGJ());
            }

            @Override
            public boolean canExtract() {
                return handler.canExtract();
            }

            @Override
            public boolean canReceive() {
                return handler.canReceive();
            }
        };
    }

    /** Lets the Galacticraft wire network talk to other mods' FE blocks. */
    public static IGCEnergyHandler fromFE(IEnergyStorage storage) {
        return new IGCEnergyHandler() {
            @Override
            public float receiveGJ(float amount, boolean simulate) {
                return fromFE(storage.receiveEnergy(toFE(amount), simulate));
            }

            @Override
            public float extractGJ(float amount, boolean simulate) {
                return fromFE(storage.extractEnergy(toFE(amount), simulate));
            }

            @Override
            public float getStoredGJ() {
                return fromFE(storage.getEnergyStored());
            }

            @Override
            public float getCapacityGJ() {
                return fromFE(storage.getMaxEnergyStored());
            }

            @Override
            public boolean canReceive() {
                return storage.canReceive();
            }

            @Override
            public boolean canExtract() {
                return storage.canExtract();
            }
        };
    }
}
