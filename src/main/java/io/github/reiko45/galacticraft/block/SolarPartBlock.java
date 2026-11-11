package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible placeholder occupying the space of a solar panel's pole and 3x3 panel (1.12.2 BlockMulti with
 * TileEntityMulti). The main block's renderer draws the panel; breaking any part breaks the whole panel.
 */
public class SolarPartBlock extends Block implements EntityBlock {
    public static final MapCodec<SolarPartBlock> CODEC = simpleCodec(SolarPartBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** 1.12.2 AABB_SOLAR_POLE (0.3-0.7) and AABB_SOLAR (y 0.2-0.8); NASA Workbench parts are full blocks. */
    private static final VoxelShape POLE = Block.box(4.8, 0, 4.8, 11.2, 16, 11.2);
    private static final VoxelShape PANEL = Block.box(0, 3.2, 0, 16, 12.8, 16);

    public enum Part implements StringRepresentable {
        POLE, PANEL, WORKBENCH;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public SolarPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(PART, Part.PANEL));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case POLE -> POLE;
            case PANEL -> PANEL;
            case WORKBENCH -> net.minecraft.world.phys.shapes.Shapes.block();
        };
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPartBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof SolarPartBlockEntity part && part.mainPos() != null) {
            return new ItemStack(level.getBlockState(part.mainPos()).getBlock());
        }
        return ItemStack.EMPTY;
    }

    /** Using a part uses the main block (1.12.2 BlockMulti#onBlockActivated). */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof SolarPartBlockEntity part && part.mainPos() != null) {
            final BlockState main = level.getBlockState(part.mainPos());
            return main.useWithoutItem(level, player, hit.withPosition(part.mainPos()));
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    /** Breaking any part breaks the panel (1.12.2 TileEntityMulti#onBlockRemoval). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SolarPartBlockEntity part) {
            final BlockPos main = part.mainPos();
            if (main != null && (level.getBlockState(main).getBlock() instanceof SolarPanelBlock
                    || level.getBlockState(main).getBlock() instanceof NasaWorkbenchBlock)) {
                level.destroyBlock(main, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
