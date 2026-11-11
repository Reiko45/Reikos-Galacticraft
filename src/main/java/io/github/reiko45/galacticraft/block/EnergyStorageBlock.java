package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import io.github.reiko45.galacticraft.machine.StorageSides;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Energy Storage Module / Cluster: front and back show the charge level in 17 steps (1.12.2 textures 0-16); the input
 * and output faces can be moved with sneak + wrench.
 */
public class EnergyStorageBlock extends MachineBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 16);
    public static final EnumProperty<StorageSides> SIDES = EnumProperty.create("sides", StorageSides.class);

    public EnergyStorageBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0).setValue(SIDES, StorageSides.RIGHT_LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL, SIDES);
    }

    @Override
    public boolean cycleSides(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(SIDES, state.getValue(SIDES).next()));
        level.invalidateCapabilities(pos);
        return true;
    }
}
