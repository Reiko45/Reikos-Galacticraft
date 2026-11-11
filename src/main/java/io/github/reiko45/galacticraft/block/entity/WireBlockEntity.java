package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.energy.network.EnergyNetworkManager;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless marker that tells the network manager when a wire is loaded or removed
 * (1.12.2 TileEntityAluminumWire). Wires do not tick; networks do.
 */
public class WireBlockEntity extends BlockEntity {
    public WireBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.WIRE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).wireAdded(this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).wireRemoved(this.worldPosition);
        }
    }
}
