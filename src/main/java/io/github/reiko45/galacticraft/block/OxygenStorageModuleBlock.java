package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Oxygen Storage Module block: SIDES holds the oxygen input/output faces (sneak + wrench cycles them). */
public class OxygenStorageModuleBlock extends SidedMachineBlock {
    public OxygenStorageModuleBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
    }
}
