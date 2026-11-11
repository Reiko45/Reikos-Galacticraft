/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.perlin.generator.GradientNoise).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.noise;

import java.util.Random;

/**
 * Fractal (octave-summed) {@link FishyNoise}. Unlike the 1.12.2 class the frequency is fixed at
 * construction time, which makes instances immutable and safe for parallel chunk generation.
 */
public final class GradientNoise {
    private final FishyNoise noise;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final int octaves;
    private final double persistence;
    private final double frequency;
    private final double amplitude;

    public GradientNoise(long seed, int octaves, double persistence, double frequency) {
        this(seed, octaves, persistence, frequency, 1.0D);
    }

    public GradientNoise(long seed, int octaves, double persistence, double frequency, double amplitude) {
        this.octaves = octaves;
        this.persistence = persistence;
        this.frequency = frequency;
        this.amplitude = amplitude;
        final Random rand = new Random(seed);
        this.offsetX = rand.nextFloat() / 2.0F;
        this.offsetY = rand.nextFloat() / 2.0F;
        this.offsetZ = rand.nextFloat() / 2.0F;
        this.noise = new FishyNoise(seed);
    }

    public double eval(double x, double z) {
        if (this.octaves == 1) {
            return this.noise.eval(x * this.frequency + this.offsetX, z * this.frequency + this.offsetY) * this.amplitude;
        }
        x *= this.frequency;
        z *= this.frequency;
        double value = 0.0D;
        double curAmplitude = this.amplitude;
        for (int n = 0; n < this.octaves; n++) {
            value += this.noise.eval(x + this.offsetX, z + this.offsetY) * curAmplitude;
            x *= 2.0D;
            z *= 2.0D;
            curAmplitude *= this.persistence;
        }
        return value;
    }

    public double eval(double x, double y, double z) {
        if (this.octaves == 1) {
            return this.noise.eval(x * this.frequency + this.offsetX, y * this.frequency + this.offsetY,
                    z * this.frequency + this.offsetZ) * this.amplitude;
        }
        x *= this.frequency;
        y *= this.frequency;
        z *= this.frequency;
        double value = 0.0D;
        double curAmplitude = this.amplitude;
        for (int n = 0; n < this.octaves; n++) {
            value += this.noise.eval(x + this.offsetX, y + this.offsetY, z + this.offsetZ) * curAmplitude;
            x *= 2.0D;
            y *= 2.0D;
            z *= 2.0D;
            curAmplitude *= this.persistence;
        }
        return value;
    }
}
