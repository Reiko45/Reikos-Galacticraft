package io.github.reiko45.galacticraft.world;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.registry.GCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;

/**
 * Underground crude oil pockets (1.12.2 EventHandlerGC#generateOil, default oil gen factor 1.8).
 *
 * <p>Per chunk: two rolls at 9% each (half that under mountains, 1.8x under oceans and deserts). A pocket is a
 * flattened ellipsoid {@code x^2 + 3y^2 + z^2 <= r^2}, r 3-7, centred at y 17-30 within the chunk. Positions next to
 * air or another liquid, or below sand or gravel, are left alone so the oil stays sealed in.
 */
public class OilPocketFeature extends Feature<NoneFeatureConfiguration> {
    public static final double BASE_CHANCE = Math.min(0.2D, 0.05D * 1.8D);

    public OilPocketFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static double chance(Holder<Biome> biome) {
        double chance = BASE_CHANCE;
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) {
            chance /= 2.0D;
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(Tags.Biomes.IS_DESERT)) {
            chance *= 1.8D;
        }
        return chance;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        final WorldGenLevel level = context.level();
        final RandomSource random = context.random();
        final BlockPos origin = context.origin();
        // 1.12.2 used the surface biome (biomes were 2D); look it up at the terrain height of the chunk centre.
        final int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + 8, origin.getZ() + 8);
        final double chance = chance(level.getBiome(new BlockPos(origin.getX() + 8, surface, origin.getZ() + 8)));
        final boolean first = random.nextDouble() <= chance;
        final boolean second = random.nextDouble() <= chance;
        if (!first && !second) {
            return false;
        }
        // Centre inside this chunk; with r + 1 <= 8 the pocket never reaches past the neighbouring chunks.
        return placePocket(level, new BlockPos(origin.getX() + random.nextInt(16), 17 + random.nextInt(10) + random.nextInt(5),
                origin.getZ() + random.nextInt(16)), 3 + random.nextInt(5));
    }

    public static boolean placePocket(WorldGenLevel level, BlockPos centre, int radius) {
        final BlockState oil = GCFluids.OIL_BLOCK.get().defaultBlockState();
        final int r2 = radius * radius;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int bx = -radius; bx <= radius; bx++) {
            for (int by = -radius + 2; by <= radius - 2; by++) {
                final int xy = bx * bx + by * by * 3;
                for (int bz = -radius; bz <= radius; bz++) {
                    if (xy + bz * bz > r2) {
                        continue;
                    }
                    pos.setWithOffset(centre, bx, by, bz);
                    if (leaks(level, pos.west()) || leaks(level, pos.east()) || leaks(level, pos.below())
                            || leaks(level, pos.north()) || leaks(level, pos.south()) || unstable(level, pos.above())
                            || level.getBlockState(pos).is(BlockTags.FEATURES_CANNOT_REPLACE)) {
                        continue;
                    }
                    level.setBlock(pos, oil, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** 1.12.2 checkBlock: air, or a liquid other than oil. */
    private static boolean leaks(WorldGenLevel level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty() && !state.getFluidState().is(GCFluidTags.OIL);
    }

    /** 1.12.2 checkBlockAbove: sand or gravel would fall into the pocket. */
    private static boolean unstable(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FallingBlock;
    }
}
