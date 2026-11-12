package io.github.reiko45.galacticraft.world.noise;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * Pre-1.13 octave noise, as 1.12.2 terrain generators sampled it (vanilla NoiseGeneratorOctaves, which Galacticraft's
 * planets used): octave {@code i} is sampled at {@code scale / 2^i} and weighted {@code 2^i}, so the last octaves are
 * the broad, strong ones. Each octave is a 1.21 {@link ImprovedNoise}, the same improved Perlin noise.
 *
 * <p>One difference from 1.12.2: its 3D sampler reused a column's corner gradients for every sample in the same integer
 * y cell (an optimisation that also ignored the fractional y between them); here each sample is exact.
 */
public final class LegacyOctaves {
    private final ImprovedNoise[] octaves;

    public LegacyOctaves(RandomSource random, int count) {
        this.octaves = new ImprovedNoise[count];
        for (int i = 0; i < count; i++) {
            this.octaves[i] = new ImprovedNoise(random);
        }
    }

    /**
     * 1.12.2 generateNoiseOctaves(x, y, z, xSize, ySize, zSize, xScale, yScale, zScale): a grid of samples starting at
     * cell (x, y, z), indexed {@code (x * zSize + z) * ySize + y}.
     */
    public double[] sample(int x, int y, int z, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
        final double[] out = new double[xSize * ySize * zSize];
        double weight = 1.0D;
        for (ImprovedNoise noise : this.octaves) {
            final double sx = xScale * weight;
            final double sy = yScale * weight;
            final double sz = zScale * weight;
            final double ox = wrap(x * sx);
            final double oy = y * sy;
            final double oz = wrap(z * sz);
            int index = 0;
            for (int i = 0; i < xSize; i++) {
                for (int k = 0; k < zSize; k++) {
                    for (int j = 0; j < ySize; j++) {
                        out[index++] += noise.noise(ox + i * sx, oy + j * sy, oz + k * sz) / weight;
                    }
                }
            }
            weight /= 2.0D;
        }
        return out;
    }

    /**
     * 1.12.2 generateNoiseOctaves(x, z, xSize, zSize, xScale, zScale, unused): one layer of samples, which 1.12.2 took
     * from a flat 2D slice of each octave; indexed {@code x * zSize + z}.
     */
    public double[] sample2d(int x, int z, int xSize, int zSize, double xScale, double zScale) {
        final double[] out = new double[xSize * zSize];
        double weight = 1.0D;
        for (ImprovedNoise noise : this.octaves) {
            final double sx = xScale * weight;
            final double sz = zScale * weight;
            final double ox = wrap(x * sx);
            final double oz = wrap(z * sz);
            // y + the octave's own y offset = 0: the integer y = 0 slice, as 1.12.2's 2D path used
            final double flat = -noise.yo;
            int index = 0;
            for (int i = 0; i < xSize; i++) {
                for (int k = 0; k < zSize; k++) {
                    out[index++] += noise.noise(ox + i * sx, flat, oz + k * sz) / weight;
                }
            }
            weight /= 2.0D;
        }
        return out;
    }

    /** 1.12.2 kept the integer part of x and z within 2^24 to hold on to precision far from the origin. */
    private static double wrap(double value) {
        final long whole = Mth.lfloor(value);
        return value - whole + whole % 16777216L;
    }
}
