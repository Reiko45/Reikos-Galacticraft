package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.FluidPipeBlockEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Fluid Pipe (1.12.2 BlockFluidPipe, NORMAL and PULL modes in one block): carries any liquid or gas. Dye it to keep
 * networks apart (pipes join when they share a colour or either is white); a Standard Wrench switches it to pull mode,
 * which draws fluid out of the blocks next to it.
 */
public class FluidPipeBlock extends Block implements EntityBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final BooleanProperty PULL = BooleanProperty.create("pull");

    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        final VoxelShape core = Block.box(5.6, 5.6, 5.6, 10.4, 10.4, 10.4);
        final VoxelShape[] arms = {
                Block.box(5.6, 0, 5.6, 10.4, 5.6, 10.4), Block.box(5.6, 10.4, 5.6, 10.4, 16, 10.4),
                Block.box(5.6, 5.6, 0, 10.4, 10.4, 5.6), Block.box(5.6, 5.6, 10.4, 10.4, 10.4, 16),
                Block.box(0, 5.6, 5.6, 5.6, 10.4, 10.4), Block.box(10.4, 5.6, 5.6, 16, 10.4, 10.4)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, arms[i]);
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public FluidPipeBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(COLOR, DyeColor.WHITE).setValue(PULL, false);
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
        builder.add(COLOR, PULL);
    }

    /** 1.12.2 collision: core 0.35-0.65 plus an arm per connection. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(dir))) {
                mask |= 1 << dir.get3DDataValue();
            }
        }
        return SHAPES[mask];
    }

    public static boolean colorsConnect(DyeColor a, DyeColor b) {
        return a == b || a == DyeColor.WHITE || b == DyeColor.WHITE;
    }

    public static boolean connects(LevelAccessor level, BlockPos pos, BlockState state, Direction direction) {
        final BlockPos neighbor = pos.relative(direction);
        final BlockState other = level.getBlockState(neighbor);
        if (other.getBlock() instanceof FluidPipeBlock) {
            return colorsConnect(state.getValue(COLOR), other.getValue(COLOR));
        }
        return level instanceof Level real && real.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, direction.getOpposite()) != null;
    }

    public BlockState withConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir), connects(level, pos, state, dir));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), connects(level, pos, state, direction));
    }

    /** Dye a pipe (1.12.2: consumes the dye, gives back the previous non-white dye). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye) || dye.getDyeColor() == state.getValue(COLOR)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            final DyeColor previous = state.getValue(COLOR);
            level.setBlockAndUpdate(pos, withConnections(level, pos, state.setValue(COLOR, dye.getDyeColor())));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (previous != DyeColor.WHITE) {
                    Block.popResource(level, pos, new ItemStack(DyeItem.byColor(previous)));
                }
            }
            if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
                pipe.networkChanged();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Standard Wrench: switch between normal and pull mode. */
    public void toggleMode(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(PULL, !state.getValue(PULL)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }
}
