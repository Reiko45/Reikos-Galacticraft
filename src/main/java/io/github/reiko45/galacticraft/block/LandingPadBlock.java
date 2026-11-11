package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Rocket Launch Pad (1.12.2 BlockLandingPad / BlockLandingPadFull). Nine pads placed in a 3x3 square join into one
 * launch pad: the middle one becomes the CENTRE that rockets stand on and Fuel Loaders feed, the others PARTs. Breaking
 * any of them splits the square back into single pads. A launching rocket takes all nine with it.
 */
public class LandingPadBlock extends Block {
    public static final MapCodec<LandingPadBlock> CODEC = simpleCodec(LandingPadBlock::new);
    public static final EnumProperty<Pad> PAD = EnumProperty.create("pad", Pad.class);
    /** 1.12.2 AABB_PAD (0.1875 high), single or joined. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);

    public enum Pad implements StringRepresentable {
        SINGLE, CENTRE, PART;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public LandingPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(PAD, Pad.SINGLE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PAD);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A pad of the same kind as {@code block} (launch pads and fueling pads never join each other). */
    private static boolean isPad(Level level, BlockPos pos, Pad pad, Block block) {
        final BlockState state = level.getBlockState(pos);
        return state.is(block) && state.getValue(PAD) == pad;
    }

    private static List<BlockPos> square(BlockPos centre) {
        return BlockPos.betweenClosedStream(centre.offset(-1, 0, -1), centre.offset(1, 0, 1)).map(BlockPos::immutable).toList();
    }

    /** 1.12.2 TileEntityLandingPadSingle#update: nine single pads in a square become a full pad. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide || state.getValue(PAD) != Pad.SINGLE || oldState.is(this)) {
            return;
        }
        for (BlockPos centre : square(pos)) {
            if (square(centre).stream().allMatch(p -> isPad(level, p, Pad.SINGLE, this))) {
                for (BlockPos part : square(centre)) {
                    level.setBlock(part, defaultBlockState().setValue(PAD, part.equals(centre) ? Pad.CENTRE : Pad.PART), Block.UPDATE_ALL);
                }
                return;
            }
        }
    }

    /** Breaking a joined pad turns the rest of its square back into single pads. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this) && state.getValue(PAD) != Pad.SINGLE) {
            // The block at pos is already gone here, so look for the centre around it rather than through it.
            BlockPos centre = state.getValue(PAD) == Pad.CENTRE ? pos : null;
            for (BlockPos candidate : square(pos)) {
                if (centre == null && isPad(level, candidate, Pad.CENTRE, this)) {
                    centre = candidate;
                }
            }
            if (centre != null) {
                for (BlockPos part : square(centre)) {
                    if (!part.equals(pos) && level.getBlockState(part).is(this)) {
                        level.setBlock(part, defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The centre of the joined pad that {@code pos} belongs to, or null. */
    @Nullable
    public static BlockPos findCentre(Level level, BlockPos pos) {
        final Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof LandingPadBlock)) {
            return null;
        }
        if (isPad(level, pos, Pad.CENTRE, block)) {
            return pos;
        }
        if (!isPad(level, pos, Pad.PART, block)) {
            return null;
        }
        for (BlockPos candidate : square(pos)) {
            if (isPad(level, candidate, Pad.CENTRE, block)) {
                return candidate;
            }
        }
        return null;
    }

    /** The centre of the joined Rocket Launch Pad at {@code pos} (not a Buggy Fueling Pad), or null. */
    @Nullable
    public static BlockPos findLaunchPadCentre(Level level, BlockPos pos) {
        final BlockPos centre = findCentre(level, pos);
        return centre != null && !(level.getBlockState(centre).getBlock() instanceof BuggyFuelingPadBlock) ? centre : null;
    }
}
