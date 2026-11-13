/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.command.CommandGCInv and GCInvSaveData). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.command;

import io.github.reiko45.galacticraft.player.GCPlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 1.12.2 GCInvSaveData: Galacticraft inventories put away by {@code /gcinv save}, by lower-case player name, until
 * {@code /gcinv restore} (or, after a server restart, the player's next login) gives them back.
 */
public final class GCInvData extends SavedData {
    private static final String FILE = "galacticraftcore_gcinv";
    /** 1.12.2 dontload: names saved since the server started, not given back at login until restored (not saved). */
    private static final Set<String> DONT_LOAD = new HashSet<>();

    private final Map<String, NonNullList<ItemStack>> saved = new HashMap<>();

    public static GCInvData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(GCInvData::new, GCInvData::load, null), FILE);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /** Take the player's Galacticraft inventory away into the save. */
    public void save(ServerPlayer player) {
        final ItemStackHandler inventory = GCPlayerData.get(player).inventory();
        final NonNullList<ItemStack> items = NonNullList.withSize(inventory.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getSlots(); i++) {
            items.set(i, inventory.getStackInSlot(i));
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        this.saved.put(key(player.getGameProfile().getName()), items);
        DONT_LOAD.add(key(player.getGameProfile().getName()));
        GCPlayerData.get(player).markGearChanged();
        setDirty();
    }

    public boolean has(String name) {
        return this.saved.containsKey(key(name));
    }

    /** /gcinv restore: allowed back; if the player is here, given back now. @return whether anything was saved */
    public boolean restore(String name, @javax.annotation.Nullable ServerPlayer player) {
        DONT_LOAD.remove(key(name));
        if (!has(name)) {
            return false;
        }
        if (player != null) {
            giveBack(player);
        }
        return true;
    }

    /** 1.12.2 doLoad at login: unless saved since the server started, put the saved inventory back. */
    public void onLogin(ServerPlayer player) {
        final String name = key(player.getGameProfile().getName());
        if (has(name) && !DONT_LOAD.contains(name)) {
            giveBack(player);
        }
    }

    private void giveBack(ServerPlayer player) {
        final NonNullList<ItemStack> items = this.saved.remove(key(player.getGameProfile().getName()));
        final ItemStackHandler inventory = GCPlayerData.get(player).inventory();
        for (int i = 0; i < Math.min(items.size(), inventory.getSlots()); i++) {
            inventory.setStackInSlot(i, items.get(i));
        }
        GCPlayerData.get(player).markGearChanged();
        setDirty();
    }

    private static GCInvData load(CompoundTag tag, HolderLookup.Provider registries) {
        final GCInvData data = new GCInvData();
        for (String name : tag.getAllKeys()) {
            final ListTag list = tag.getList(name, Tag.TAG_COMPOUND);
            int size = 0;
            for (Tag entry : list) {
                size = Math.max(size, ((CompoundTag) entry).getByte("Slot") + 1);
            }
            final NonNullList<ItemStack> items = NonNullList.withSize(Math.max(size, 11), ItemStack.EMPTY);
            for (Tag entry : list) {
                final CompoundTag item = (CompoundTag) entry;
                items.set(item.getByte("Slot"), ItemStack.parseOptional(registries, item));
            }
            data.saved.put(name, items);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        this.saved.forEach((name, items) -> {
            final ListTag list = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isEmpty()) {
                    final CompoundTag item = (CompoundTag) items.get(i).save(registries);
                    item.putByte("Slot", (byte) i);
                    list.add(item);
                }
            }
            tag.put(name, list);
        });
        return tag;
    }
}
