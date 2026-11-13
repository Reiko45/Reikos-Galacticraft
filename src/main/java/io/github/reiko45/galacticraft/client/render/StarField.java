/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.SkyProviderMoon#renderStars and the other
 * sky providers' copies of it). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.reiko45.galacticraft.GCConfig;
import java.util.Random;

/**
 * The stars of a Galacticraft sky, built once and rebuilt when the client's moreStars setting changes. With moreStars
 * (on by default, as in 1.12.2) there are more of them, each coordinate pushed out by its own random distance, so they
 * sit at different depths; without it, fewer stars on a sphere.
 */
public final class StarField {
    /**
     * One sky's stars: the count and sphere radius without moreStars, the count and the distance range with it, and the
     * star size range.
     */
    public record Spec(int count, double radius, int moreCount, double moreMin, double moreRange, float minSize, float sizeRange) {
    }

    /** SkyProviderMoon: 6,000 at 100, or 20,000 at 150-250. */
    public static final Spec MOON = new Spec(6000, 100.0D, 20000, 150.0D, 100.0D, 0.15F, 0.1F);
    /** SkyProviderMars and SkyProviderVenus: 6,000 at 100, or 35,000 at 130-280. */
    public static final Spec PLANET = new Spec(6000, 100.0D, 35000, 130.0D, 150.0D, 0.15F, 0.1F);
    /** SkyProviderOrbit: 6,000 close stars at 50, or 20,000 at 75-125. */
    public static final Spec ORBIT = new Spec(6000, 50.0D, 20000, 75.0D, 50.0D, 0.07F, 0.06F);
    /** SkyProviderAsteroids: 6,000 small stars at 80, or 35,000 at 65-140. */
    public static final Spec ASTEROIDS = new Spec(6000, 80.0D, 35000, 65.0D, 75.0D, 0.08F, 0.07F);

    private final Spec spec;
    private VertexBuffer buffer;
    private boolean builtWithMore;

    public StarField(Spec spec) {
        this.spec = spec;
    }

    /** The stars for the current setting. */
    public VertexBuffer get() {
        final boolean more = GCConfig.client(GCConfig.MORE_STARS);
        if (this.buffer == null || more != this.builtWithMore) {
            if (this.buffer != null) {
                this.buffer.close();
            }
            this.buffer = build(this.spec, more);
            this.builtWithMore = more;
        }
        return this.buffer;
    }

    static VertexBuffer build(Spec spec, boolean more) {
        final Random random = new Random(10842L);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        final int count = more ? spec.moreCount() : spec.count();
        for (int i = 0; i < count; ++i) {
            double x = random.nextFloat() * 2.0F - 1.0F;
            double y = random.nextFloat() * 2.0F - 1.0F;
            double z = random.nextFloat() * 2.0F - 1.0F;
            final double size = spec.minSize() + random.nextFloat() * spec.sizeRange();
            double length = x * x + y * y + z * z;
            if (length < 1.0D && length > 0.01D) {
                length = 1.0D / Math.sqrt(length);
                x *= length;
                y *= length;
                z *= length;
                // with moreStars, x, y and z each get their own distance (in that order, before the spin)
                final double px = x * (more ? random.nextDouble() * spec.moreRange() + spec.moreMin() : spec.radius());
                final double py = y * (more ? random.nextDouble() * spec.moreRange() + spec.moreMin() : spec.radius());
                final double pz = z * (more ? random.nextDouble() * spec.moreRange() + spec.moreMin() : spec.radius());
                final double yaw = Math.atan2(x, z);
                final double sinYaw = Math.sin(yaw);
                final double cosYaw = Math.cos(yaw);
                final double pitch = Math.atan2(Math.sqrt(x * x + z * z), y);
                final double sinPitch = Math.sin(pitch);
                final double cosPitch = Math.cos(pitch);
                final double spin = random.nextDouble() * Math.PI * 2.0D;
                final double sinSpin = Math.sin(spin);
                final double cosSpin = Math.cos(spin);
                for (int corner = 0; corner < 4; ++corner) {
                    final double a = ((corner & 2) - 1) * size;
                    final double b = ((corner + 1 & 2) - 1) * size;
                    final double c = a * cosSpin - b * sinSpin;
                    final double d = b * cosSpin + a * sinSpin;
                    final double e = c * sinPitch;
                    final double f = -c * cosPitch;
                    final double dx = f * sinYaw - d * cosYaw;
                    final double dz = d * sinYaw + f * cosYaw;
                    builder.addVertex((float) (px + dx), (float) (py + e), (float) (pz + dz));
                }
            }
        }
        final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
