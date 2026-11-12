/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.blocks.BlockSulphuricAcid and the acid
 * check of event.EventHandlerVenus). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.fluid;

import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;

/**
 * Sulphuric Acid (1.12.2 BlockSulphuricAcid): pools in the Venus valleys and vapor pits. Anything alive in it (up to its
 * shins and more) takes 3 acid damage a second; it drips through the rock under it and now and then bubbles like lava.
 */
public class SulphuricAcidBlock extends LiquidBlock {
    public static final float DAMAGE = 3.0F;

    public SulphuricAcidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    /** EventHandlerVenus#onWorldTick: once a second, if the entity's box shrunk by 0.1 and 0.4 (vertically) is in acid. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity) || entity.tickCount % 20 != 1) {
            return;
        }
        final AABB acid = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D,
                pos.getY() + state.getFluidState().getHeight(level, pos), pos.getZ() + 1.0D);
        if (entity.getBoundingBox().inflate(-0.1D, -0.4D, -0.1D).intersects(acid)) {
            entity.hurt(GCDamageTypes.acid(level), DAMAGE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(1200) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS,
                    random.nextFloat() * 0.25F + 0.75F, 0.00001F + random.nextFloat() * 0.5F, false);
        }
        if (random.nextInt(10) == 0) {
            final BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && !level.getBlockState(pos.below(2)).blocksMotion()) {
                level.addParticle(GCParticles.BACTERIAL_DRIP.get(), pos.getX() + random.nextFloat(), pos.getY() - 1.05D, pos.getZ() + random.nextFloat(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }
}
