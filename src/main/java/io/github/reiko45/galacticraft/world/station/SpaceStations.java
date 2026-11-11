package io.github.reiko45.galacticraft.world.station;

import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every space station of the world (1.12.2 SpaceStationWorldData, one file per station, and GCPlayerStats
 * #spaceStationDimensionData). 1.12.2 gave each station its own dimension with the station at 0,64,0; here all Earth
 * stations share the orbit dimension and station {@code id} has its arrival block at ({@code id} x 4096, 64, 0), so
 * each has 2,048 blocks of space around it.
 */
public final class SpaceStations extends SavedData {
    public static final String FILE = "galacticraftcore_space_stations";
    public static final int SPACING = 4096;
    public static final int BASE_Y = 64;

    /** One station (1.12.2 SpaceStationWorldData). */
    public static final class Station {
        private final int id;
        private UUID owner;
        private String ownerName;
        private String name;
        /** Lower-case names of the players allowed in besides the owner (1.12.2 allowedPlayers). */
        private final Set<String> allowed = new LinkedHashSet<>();
        private boolean allowAll;
        private final StationSpin spin;

        Station(int id, UUID owner, String ownerName) {
            this.id = id;
            this.owner = owner;
            this.ownerName = ownerName;
            this.name = "Station: " + ownerName;
            this.spin = new StationSpin(base());
        }

        public StationSpin spin() {
            return this.spin;
        }

        public int id() {
            return this.id;
        }

        public UUID owner() {
            return this.owner;
        }

        public String ownerName() {
            return this.ownerName;
        }

        public String name() {
            return this.name;
        }

        public boolean allowAll() {
            return this.allowAll;
        }

        public Set<String> allowedPlayers() {
            return Collections.unmodifiableSet(this.allowed);
        }

        /** The Space Station Arrival Block. */
        public BlockPos base() {
            return new BlockPos(this.id * SPACING, BASE_Y, 0);
        }

        /** Where arriving players are put. */
        public BlockPos arrival() {
            return base().offset(StationStructure.ARRIVAL_OFFSET);
        }

        public boolean isOwner(Player player) {
            return player.getUUID().equals(this.owner);
        }

        /** 1.12.2: the owner, allowed players, or anyone when the station is open to all. */
        public boolean canVisit(Player player) {
            return isOwner(player) || this.allowAll || this.allowed.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
        }

        CompoundTag save() {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("Id", this.id);
            tag.putUUID("Owner", this.owner);
            tag.putString("OwnerName", this.ownerName);
            tag.putString("Name", this.name);
            tag.putBoolean("AllowAll", this.allowAll);
            final ListTag allowed = new ListTag();
            for (String player : this.allowed) {
                allowed.add(StringTag.valueOf(player));
            }
            tag.put("Allowed", allowed);
            tag.put("Spin", this.spin.save());
            return tag;
        }

        static Station load(CompoundTag tag) {
            final Station station = new Station(tag.getInt("Id"), tag.getUUID("Owner"), tag.getString("OwnerName"));
            station.name = tag.getString("Name");
            station.allowAll = tag.getBoolean("AllowAll");
            for (Tag player : tag.getList("Allowed", Tag.TAG_STRING)) {
                station.allowed.add(player.getAsString());
            }
            if (tag.contains("Spin")) {
                station.spin.load(tag.getCompound("Spin"));
            }
            return station;
        }
    }

    private final List<Station> stations = new ArrayList<>();

    public static SpaceStations get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SpaceStations::new, SpaceStations::load, null), FILE);
    }

    public static SpaceStations load(CompoundTag tag, HolderLookup.Provider registries) {
        final SpaceStations data = new SpaceStations();
        for (Tag station : tag.getList("Stations", Tag.TAG_COMPOUND)) {
            data.stations.add(Station.load((CompoundTag) station));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (Station station : this.stations) {
            list.add(station.save());
        }
        tag.put("Stations", list);
        return tag;
    }

    public List<Station> all() {
        return Collections.unmodifiableList(this.stations);
    }

    @Nullable
    public Station byId(int id) {
        for (Station station : this.stations) {
            if (station.id == id) {
                return station;
            }
        }
        return null;
    }

    /** 1.12.2 allowed one station per player per home planet; Earth is the only home planet so far. */
    @Nullable
    public Station ownedBy(UUID player) {
        for (Station station : this.stations) {
            if (station.owner.equals(player)) {
                return station;
            }
        }
        return null;
    }

    /** The stations a player may fly to. */
    public List<Station> visitableBy(Player player) {
        final List<Station> list = new ArrayList<>();
        for (Station station : this.stations) {
            if (station.canVisit(player)) {
                list.add(station);
            }
        }
        return list;
    }

    /** The station whose region of the orbit dimension {@code pos} is in, if there is one. */
    @Nullable
    public Station at(BlockPos pos) {
        return byId(Math.floorDiv(pos.getX() + SPACING / 2, SPACING));
    }

    /** A new station record (not built yet), with the lowest free id. */
    public Station add(UUID owner, String ownerName) {
        int id = 0;
        while (byId(id) != null) {
            id++;
        }
        final Station station = new Station(id, owner, ownerName);
        this.stations.add(station);
        setDirty();
        return station;
    }

    /** 1.12.2 WorldUtil#bindSpaceStationToNewDimension + ChunkProviderOrbit: a new station, built in orbit. */
    public Station create(ServerLevel orbit, Player owner) {
        final Station station = add(owner.getUUID(), owner.getGameProfile().getName());
        orbit.getChunk(station.base());
        StationStructure.build(orbit, station.base());
        station.spin.refresh(orbit, station.base());
        StationSpin.sync(orbit, station);
        return station;
    }

    public void rename(Station station, String name) {
        station.name = name;
        setDirty();
    }

    public boolean allow(Station station, String player) {
        final boolean added = station.allowed.add(player.toLowerCase(Locale.ROOT));
        setDirty();
        return added;
    }

    public boolean disallow(Station station, String player) {
        final boolean removed = station.allowed.remove(player.toLowerCase(Locale.ROOT));
        setDirty();
        return removed;
    }

    public void setAllowAll(Station station, boolean allowAll) {
        station.allowAll = allowAll;
        setDirty();
    }

    public void setOwner(Station station, UUID owner, String ownerName) {
        station.owner = owner;
        station.ownerName = ownerName;
        setDirty();
    }

    public static boolean isOrbit(ServerLevel level) {
        return level.dimension().equals(GCDimensions.ORBIT);
    }
}
