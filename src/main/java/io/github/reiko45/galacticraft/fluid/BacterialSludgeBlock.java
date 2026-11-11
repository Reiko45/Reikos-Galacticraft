/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.blocks.BlockSludge). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.fluid;

import io.github.reiko45.galacticraft.entity.SludgelingEntity;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;

/**
 * Bacterial Sludge (1.12.2 BlockSludge), pooled on the floors of Mars caverns. Anything that wades into it (except a
 * flying player or a sludgeling) stirs up a Sludgeling, until three of them are within five blocks. It drips through
 * the block under it into any space below, and now and then bubbles like lava.
 *
 * <p>1.12.2 played the bubbling for a null player on the client, which plays nothing; here it is heard.
 */
public class BacterialSludgeBlock extends LiquidBlock {
    public static final int SLUDGELING_CAP = 3;
    public static final int SLUDGELING_RANGE = 5;

    public BacterialSludgeBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || entity instanceof SludgelingEntity || entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        if (level.getEntitiesOfClass(SludgelingEntity.class, new AABB(pos).inflate(SLUDGELING_RANGE)).size() < SLUDGELING_CAP) {
            final SludgelingEntity sludgeling = GCEntities.SLUDGELING.get().create(level);
            if (sludgeling != null) {
                final RandomSource random = level.getRandom();
                sludgeling.moveTo(pos.getX() + random.nextInt(5) - 2 + 0.5D, pos.getY(), pos.getZ() + random.nextInt(5) - 2 + 0.5D,
                        random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(sludgeling);
            }
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
