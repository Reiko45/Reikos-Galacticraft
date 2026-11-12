/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.perlin.NoiseModule and generator.Gradient /
 * generator.Billowed). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.noise;

import java.util.Random;

/**
 * 1.12.2's NoiseModule with its Gradient and Billowed generators: FishyNoise octaves with per-axis frequencies and an
 * amplitude, offset by up to half a unit (plus 0.01) per axis from the seed. Billowed sums absolute values. Unlike
 * 1.12.2 the module is immutable (chunks generate in parallel); callers that changed frequencies between uses pass them
 * in instead.
 */
public final class NoiseModule {
    private final FishyNoise noise;
    private final boolean billowed;
    private final int octaves;
    private final float persistence;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float frequencyX;
    private final float frequencyY;
    private final float frequencyZ;
    private final float amplitude;

    private NoiseModule(long seed, boolean billowed, int octaves, float persistence, float frequencyX, float frequencyY, float frequencyZ,
                        float amplitude) {
        this.billowed = billowed;
        this.octaves = octaves;
        this.persistence = persistence;
        final Random rand = new Random(seed);
        this.offsetX = rand.nextFloat() / 2 + 0.01F;
        this.offsetY = rand.nextFloat() / 2 + 0.01F;
        this.offsetZ = rand.nextFloat() / 2 + 0.01F;
        this.noise = new FishyNoise(seed);
        this.frequencyX = frequencyX;
        this.frequencyY = frequencyY;
        this.frequencyZ = frequencyZ;
        this.amplitude = amplitude;
    }

    /** 1.12.2 Gradient(seed, octaves, persistence) with the given frequencies and amplitude. */
    public static NoiseModule gradient(long seed, int octaves, float persistence, float frequencyX, float frequencyY, float frequencyZ, float amplitude) {
        return new NoiseModule(seed, false, octaves, persistence, frequencyX, frequencyY, frequencyZ, amplitude);
    }

    /** 1.12.2 Billowed(seed, octaves, persistence). */
    public static NoiseModule billowed(long seed, int octaves, float persistence, float frequency, float amplitude) {
        return new NoiseModule(seed, true, octaves, persistence, frequency, frequency, frequency, amplitude);
    }

    private float octave(double value) {
        return this.billowed ? Math.abs((float) value) : (float) value;
    }

    public float get(float i, float j) {
        return get(i, j, this.frequencyX, this.frequencyY);
    }

    /** getNoise(i, j) with the first two frequencies given (1.12.2 changed them between asteroids). */
    public float get(float i, float j, float frequencyI, float frequencyJ) {
        if (this.octaves == 1 && !this.billowed) {
            return (float) this.noise.eval(i * frequencyI + this.offsetX, j * frequencyJ + this.offsetY) * this.amplitude;
        }
        i *= frequencyI;
        j *= frequencyJ;
        float value = 0;
        float curAmplitude = this.amplitude;
        for (int n = 0; n < this.octaves; n++) {
            value += octave(this.noise.eval(i + this.offsetX, j + this.offsetY) * curAmplitude);
            i *= 2;
            j *= 2;
            curAmplitude *= this.persistence;
        }
        return value;
    }

    public float get(float i, float j, float k) {
        if (this.octaves == 1 && !this.billowed) {
            return (float) this.noise.eval(i * this.frequencyX + this.offsetX, j * this.frequencyY + this.offsetY, k * this.frequencyZ + this.offsetZ)
                    * this.amplitude;
        }
        i *= this.frequencyX;
        j *= this.frequencyY;
        k *= this.frequencyZ;
        float value = 0;
        float curAmplitude = this.amplitude;
        for (int n = 0; n < this.octaves; n++) {
            value += octave(this.noise.eval(i + this.offsetX, j + this.offsetY, k + this.offsetZ) * curAmplitude);
            i *= 2;
            j *= 2;
            k *= 2;
            // 1.12.2 Billowed kept the full amplitude for every 3D octave
            curAmplitude *= this.billowed ? 1.0F : this.persistence;
        }
        return value;
    }
}
