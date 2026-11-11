package io.github.reiko45.galacticraft.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * Server-side {@link ContainerData} built from getters. Vanilla syncs container data as 16-bit values,
 * so ints (energy can be millions of gJ) take two slots; read them back with {@link #readInt}.
 */
public final class SyncedData implements ContainerData {
    private final List<IntSupplier> slots = new ArrayList<>();

    public SyncedData addShort(IntSupplier getter) {
        this.slots.add(getter);
        return this;
    }

    public SyncedData addInt(IntSupplier getter) {
        this.slots.add(() -> getter.getAsInt() & 0xFFFF);
        this.slots.add(() -> (getter.getAsInt() >>> 16) & 0xFFFF);
        return this;
    }

    @Override
    public int get(int index) {
        return this.slots.get(index).getAsInt();
    }

    @Override
    public void set(int index, int value) {
        // Server-side values are computed; clients use a SimpleContainerData.
    }

    @Override
    public int getCount() {
        return this.slots.size();
    }

    public static ContainerData client(int count) {
        return new SimpleContainerData(count);
    }

    public static int readInt(ContainerData data, int index) {
        return (data.get(index) & 0xFFFF) | ((data.get(index + 1) & 0xFFFF) << 16);
    }

    public static int readShort(ContainerData data, int index) {
        return (short) data.get(index);
    }
}
