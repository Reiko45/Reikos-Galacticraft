/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.world.gen.WorldGenEggs and
 * EventHandlerMars#onPlanetDecorated). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Slimeling eggs on Mars (1.12.2 WorldGenEggs): two tries a chunk, each at a random point at y 24-127 nudged by up to
 * 7 blocks sideways and 3 up or down; an egg of a random colour goes there if it is air on top of fine regolith.
 */
public class SlimelingEggFeature extends Feature<NoneFeatureConfiguration> {
    public SlimelingEggFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource random = context.random();
        final BlockPos origin = context.origin();
        final BlockPos pos = origin.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4),
                random.nextInt(8) - random.nextInt(8));
        if (!level.isEmptyBlock(pos) || !level.getBlockState(pos.below()).is(GCBlocks.MARS_FINE_REGOLITH.get())) {
            return false;
        }
        final Block egg = switch (random.nextInt(3)) {
            case 0 -> GCBlocks.RED_SLIMELING_EGG.get();
            case 1 -> GCBlocks.BLUE_SLIMELING_EGG.get();
            default -> GCBlocks.YELLOW_SLIMELING_EGG.get();
        };
        level.setBlock(pos, egg.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
}
