/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.tile.TileEntitySpout). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1.12.2 TileEntitySpout: one tick in 400, if open to the air above, a puff of 4-7 acid vapor particles. */
public class VaporSpoutBlockEntity extends BlockEntity {
    public VaporSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.VAPOR_SPOUT.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VaporSpoutBlockEntity spout) {
        final RandomSource random = level.getRandom();
        if (random.nextInt(400) != 0 || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        final double x = pos.getX() + 0.45D + random.nextDouble() * 0.1D;
        final double y = pos.getY() + 1.0D;
        final double z = pos.getZ() + 0.45D + random.nextDouble() * 0.1D;
        for (int i = 0; i < 4 + random.nextInt(4); i++) {
            level.addParticle(GCParticles.ACID_VAPOR.get(), x, y, z, random.nextDouble() * 0.5D - 0.25D, random.nextDouble() * 0.5D + 0.5D,
                    random.nextDouble() * 0.5D - 0.25D);
        }
    }
}
