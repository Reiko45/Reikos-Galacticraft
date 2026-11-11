package io.github.reiko45.galacticraft.block;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 BlockCavernousVine: glowing vines that hang from Mars cavern ceilings. They grow down while the two blocks
 * below are air, entangle living things (every tick they set the victim's motion to 0.06 upwards, which with gravity
 * leaves it hanging, sinking very slowly) and poison them (never fatally), and only shears collect them. The glow fades
 * towards the top of a strand: the lowest vine gives light 15, each one above it 4 less.
 */
public class CavernousVinesBlock extends Block {
    /** The 1.12.2 vinetype (vine_0, vine_1, vine_2), cycling down a strand. */
    public static final IntegerProperty TYPE = IntegerProperty.create("type", 0, 2);
    /**
     * How many vines hang from here down, this one included (5 = five or more). 1.12.2 counted them for every light
     * query; keeping the count in the state lets the light engine read the glow without looking at other blocks.
     */
    public static final IntegerProperty HANGING = IntegerProperty.create("hanging", 1, 5);

    public CavernousVinesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0).setValue(HANGING, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, HANGING);
    }

    /** 1.12.2 getVineLight: 19 less 4 for each vine from here down, so 15 at the tip, 11 above it, ... 0. */
    public static int light(BlockState state) {
        return Math.max(Math.min(19 - state.getValue(HANGING) * 4, 15), 0);
    }

    /** A vine of a strand: its type, and how many vines hang from it down. */
    public BlockState vine(int type, int hanging) {
        return this.defaultBlockState().setValue(TYPE, type % 3).setValue(HANGING, Math.clamp(hanging, 1, 5));
    }

    private int hangingAbove(BlockState below) {
        return below.is(this) ? Math.min(below.getValue(HANGING) + 1, 5) : 1;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        final BlockState above = level.getBlockState(pos.above());
        return above.is(this) || above.isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        final BlockState state = vine(length(context.getLevel(), context.getClickedPos().above()),
                hangingAbove(context.getLevel().getBlockState(context.getClickedPos().below())));
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
            BlockPos neighbourPos) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == Direction.DOWN) {
            // The strand below grew or shrank: recount, which updates the vine above in turn
            return state.setValue(HANGING, hangingAbove(neighbour));
        }
        return state;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.below()) && level.isEmptyBlock(pos.below(2))) {
            level.setBlock(pos.below(), vine(length(level, pos), 1), Block.UPDATE_CLIENTS);
        }
    }

    /** Vines from {@code pos} upwards (1.12.2 getVineLength). */
    private int length(BlockGetter level, BlockPos pos) {
        int count = 0;
        final BlockPos.MutableBlockPos cursor = pos.mutable();
        while (count < 256 && level.getBlockState(cursor).getBlock() == this) {
            count++;
            cursor.move(Direction.UP);
        }
        return count;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        final Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, 0.06D, motion.z);
        entity.setYRot(entity.getYRot() + 0.4F);
        // Amplifier 20 makes poison hurt every tick it lasts; poison never takes the last half heart.
        living.addEffect(new MobEffectInstance(MobEffects.POISON, 5, 20, false, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.galacticraftcore.cavernous_vines.desc").withStyle(ChatFormatting.GRAY));
    }
}
