/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.util.ColorUtil). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.util;

import net.minecraft.world.phys.Vec3;

/**
 * 1.12.2 ColorUtil#addColorsRealistically: mixes two colours by their hue on a painter's colour wheel (red, orange,
 * yellow, green, cyan, blue, magenta), so red and yellow make orange rather than a muddy brown. Used for the colour
 * of a bred Slimeling.
 */
public final class ColorUtil {
    private static final Vec3 RED = new Vec3(255, 0, 0);
    private static final Vec3 ORANGE = new Vec3(255, 160, 0);
    private static final Vec3 YELLOW = new Vec3(255, 255, 0);
    private static final Vec3 GREEN = new Vec3(0, 255, 0);
    private static final Vec3 CYAN = new Vec3(0, 255, 255);
    private static final Vec3 BLUE = new Vec3(0, 0, 255);
    private static final Vec3 MAGENTA = new Vec3(255, 0, 255);

    private static final double[] ANGLES = {-110D, -30D, 0D, 60D, 120D, 180D, 215D, 250D, 330D, 360D, 420D, 480D};
    private static final Vec3[] COLORS = {BLUE, MAGENTA, RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, MAGENTA, RED, ORANGE, YELLOW};

    private ColorUtil() {
    }

    /** Both colours and the result in 0-1 components. */
    public static Vec3 addColorsRealistically(Vec3 color1, Vec3 color2) {
        double hue1 = hue(color1);
        double hue2 = hue(color2);
        if (hue1 - hue2 > 180.0D) {
            hue2 += 360.0D;
        }
        if (hue2 - hue1 > 180.0D) {
            hue1 += 360.0D;
        }
        double hue = (hue1 + hue2) / 2.0D;
        if (hue > 360.0D) {
            hue -= 360.0D;
        }
        return rgb(hue).scale(1.0D / 255.0D);
    }

    private static Vec3 rgb(double degrees) {
        degrees %= 360.0D;
        double previous = ANGLES[1];
        for (int i = 2; i < ANGLES.length - 2; i++) {
            final double angle = ANGLES[i];
            if (degrees <= angle) {
                return interpolate(COLORS, i, (angle - degrees) / (angle - previous));
            }
            previous = angle;
        }
        return Vec3.ZERO;
    }

    private static double hue(Vec3 input) {
        final double max = Math.max(Math.max(input.x, input.y), input.z);
        if (max <= 0.0D) {
            return 0.0D;
        }
        final Vec3 rgb = input.scale(255.0D / max);
        double nearest = 1024.0D;
        int index = 0;
        for (int i = 2; i < ANGLES.length - 2; i++) {
            final double separation = COLORS[i].distanceTo(rgb);
            if (separation < nearest) {
                nearest = separation;
                index = i;
            }
        }
        final double before = COLORS[index - 1].distanceTo(rgb);
        final double after = COLORS[index + 1].distanceTo(rgb);
        double hue;
        if (before < after) {
            hue = interpolate(ANGLES, index, nearest / COLORS[index - 1].distanceTo(COLORS[index]));
            if (hue < 0.0D) {
                hue += 360.0D;
            }
        } else {
            hue = interpolate(ANGLES, index + 1, after / COLORS[index + 1].distanceTo(COLORS[index]));
            if (hue > 360.0D) {
                hue -= 360.0D;
            }
        }
        return hue;
    }

    private static double cubic(double y0, double y1, double y2, double y3, double mu) {
        final double mu2 = mu * mu;
        final double a3 = y3 - y2 - y0 + y1;
        final double a2 = y0 - y1 - a3;
        final double a1 = y2 - y0;
        return a3 * mu * mu2 + a2 * mu2 + a1 * mu + y1;
    }

    private static Vec3 interpolate(Vec3[] array, int i, double mu) {
        final Vec3 p0 = array[i + 1];
        final Vec3 p1 = array[i];
        final Vec3 p2 = array[i - 1];
        final Vec3 p3 = array[i - 2];
        return new Vec3(cubic(p0.x, p1.x, p2.x, p3.x, mu), cubic(p0.y, p1.y, p2.y, p3.y, mu), cubic(p0.z, p1.z, p2.z, p3.z, mu));
    }

    private static double interpolate(double[] array, int i, double mu) {
        return cubic(array[i + 1], array[i], array[i - 1], array[i - 2], mu);
    }
}
