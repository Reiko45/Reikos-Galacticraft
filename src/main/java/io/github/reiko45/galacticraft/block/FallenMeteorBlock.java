package io.github.reiko45.galacticraft.block;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.block.entity.FallenMeteorBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Fallen Meteor (1.12.2 BlockFallenMeteor + TileEntityFallenMeteor). A meteor lands hot: for 5,000 ticks it glows
 * orange, cooling to grey, and anything living that touches it is set on fire and pushed away. It falls like sand
 * (through air, fire, water and lava) and drops raw meteoric iron. A Fallen Meteor placed by hand is already cold.
 */
public class FallenMeteorBlock extends BaseEntityBlock {
    public static final MapCodec<FallenMeteorBlock> CODEC = simpleCodec(FallenMeteorBlock::new);
    /** 1.12.2 BOUNDS (0.15, 0.05, 0.15) - (0.85, 0.75, 0.85). */
    private static final VoxelShape SHAPE = Block.box(2.4D, 0.8D, 2.4D, 13.6D, 12.0D, 13.6D);
    /** 1.12.2 Block#tickRate default. */
    private static final int FALL_DELAY = 10;

    public FallenMeteorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FallenMeteorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GCBlockEntities.FALLEN_METEOR.get(), FallenMeteorBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof FallenMeteorBlockEntity meteor) {
            meteor.setHeatLevel(0);
        }
    }

    /** 1.12.2 onEntityCollision: a hot meteor hisses, smokes, sets fire for 2 seconds and pushes the entity away. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || !(level.getBlockEntity(pos) instanceof FallenMeteorBlockEntity meteor)
                || meteor.heatLevel() <= 0) {
            return;
        }
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D, 8, 0.4D, 0.4D, 0.4D, 0.0D);
        }
        if (!living.isOnFire()) {
            living.igniteForSeconds(2.0F);
        }
        double dx = pos.getX() + 0.5D - living.getX();
        double dz = living.getZ() - pos.getZ();
        while (dx * dx + dz * dz < 1.0E-4D) {
            dx = (Math.random() - Math.random()) * 0.01D;
            dz = (Math.random() - Math.random()) * 0.01D;
        }
        living.knockback(1.0D, dx, dz);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, FALL_DELAY);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, FALL_DELAY);
        return state;
    }

    /** 1.12.2 tryToFall: drop straight to the first block it can rest on, keeping its heat. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canFallInto(level, pos.below()) || pos.getY() <= level.getMinBuildHeight()) {
            return;
        }
        final int heat = level.getBlockEntity(pos) instanceof FallenMeteorBlockEntity meteor ? meteor.heatLevel() : 0;
        level.removeBlock(pos, false);
        BlockPos below = pos.below();
        while (canFallInto(level, below.below()) && below.getY() > level.getMinBuildHeight()) {
            below = below.below();
        }
        level.setBlockAndUpdate(below, state);
        if (level.getBlockEntity(below) instanceof FallenMeteorBlockEntity meteor) {
            meteor.setHeatLevel(heat);
        }
    }

    private static boolean canFallInto(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(net.minecraft.world.level.block.Blocks.FIRE) || state.getFluidState().is(Fluids.WATER)
                || state.getFluidState().is(Fluids.FLOWING_WATER) || state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA)
                || state.is(io.github.reiko45.galacticraft.registry.GCBlocks.BREATHABLE_AIR.get());
    }

    /** 1.12.2 colorMultiplier: (198, 108, 58) when hot, fading to untinted as it cools. */
    public static int tint(@Nullable BlockGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof FallenMeteorBlockEntity meteor)) {
            return 0xFFFFFF;
        }
        final int add = (int) (200.0F - meteor.scaledHeatLevel() * 200.0F);
        final int r = Math.min(255, 198 + add);
        final int g = Math.min(255, 108 + add);
        final int b = Math.min(255, 58 + add);
        return r << 16 | g << 8 | b;
    }
}
