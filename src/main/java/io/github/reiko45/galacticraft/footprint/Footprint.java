package io.github.reiko45.galacticraft.footprint;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One footprint (1.12.2 wrappers/Footprint): where it lies, which way it points and how old it is. Footprints fade
 * out and vanish after {@link #MAX_AGE} ticks.
 */
public final class Footprint {
    public static final short MAX_AGE = 3200;
    public static final StreamCodec<ByteBuf, Footprint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Footprint::x, ByteBufCodecs.DOUBLE, Footprint::y, ByteBufCodecs.DOUBLE, Footprint::z,
            ByteBufCodecs.FLOAT, Footprint::rotation, ByteBufCodecs.SHORT, Footprint::age, Footprint::new);

    private final double x;
    private final double y;
    private final double z;
    private final float rotation;
    private short age;

    public Footprint(double x, double y, double z, float rotation, short age) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.age = age;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public float rotation() {
        return this.rotation;
    }

    public short age() {
        return this.age;
    }

    /** @return true when the footprint is gone */
    public boolean grow(int ticks) {
        this.age = (short) Math.min(MAX_AGE, this.age + ticks);
        return this.age >= MAX_AGE;
    }
}
