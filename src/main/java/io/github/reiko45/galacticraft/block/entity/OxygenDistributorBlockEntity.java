package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import io.github.reiko45.galacticraft.oxygen.OxygenReliantBlock;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Oxygen Bubble Distributor (1.12.2 TileEntityOxygenDistributor): 6,000 mB tank, 8 mB/t of oxygen and 10 gJ/t. While
 * running its breathable bubble grows by 0.01 blocks per tick up to a radius of 10; without power or oxygen it shrinks
 * by 0.1 per tick. Torches inside light up. The bubble can be shown or hidden. Slots: 0 battery, 1 oxygen supply.
 */
public class OxygenDistributorBlockEntity extends OxygenMachineBlockEntity {
    public static final float MAX_BUBBLE = 10.0F;
    private static final Map<Level, Set<OxygenDistributorBlockEntity>> LOADED = new WeakHashMap<>();

    private float bubbleSize;
    private float lastSyncedSize;
    private boolean bubbleVisible = true;
    private boolean active;

    public OxygenDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_DISTRIBUTOR.get(), pos, state, 2, 6000, 8, 16_000.0F, 10.0F);
    }

    /** Distributors loaded in a level (server or client). */
    public static Set<OxygenDistributorBlockEntity> loaded(Level level) {
        final Set<OxygenDistributorBlockEntity> set = LOADED.get(level);
        return set == null ? Set.of() : set;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            LOADED.computeIfAbsent(this.level, l -> Collections.newSetFromMap(new java.util.IdentityHashMap<>())).add(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            LOADED.getOrDefault(this.level, Set.of()).remove(this);
        }
        super.setRemoved();
    }

    /** Broken: torches that were in the bubble re-check their oxygen. */
    @Override
    public void onBroken() {
        if (this.level != null && !this.level.isClientSide) {
            LOADED.getOrDefault(this.level, Set.of()).remove(this);
            updateReliantBlocks(Mth.ceil(this.bubbleSize), 0.0F);
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (this.level != null) {
            LOADED.getOrDefault(this.level, Set.of()).remove(this);
        }
        super.onChunkUnloaded();
    }

    @Override
    protected int batterySlot() {
        return 0;
    }

    @Override
    protected int oxygenSupplySlot() {
        return 1;
    }

    @Override
    protected boolean shouldUseEnergy() {
        return this.oxygen.getFluidAmount() > this.oxygenPerTick;
    }

    @Override
    protected boolean shouldUseOxygen() {
        return this.hasEnoughEnergyToRun;
    }

    @Override
    protected void work() {
        if (this.energy.getStoredGJ() > 0.0F && this.hasEnoughEnergyToRun && this.oxygen.getFluidAmount() > this.oxygenPerTick) {
            this.bubbleSize += 0.01F;
        } else {
            this.bubbleSize -= 0.1F;
        }
        this.bubbleSize = Mth.clamp(this.bubbleSize, 0.0F, MAX_BUBBLE);
        this.active = this.bubbleSize >= 1.0F && this.hasEnoughEnergyToRun && this.oxygen.getFluidAmount() > this.oxygenPerTick;
        if (this.ticks % (this.active ? 20 : 4) == 0 && (this.active || this.bubbleSize > 0.0F || this.lastSyncedSize > 0.0F)) {
            updateReliantBlocks(Mth.floor(this.bubbleSize) + 4, this.bubbleSize);
        }
        // Sync the bubble for rendering in 0.1 steps (and when it reaches either end).
        if (Math.abs(this.bubbleSize - this.lastSyncedSize) >= 0.1F || this.bubbleSize != this.lastSyncedSize
                && (this.bubbleSize == 0.0F || this.bubbleSize == MAX_BUBBLE)) {
            this.lastSyncedSize = this.bubbleSize;
            sync();
        }
    }

    /** Light the oxygen-reliant blocks (torches) inside the bubble and let the ones outside re-check. */
    private void updateReliantBlocks(int range, float radius) {
        if (this.level == null) {
            return;
        }
        final float r2 = radius * radius;
        for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-range, -range, -range), this.worldPosition.offset(range, range, range))) {
            final BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof OxygenReliantBlock reliant) {
                if (pos.distSqr(this.worldPosition) <= r2) {
                    reliant.onOxygenAdded(this.level, pos.immutable(), state);
                } else {
                    reliant.onOxygenRemoved(this.level, pos.immutable(), state);
                }
            }
        }
    }

    /** 1.12.2 inBubble: inside the sphere around the block centre. */
    public boolean inBubble(double x, double y, double z) {
        final double r2 = (double) this.bubbleSize * this.bubbleSize;
        final double dx = this.worldPosition.getX() + 0.5D - x;
        final double dz = this.worldPosition.getZ() + 0.5D - z;
        final double dy = this.worldPosition.getY() + 0.5D - y;
        return dx * dx + dz * dz + dy * dy < r2;
    }

    public float bubbleSize() {
        return this.bubbleSize;
    }

    public void setBubbleSize(float size) {
        this.bubbleSize = Mth.clamp(size, 0.0F, MAX_BUBBLE);
    }

    public boolean bubbleVisible() {
        return this.bubbleVisible;
    }

    public void toggleBubbleVisible() {
        this.bubbleVisible = !this.bubbleVisible;
        setChanged();
        sync();
    }

    public AABB renderBounds() {
        return new AABB(this.worldPosition).inflate(MAX_BUBBLE);
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public MachineStatus status() {
        if (this.oxygen.getFluidAmount() < this.oxygenPerTick) {
            return MachineStatus.MISSING_OXYGEN;
        }
        return electricStatus();
    }

    @Override
    protected int guiValue1() {
        return this.bubbleVisible ? 1 : 0;
    }

    @Override
    public OxygenMachineMenu.Kind menuKind() {
        return OxygenMachineMenu.Kind.DISTRIBUTOR;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("BubbleSize", this.bubbleSize);
        tag.putBoolean("BubbleVisible", this.bubbleVisible);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.bubbleSize = tag.getFloat("BubbleSize");
        this.lastSyncedSize = this.bubbleSize;
        this.bubbleVisible = !tag.contains("BubbleVisible") || tag.getBoolean("BubbleVisible");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putFloat("BubbleSize", this.bubbleSize);
        tag.putBoolean("BubbleVisible", this.bubbleVisible);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.bubbleSize = tag.getFloat("BubbleSize");
        this.bubbleVisible = tag.getBoolean("BubbleVisible");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
