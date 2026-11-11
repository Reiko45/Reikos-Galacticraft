package io.github.reiko45.galacticraft.block;

import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.machine.InputSide;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** An electric machine whose power input face can be moved with sneak + wrench. */
public class ConfigurableMachineBlock extends MachineBlock {
    public static final EnumProperty<InputSide> INPUT = EnumProperty.create("input", InputSide.class);

    public ConfigurableMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends MachineBlockEntity> factory) {
        super(properties, factory);
        registerDefaultState(defaultBlockState().setValue(INPUT, InputSide.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INPUT);
    }

    @Override
    public boolean cycleSides(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(INPUT, state.getValue(INPUT).next()));
        level.invalidateCapabilities(pos);
        return true;
    }
}
