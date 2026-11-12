/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.wrappers.FlagData). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.race;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A team's flag (1.12.2 FlagData): 48 x 32 pixels of 0xRRGGBB colour, (0, 0) at the top left. New flags are white
 * (1.12.2 stored each channel as a byte from -128 and started them at 127, i.e. 255).
 */
public final class FlagData {
    public static final int WIDTH = 48;
    public static final int HEIGHT = 32;
    public static final int WHITE = 0xFFFFFF;

    public static final StreamCodec<ByteBuf, FlagData> STREAM_CODEC = StreamCodec.of((buf, flag) -> {
        ByteBufCodecs.VAR_INT.encode(buf, flag.width);
        ByteBufCodecs.VAR_INT.encode(buf, flag.height);
        for (int pixel : flag.pixels) {
            buf.writeMedium(pixel);
        }
    }, buf -> {
        final int width = Math.max(1, Math.min(256, ByteBufCodecs.VAR_INT.decode(buf)));
        final int height = Math.max(1, Math.min(256, ByteBufCodecs.VAR_INT.decode(buf)));
        final FlagData flag = new FlagData(width, height);
        for (int i = 0; i < flag.pixels.length; i++) {
            flag.pixels[i] = buf.readUnsignedMedium();
        }
        return flag;
    });

    private final int width;
    private final int height;
    private final int[] pixels;

    public FlagData() {
        this(WIDTH, HEIGHT);
    }

    public FlagData(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
        Arrays.fill(this.pixels, WHITE);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /** 0xRRGGBB at (x, y); black outside the flag, as 1.12.2 getColorAt returned. */
    public int colorAt(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return 0;
        }
        return this.pixels[y * this.width + x];
    }

    public void setColorAt(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < this.width && y < this.height) {
            this.pixels[y * this.width + x] = rgb & 0xFFFFFF;
        }
    }

    public FlagData copy() {
        final FlagData copy = new FlagData(this.width, this.height);
        System.arraycopy(this.pixels, 0, copy.pixels, 0, this.pixels.length);
        return copy;
    }

    /** 1.12.2 saveFlagData's compact form: FWidth, FHeight and an int array per row. */
    public void save(CompoundTag tag) {
        tag.putInt("FWidth", this.width);
        tag.putInt("FHeight", this.height);
        for (int y = 0; y < this.height; y++) {
            tag.putIntArray("FRow" + y, Arrays.copyOfRange(this.pixels, y * this.width, (y + 1) * this.width));
        }
    }

    public static FlagData load(CompoundTag tag) {
        if (!tag.contains("FWidth")) {
            return new FlagData();
        }
        final FlagData flag = new FlagData(Math.max(1, tag.getInt("FWidth")), Math.max(1, tag.getInt("FHeight")));
        for (int y = 0; y < flag.height; y++) {
            final int[] row = tag.getIntArray("FRow" + y);
            for (int x = 0; x < Math.min(row.length, flag.width); x++) {
                flag.pixels[y * flag.width + x] = row[x] & 0xFFFFFF;
            }
        }
        return flag;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FlagData other && other.width == this.width && other.height == this.height && Arrays.equals(other.pixels, this.pixels);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.width + this.height) + Arrays.hashCode(this.pixels);
    }
}
