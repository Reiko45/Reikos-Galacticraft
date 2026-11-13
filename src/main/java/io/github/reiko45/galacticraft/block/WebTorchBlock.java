/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.blocks.BlockTorchWeb). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Web String and its glowing end (1.12.2 BlockTorchWeb, web_torch_0 and web_torch_1): strands of spider silk hanging in
 * the Venus dungeons, one string below another under a solid block, the lowest a glowing bulb (light 15). They have no
 * collision, drop nothing (shears keep them), and fall when what holds them goes.
 */
public class WebTorchBlock extends Block {
    public static final MapCodec<WebTorchBlock> CODEC = simpleCodec(properties -> new WebTorchBlock(properties, false));
    /** 1.12.2 AABB_WEB and AABB_WEB_TORCH. */
    private static final VoxelShape WEB = Block.box(5.6D, 0.0D, 5.6D, 10.4D, 16.0D, 10.4D);
    private static final VoxelShape WEB_TORCH = Block.box(5.6D, 4.0D, 5.6D, 10.4D, 16.0D, 10.4D);

    private final boolean torch;

    public WebTorchBlock(Properties properties, boolean torch) {
        super(properties);
        this.torch = torch;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public boolean isTorch() {
        return this.torch;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.torch ? WEB_TORCH : WEB;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** 1.12.2 canBlockStay: under something solid, or under a plain string. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        final BlockState above = level.getBlockState(pos.above());
        return above.isSolid() || above.is(GCBlocks.WEB_STRING.get());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
}
