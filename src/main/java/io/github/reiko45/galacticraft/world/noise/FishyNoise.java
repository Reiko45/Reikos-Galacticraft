/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.perlin.FishyNoise).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.noise;

import java.util.Random;

/**
 * Seeded gradient (Perlin-style) noise used by the 1.12.2 planet terrain generators.
 * Ported verbatim so terrain shapes match the original; the deprecated float API was dropped.
 * Instances are immutable after construction and therefore safe to share across worldgen threads.
 */
public final class FishyNoise {
    private static final double[][] GRAD_2D = {
            {1.0D, 0.0D},
            {0.9239000082015991D, 0.38269999623298645D},
            {0.7071070075035095D, 0.7071070075035095D},
            {0.38269999623298645D, 0.9239000082015991D},
            {0.0D, 1.0D},
            {-0.38269999623298645D, 0.9239000082015991D},
            {-0.7071070075035095D, 0.7071070075035095D},
            {-0.9239000082015991D, 0.38269999623298645D},
            {-1.0D, 0.0D},
            {-0.9239000082015991D, -0.38269999623298645D},
            {-0.7071070075035095D, -0.7071070075035095D},
            {-0.38269999623298645D, -0.9239000082015991D},
            {0.0D, -1.0D},
            {0.38269999623298645D, -0.9239000082015991D},
            {0.7071070075035095D, -0.7071070075035095D},
            {0.9239000082015991D, -0.38269999623298645D}
    };

    private static final int[][] GRAD_3I = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, -1, 1}, {0, -1, -1}
    };

    private final int[] perm = new int[512];

    public FishyNoise(long seed) {
        final Random rand = new Random(seed);
        for (int i = 0; i < 256; i++) {
            this.perm[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            final int j = rand.nextInt(256);
            final int tmp = this.perm[i];
            this.perm[i] = this.perm[j];
            this.perm[j] = tmp;
        }
        System.arraycopy(this.perm, 0, this.perm, 256, 256);
    }

    public double eval(double x, double y) {
        int largeX = (x > 0.0D) ? (int) x : ((int) x - 1);
        int largeY = (y > 0.0D) ? (int) y : ((int) y - 1);
        x -= largeX;
        y -= largeY;
        largeX &= 0xFF;
        largeY &= 0xFF;
        final double u = x * x * x * (x * (x * 6.0D - 15.0D) + 10.0D);
        final double v = y * y * y * (y * (y * 6.0D - 15.0D) + 10.0D);
        final int randY = this.perm[largeY] + largeX;
        final int randY1 = this.perm[largeY + 1] + largeX;
        double[] grad2 = GRAD_2D[this.perm[randY] & 0xF];
        final double grad00 = grad2[0] * x + grad2[1] * y;
        grad2 = GRAD_2D[this.perm[randY1] & 0xF];
        final double grad01 = grad2[0] * x + grad2[1] * (y - 1.0D);
        grad2 = GRAD_2D[this.perm[1 + randY1] & 0xF];
        final double grad11 = grad2[0] * (x - 1.0D) + grad2[1] * (y - 1.0D);
        grad2 = GRAD_2D[this.perm[1 + randY] & 0xF];
        final double grad10 = grad2[0] * (x - 1.0D) + grad2[1] * y;
        final double lerpX0 = grad00 + u * (grad10 - grad00);
        return lerpX0 + v * (grad01 + u * (grad11 - grad01) - lerpX0);
    }

    public double eval(double x, double y, double z) {
        int unitX = (x > 0.0D) ? (int) x : ((int) x - 1);
        int unitY = (y > 0.0D) ? (int) y : ((int) y - 1);
        int unitZ = (z > 0.0D) ? (int) z : ((int) z - 1);
        x -= unitX;
        y -= unitY;
        z -= unitZ;
        unitX &= 0xFF;
        unitY &= 0xFF;
        unitZ &= 0xFF;
        final double u = x * x * x * (x * (x * 6.0D - 15.0D) + 10.0D);
        final double v = y * y * y * (y * (y * 6.0D - 15.0D) + 10.0D);
        final double w = z * z * z * (z * (z * 6.0D - 15.0D) + 10.0D);
        final int randZ = this.perm[unitZ] + unitY;
        final int randZ1 = this.perm[unitZ + 1] + unitY;
        final int randYZ = this.perm[randZ] + unitX;
        final int randY1Z = this.perm[1 + randZ] + unitX;
        final int randYZ1 = this.perm[randZ1] + unitX;
        final int randY1Z1 = this.perm[1 + randZ1] + unitX;
        int[] grad3 = GRAD_3I[this.perm[randYZ] & 0xF];
        final double grad000 = grad3[0] * x + grad3[1] * y + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[1 + randYZ] & 0xF];
        final double grad100 = grad3[0] * (x - 1.0D) + grad3[1] * y + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[randY1Z] & 0xF];
        final double grad010 = grad3[0] * x + grad3[1] * (y - 1.0D) + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[1 + randY1Z] & 0xF];
        final double grad110 = grad3[0] * (x - 1.0D) + grad3[1] * (y - 1.0D) + grad3[2] * z;
        z--;
        grad3 = GRAD_3I[this.perm[randYZ1] & 0xF];
        final double grad001 = grad3[0] * x + grad3[1] * y + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[1 + randYZ1] & 0xF];
        final double grad101 = grad3[0] * (x - 1.0D) + grad3[1] * y + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[randY1Z1] & 0xF];
        final double grad011 = grad3[0] * x + grad3[1] * (y - 1.0D) + grad3[2] * z;
        grad3 = GRAD_3I[this.perm[1 + randY1Z1] & 0xF];
        final double grad111 = grad3[0] * (x - 1.0D) + grad3[1] * (y - 1.0D) + grad3[2] * z;
        final double f1 = grad000 + u * (grad100 - grad000);
        final double f2 = grad010 + u * (grad110 - grad010);
        final double f3 = grad001 + u * (grad101 - grad001);
        final double f4 = grad011 + u * (grad111 - grad011);
        final double lerp1 = f1 + v * (f2 - f1);
        return lerp1 + w * (f3 + v * (f4 - f3) - lerp1);
    }
}
