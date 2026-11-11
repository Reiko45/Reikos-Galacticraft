package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.WireBlockEntity;
import io.github.reiko45.galacticraft.energy.GCEnergy;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Aluminum Wire and Heavy Aluminum Wire. Connects to other wires and to any block exposing Galacticraft
 * energy or FE on the facing side. Power flow is handled by {@link io.github.reiko45.galacticraft.energy.network.EnergyNetworkManager}.
 * 1.12.2 used one model per connection combination (alu_wire_n, alu_wire_ne, ...); here a multipart model of
 * a core plus one arm per connected side gives the same geometry.
 */
public class AluminumWireBlock extends Block implements EntityBlock {
    public static final MapCodec<AluminumWireBlock> CODEC = simpleCodec(AluminumWireBlock::new);
    public static final Map<Direction, BooleanProperty> CONNECTIONS = PipeBlock.PROPERTY_BY_DIRECTION;

    private final VoxelShape[] shapes = new VoxelShape[64];
    private final int tier;

    /** Thin wire: 6.5..9.5 (3/16 thick); heavy wire: 5.6..10.4, from the 1.12.2 models. */
    public AluminumWireBlock(Properties properties) {
        this(properties, false);
    }

    public AluminumWireBlock(Properties properties, boolean heavy) {
        super(properties);
        this.tier = heavy ? 2 : 1;
        final double min = heavy ? 5.6D : 6.5D;
        final double max = heavy ? 10.4D : 9.5D;
        final VoxelShape core = Block.box(min, min, min, max, max, max);
        final VoxelShape[] arms = {
                Block.box(min, 0, min, max, min, max),   // down
                Block.box(min, max, min, max, 16, max),  // up
                Block.box(min, min, 0, max, max, min),   // north
                Block.box(min, min, max, max, max, 16),  // south
                Block.box(0, min, min, min, max, max),   // west
                Block.box(max, min, min, 16, max, max),  // east
        };
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    shape = Shapes.or(shape, arms[dir.get3DDataValue()]);
                }
            }
            this.shapes[mask] = shape.optimize();
        }
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    /** 1.12.2 wire tier: 1 for Aluminum Wire, 2 for Heavy Aluminum Wire. A network delivers at most its weakest wire's tier. */
    public int tier() {
        return this.tier;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(dir))) {
                mask |= 1 << dir.get3DDataValue();
            }
        }
        return this.shapes[mask];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir), connects(context.getLevel(), context.getClickedPos(), dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), connects(level, pos, direction));
    }

    /** Whether this wire currently carries power (switchable wires stop when powered by redstone). */
    public boolean isConductive(BlockState state) {
        return true;
    }

    public static boolean connects(LevelAccessor level, BlockPos pos, Direction direction) {
        final BlockPos neighbor = pos.relative(direction);
        final BlockState neighborState = level.getBlockState(neighbor);
        if (neighborState.getBlock() instanceof AluminumWireBlock wire) {
            return wire.isConductive(neighborState);
        }
        if (level instanceof Level real) {
            final Direction face = direction.getOpposite();
            return real.getCapability(GCEnergy.BLOCK, neighbor, face) != null
                    || real.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, face) != null;
        }
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }
}
