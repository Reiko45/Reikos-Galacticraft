package io.github.reiko45.galacticraft.energy;

import net.minecraft.nbt.CompoundTag;

/**
 * Port of 1.12.2 {@code EnergyStorage}: a float gJ buffer with per-call receive/extract limits.
 * Side restrictions are applied by {@link #view(boolean, boolean)}.
 */
public class GCEnergyStorage implements IGCEnergyHandler {
    private float stored;
    private float capacity;
    private float maxReceive;
    private float maxExtract;
    private float extractRemaining;
    private int tier = 1;
    private int poweredTier = 1;
    private final Runnable onChanged;

    public GCEnergyStorage(float capacity, float maxReceive, float maxExtract, Runnable onChanged) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.extractRemaining = maxExtract;
        this.onChanged = onChanged;
    }

    /**
     * Start of the owner's tick: refill the per-tick output budget (1.12.2 maxExtractRemaining), so direct
     * pushes, the wire network and battery charging together never exceed maxExtract per tick.
     */
    public void resetTick() {
        this.extractRemaining = this.maxExtract;
    }

    @Override
    public float receiveGJ(float amount, boolean simulate) {
        final float accepted = Math.max(0.0F, Math.min(this.capacity - this.stored, Math.min(this.maxReceive, amount)));
        if (!simulate && accepted > 0.0F) {
            this.stored += accepted;
            this.onChanged.run();
        }
        return accepted;
    }

    @Override
    public float extractGJ(float amount, boolean simulate) {
        final float removed = Math.max(0.0F, Math.min(this.stored, Math.min(this.extractRemaining, amount)));
        if (!simulate && removed > 0.0F) {
            this.stored -= removed;
            this.extractRemaining -= removed;
            this.onChanged.run();
        }
        return removed;
    }

    /** Machine-internal use (consumption, generation), ignoring the transfer limits. */
    public boolean consume(float amount) {
        if (this.stored + 1.0E-4F < amount) {
            return false;
        }
        this.stored = Math.max(0.0F, this.stored - amount);
        this.onChanged.run();
        return true;
    }

    /** Machine-internal generation, ignoring the transfer limits. @return gJ actually stored */
    public float generate(float amount) {
        final float added = Math.max(0.0F, Math.min(this.capacity - this.stored, amount));
        if (added > 0.0F) {
            this.stored += added;
            this.onChanged.run();
        }
        return added;
    }

    @Override
    public float getStoredGJ() {
        return this.stored;
    }

    @Override
    public float getCapacityGJ() {
        return this.capacity;
    }

    public void setStored(float stored) {
        this.stored = Math.max(0.0F, Math.min(this.capacity, stored));
    }

    public void setCapacity(float capacity) {
        this.capacity = capacity;
        this.stored = Math.min(this.stored, capacity);
    }

    public void setMaxReceive(float maxReceive) {
        this.maxReceive = maxReceive;
    }

    public void setMaxExtract(float maxExtract) {
        this.maxExtract = maxExtract;
        this.extractRemaining = maxExtract;
    }

    public float maxExtract() {
        return this.maxExtract;
    }

    public float maxReceive() {
        return this.maxReceive;
    }

    @Override
    public int tier() {
        return this.tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    @Override
    public void setPoweredTier(int tier) {
        this.poweredTier = tier;
    }

    public int poweredTier() {
        return this.poweredTier;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0.0F;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0.0F;
    }

    public void save(CompoundTag tag) {
        tag.putFloat("Energy", this.stored);
        tag.putByte("PoweredTier", (byte) this.poweredTier);
    }

    public void load(CompoundTag tag) {
        setStored(tag.getFloat("Energy"));
        this.poweredTier = Math.max(1, tag.getByte("PoweredTier"));
    }

    /** A restricted view used for a single face: input-only, output-only or both. */
    public IGCEnergyHandler view(boolean allowReceive, boolean allowExtract) {
        final GCEnergyStorage self = this;
        return new IGCEnergyHandler() {
            @Override
            public float receiveGJ(float amount, boolean simulate) {
                return allowReceive ? self.receiveGJ(amount, simulate) : 0.0F;
            }

            @Override
            public float extractGJ(float amount, boolean simulate) {
                return allowExtract ? self.extractGJ(amount, simulate) : 0.0F;
            }

            @Override
            public float getStoredGJ() {
                return self.getStoredGJ();
            }

            @Override
            public float getCapacityGJ() {
                return self.getCapacityGJ();
            }

            @Override
            public boolean canReceive() {
                return allowReceive && self.canReceive();
            }

            @Override
            public boolean canExtract() {
                return allowExtract && self.canExtract();
            }

            @Override
            public int tier() {
                return self.tier();
            }

            @Override
            public void setPoweredTier(int tier) {
                self.setPoweredTier(tier);
            }
        };
    }
}
