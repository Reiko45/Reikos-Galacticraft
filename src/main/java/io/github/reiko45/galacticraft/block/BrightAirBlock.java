/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockBrightAir, as ChunkProviderAsteroids
 * placed it). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Air that gives off light: the asteroid generator lines the sunward (west) faces of asteroids with it, brighter the
 * more open the space in front is (1.12.2 set that light level on the block straight after generation). The world has
 * no sky light, so this is the "sunshine".
 */
public class BrightAirBlock extends Block {
    public static final MapCodec<BrightAirBlock> CODEC = simpleCodec(BrightAirBlock::new);
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public BrightAirBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 15));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState state) {
        return net.minecraft.world.level.block.RenderShape.INVISIBLE;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state,
                                                                  net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                                                  net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(LEVEL);
    }
}
