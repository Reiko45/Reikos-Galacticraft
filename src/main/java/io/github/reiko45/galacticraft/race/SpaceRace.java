/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.dimension.SpaceRace). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A Space Race team (1.12.2 SpaceRace): an id, a name, a flag and a team colour, its members (the first one owns it
 * and is the only one who may change it), how long it has been racing (ticks with a member online) and when (in those
 * ticks) it first reached each world, and the schematics waiting for members who were offline when a teammate unlocked
 * them.
 */
public final class SpaceRace {
    /** 1.12.2 SpaceRace.DEFAULT_NAME, shown as "Unnamed Team". */
    public static final String DEFAULT_NAME = "gui.space_race.unnamed";

    /** A member, kept by UUID (1.12.2: by name) with the name last seen. */
    public record Member(UUID id, String name) {
    }

    private final int id;
    private String name = DEFAULT_NAME;
    private FlagData flag = new FlagData();
    /** 0xRRGGBB (1.12.2: a 0-1 vector, white by default). */
    private int color = 0xFFFFFF;
    private final List<Member> members = new ArrayList<>();
    private int ticksSpent;
    private final Map<ResourceLocation, Integer> worldsReached = new LinkedHashMap<>();
    private final Map<UUID, List<String>> pendingSchematics = new LinkedHashMap<>();

    public SpaceRace(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FlagData flag() {
        return this.flag;
    }

    public void setFlag(FlagData flag) {
        this.flag = flag;
    }

    public int color() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public List<Member> members() {
        return this.members;
    }

    @Nullable
    public Member owner() {
        return this.members.isEmpty() ? null : this.members.get(0);
    }

    public boolean isOwner(UUID player) {
        return !this.members.isEmpty() && this.members.get(0).id().equals(player);
    }

    public boolean has(UUID player) {
        return this.members.stream().anyMatch(member -> member.id().equals(player));
    }

    @Nullable
    public Member member(String name) {
        return this.members.stream().filter(member -> member.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public int ticksSpent() {
        return this.ticksSpent;
    }

    public Map<ResourceLocation, Integer> worldsReached() {
        return this.worldsReached;
    }

    /** 1.12.2 tick: time only passes while a member is online. */
    public void tick() {
        this.ticksSpent++;
    }

    /** 1.12.2 setCelestialBodyReached: the first time a member is on a world. */
    public void reached(ResourceLocation world) {
        this.worldsReached.putIfAbsent(world, this.ticksSpent);
    }

    /** 1.12.2 addNewSchematic: for a member who is offline. */
    public void addPendingSchematic(UUID member, String page) {
        this.pendingSchematics.computeIfAbsent(member, id -> new ArrayList<>()).add(page);
    }

    public List<String> takePendingSchematics(UUID member) {
        final List<String> pages = this.pendingSchematics.remove(member);
        return pages == null ? List.of() : pages;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("SpaceRaceID", this.id);
        tag.putString("TeamName", this.name);
        tag.putLong("TicksSpent", this.ticksSpent);
        this.flag.save(tag);
        tag.putInt("TeamColor", this.color);
        final ListTag players = new ListTag();
        for (Member member : this.members) {
            final CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", member.id());
            entry.putString("PlayerName", member.name());
            players.add(entry);
        }
        tag.put("PlayerList", players);
        final ListTag worlds = new ListTag();
        this.worldsReached.forEach((world, time) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("World", world.toString());
            entry.putInt("TimeTaken", time);
            worlds.add(entry);
        });
        tag.put("CelestialBodyList", worlds);
        final ListTag schematics = new ListTag();
        this.pendingSchematics.forEach((member, pages) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putUUID("Mem", member);
            final ListTag list = new ListTag();
            pages.forEach(page -> list.add(StringTag.valueOf(page)));
            entry.put("Sch", list);
            schematics.add(entry);
        });
        tag.put("SchList", schematics);
        return tag;
    }

    public static SpaceRace load(CompoundTag tag) {
        final SpaceRace race = new SpaceRace(tag.getInt("SpaceRaceID"));
        race.name = tag.getString("TeamName");
        race.ticksSpent = (int) tag.getLong("TicksSpent");
        race.flag = FlagData.load(tag);
        race.color = tag.getInt("TeamColor");
        for (Tag entry : tag.getList("PlayerList", Tag.TAG_COMPOUND)) {
            final CompoundTag player = (CompoundTag) entry;
            if (player.hasUUID("Id")) {
                race.members.add(new Member(player.getUUID("Id"), player.getString("PlayerName")));
            }
        }
        for (Tag entry : tag.getList("CelestialBodyList", Tag.TAG_COMPOUND)) {
            final ResourceLocation world = ResourceLocation.tryParse(((CompoundTag) entry).getString("World"));
            if (world != null) {
                race.worldsReached.put(world, ((CompoundTag) entry).getInt("TimeTaken"));
            }
        }
        for (Tag entry : tag.getList("SchList", Tag.TAG_COMPOUND)) {
            final CompoundTag member = (CompoundTag) entry;
            if (member.hasUUID("Mem")) {
                for (Tag page : member.getList("Sch", Tag.TAG_STRING)) {
                    race.addPendingSchematic(member.getUUID("Mem"), page.getAsString());
                }
            }
        }
        return race;
    }
}
