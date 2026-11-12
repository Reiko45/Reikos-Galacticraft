/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityTelepadFake). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.energy.IGCEnergyHandler;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One of the 17 blocks around and above a Short Range Telepad (1.12.2 TileEntityTelepadFake). It remembers the telepad;
 * the one straight above it takes power on its top face and passes it to the telepad (1.12.2 buffered it for a tick).
 */
public class TelepadFakeBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos mainPos;

    public TelepadFakeBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.TELEPAD_FAKE.get(), pos, state);
    }

    public void setMainPos(BlockPos main) {
        this.mainPos = main.immutable();
        setChanged();
    }

    @Nullable
    public BlockPos mainPos() {
        return this.mainPos;
    }

    @Nullable
    public ShortRangeTelepadBlockEntity telepad() {
        return this.mainPos != null && this.level != null && this.level.getBlockEntity(this.mainPos) instanceof ShortRangeTelepadBlockEntity telepad
                ? telepad : null;
    }

    /** 1.12.2 updateConnectable: the plate right above the telepad. */
    public boolean connectable() {
        return this.mainPos != null && this.worldPosition.getX() == this.mainPos.getX() && this.worldPosition.getZ() == this.mainPos.getZ()
                && this.worldPosition.getY() > this.mainPos.getY();
    }

    /** The energy capability: power into the top of the connectable plate goes straight to the telepad. */
    @Nullable
    public IGCEnergyHandler energyHandler(@Nullable Direction side) {
        final ShortRangeTelepadBlockEntity telepad = connectable() && (side == null || side == Direction.UP) ? telepad() : null;
        return telepad == null ? null : telepad.energy().view(true, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.mainPos != null) {
            tag.put("MainBlockPosition", NbtUtils.writeBlockPos(this.mainPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mainPos = NbtUtils.readBlockPos(tag, "MainBlockPosition").orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
