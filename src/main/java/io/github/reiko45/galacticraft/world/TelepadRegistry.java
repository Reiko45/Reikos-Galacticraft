/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.dimension.ShortRangeTelepadHandler).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every Short Range Telepad's address, on every world (1.12.2 ShortRangeTelepadHandler): which world and block it is,
 * and whether it is switched on. Addresses are claimed first come, first served.
 */
public final class TelepadRegistry extends SavedData {
    private static final String FILE = GalacticraftCore.MOD_ID + "_short_range_telepads";

    public record Entry(ResourceKey<Level> dimension, BlockPos pos, boolean enabled) {
    }

    private final Map<Integer, Entry> telepads = new HashMap<>();

    public static TelepadRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TelepadRegistry::new, TelepadRegistry::load, null), FILE);
    }

    private static TelepadRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        final TelepadRegistry registry = new TelepadRegistry();
        for (Tag entry : tag.getList("TelepadList", Tag.TAG_COMPOUND)) {
            final CompoundTag telepad = (CompoundTag) entry;
            final ResourceLocation dimension = ResourceLocation.tryParse(telepad.getString("Dimension"));
            if (dimension != null) {
                registry.telepads.put(telepad.getInt("Address"), new Entry(ResourceKey.create(Registries.DIMENSION, dimension),
                        new BlockPos(telepad.getInt("PosX"), telepad.getInt("PosY"), telepad.getInt("PosZ")),
                        !telepad.contains("Enabled") || telepad.getBoolean("Enabled")));
            }
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        this.telepads.forEach((address, entry) -> {
            final CompoundTag telepad = new CompoundTag();
            telepad.putInt("Address", address);
            telepad.putString("Dimension", entry.dimension().location().toString());
            telepad.putInt("PosX", entry.pos().getX());
            telepad.putInt("PosY", entry.pos().getY());
            telepad.putInt("PosZ", entry.pos().getZ());
            telepad.putBoolean("Enabled", entry.enabled());
            list.add(telepad);
        });
        tag.put("TelepadList", list);
        return tag;
    }

    @Nullable
    public Entry get(int address) {
        return this.telepads.get(address);
    }

    /** 1.12.2 addShortRangeTelepad: a telepad with a valid address claims (or keeps) it. */
    public void add(ShortRangeTelepadBlockEntity telepad) {
        if (telepad.addressValid()) {
            final Entry entry = new Entry(telepad.getLevel().dimension(), telepad.getBlockPos().immutable(), !telepad.isDisabled());
            if (!entry.equals(this.telepads.put(telepad.address(), entry))) {
                setDirty();
            }
        }
    }

    /** 1.12.2 removeShortRangeTeleporter. */
    public void remove(ShortRangeTelepadBlockEntity telepad) {
        if (telepad.addressValid() && this.telepads.remove(telepad.address()) != null) {
            setDirty();
        }
    }
}
