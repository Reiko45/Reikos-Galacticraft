package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Remembers which solar panel a part belongs to (1.12.2 TileEntityMulti#mainBlockPosition). */
public class SolarPartBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos mainPos;

    public SolarPartBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.SOLAR_PART.get(), pos, state);
    }

    @Nullable
    public BlockPos mainPos() {
        return this.mainPos;
    }

    public void setMainPos(BlockPos mainPos) {
        this.mainPos = mainPos.immutable();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.mainPos != null) {
            tag.put("MainPos", NbtUtils.writeBlockPos(this.mainPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mainPos = NbtUtils.readBlockPos(tag, "MainPos").orElse(null);
    }
}
