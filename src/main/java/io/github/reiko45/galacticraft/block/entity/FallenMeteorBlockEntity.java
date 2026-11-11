package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.2 TileEntityFallenMeteor: heat starts at 5,000 and drops by one per tick. The client gets it now and then
 * (1.12.2 sent it every 100 ticks) to redraw the colour.
 */
public class FallenMeteorBlockEntity extends BlockEntity {
    public static final int MAX_HEAT_LEVEL = 5000;
    private static final int SYNC_INTERVAL = 100;

    private int heatLevel = MAX_HEAT_LEVEL;

    public FallenMeteorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.FALLEN_METEOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FallenMeteorBlockEntity meteor) {
        if (meteor.heatLevel > 0) {
            meteor.heatLevel--;
            meteor.setChanged();
            if (meteor.heatLevel % SYNC_INTERVAL == 0) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public int heatLevel() {
        return this.heatLevel;
    }

    public void setHeatLevel(int heatLevel) {
        this.heatLevel = Math.max(0, Math.min(MAX_HEAT_LEVEL, heatLevel));
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public float scaledHeatLevel() {
        return (float) this.heatLevel / MAX_HEAT_LEVEL;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MeteorHeatLevel", this.heatLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.heatLevel = tag.contains("MeteorHeatLevel") ? tag.getInt("MeteorHeatLevel") : MAX_HEAT_LEVEL;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("MeteorHeatLevel", this.heatLevel);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The colour depends on the heat: redraw the block when a new value arrives. */
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (this.level != null && this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
