/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.dimension.SpaceRaceManager and
 * WorldDataSpaceRaces, and the space race cases of network.PacketSimple and command.CommandJoinSpaceRace). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.race;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every Space Race team on the server (1.12.2 SpaceRaceManager, saved like WorldDataSpaceRaces). A player is in at most
 * one team; opening the Space Race Manager makes one for them. Its owner names it, draws its flag and picks its colour,
 * invites players within 25 blocks (they join with /joinrace) and removes members (who get a team of their own). When a
 * member unlocks a schematic, teammates get it too (offline ones the next time they log in). While a member is online
 * the team's clock runs, and the first time any member is on a world the time is noted.
 */
public final class SpaceRaces extends SavedData {
    private static final String FILE = GalacticraftCore.MOD_ID + "_space_races";
    /** 1.12.2 GuiNewSpaceRace's text box limit. */
    public static final int MAX_NAME_LENGTH = 25;

    private final List<SpaceRace> races = new ArrayList<>();
    /** 1.12.2 kept lastSpaceRaceID in a static, starting again at 1 after every restart. */
    private int nextId = 1;

    public static SpaceRaces get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(SpaceRaces::new, SpaceRaces::load, null), FILE);
    }

    private static SpaceRaces load(CompoundTag tag, HolderLookup.Provider registries) {
        final SpaceRaces races = new SpaceRaces();
        for (Tag entry : tag.getList("SpaceRaceList", Tag.TAG_COMPOUND)) {
            final SpaceRace race = SpaceRace.load((CompoundTag) entry);
            races.races.add(race);
            races.nextId = Math.max(races.nextId, race.id() + 1);
        }
        races.nextId = Math.max(races.nextId, tag.getInt("NextId"));
        return races;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        this.races.forEach(race -> list.add(race.save()));
        tag.put("SpaceRaceList", list);
        tag.putInt("NextId", this.nextId);
        return tag;
    }

    public List<SpaceRace> races() {
        return this.races;
    }

    @Nullable
    public SpaceRace byId(int id) {
        return this.races.stream().filter(race -> race.id() == id).findFirst().orElse(null);
    }

    /** 1.12.2 getSpaceRaceFromPlayer. */
    @Nullable
    public SpaceRace of(UUID player) {
        return this.races.stream().filter(race -> race.has(player)).findFirst().orElse(null);
    }

    private SpaceRace newRace(ServerPlayer player) {
        final SpaceRace race = new SpaceRace(this.nextId++);
        race.members().add(new SpaceRace.Member(player.getUUID(), player.getGameProfile().getName()));
        this.races.add(race);
        setDirty();
        return race;
    }

    /** The player's team, made (unnamed, white flag) if they have none: 1.12.2 did that when the manager first opened. */
    public SpaceRace ensure(ServerPlayer player) {
        final SpaceRace race = of(player.getUUID());
        return race != null ? race : newRace(player);
    }

    // ------------------------------------------------------------------ changes (1.12.2 PacketSimple S_* cases)

    /** S_START_NEW_SPACE_RACE: the owner's new name, flag and colour (the member list stays the server's). */
    public void update(ServerPlayer player, String name, FlagData flag, int color) {
        final SpaceRace race = ensure(player);
        if (!race.isOwner(player.getUUID())) {
            return;
        }
        final String trimmed = name.strip();
        race.setName(trimmed.isEmpty() ? SpaceRace.DEFAULT_NAME : trimmed.substring(0, Math.min(trimmed.length(), MAX_NAME_LENGTH)));
        if (flag.width() == FlagData.WIDTH && flag.height() == FlagData.HEIGHT) {
            race.setFlag(flag);
        }
        race.setColor(color);
        setDirty();
        broadcast(player.server, race);
    }

    /** S_INVITE_RACE_PLAYER: the owner invites an online player within 25 blocks; they are told to use /joinrace. */
    public boolean invite(ServerPlayer owner, String name) {
        final SpaceRace race = of(owner.getUUID());
        final ServerPlayer invited = owner.server.getPlayerList().getPlayerByName(name);
        if (race == null || !race.isOwner(owner.getUUID()) || invited == null || race.has(invited.getUUID())
                || invited.level() != owner.level() || invited.distanceToSqr(owner) > 25.0D * 25.0D) {
            return false;
        }
        GCPlayerData.get(invited).spaceRaceInvite = race.id();
        invited.sendSystemMessage(Component.translatable("gui.galacticraftcore.space_race.chat.invite_received",
                        Component.literal(owner.getGameProfile().getName()).withStyle(ChatFormatting.GREEN))
                .append("  ")
                .append(Component.translatable("gui.galacticraftcore.space_race.chat.to_join", displayName(race).copy().withStyle(ChatFormatting.DARK_PURPLE),
                        Component.literal("/joinrace").withStyle(ChatFormatting.AQUA)))
                .withStyle(ChatFormatting.DARK_AQUA));
        return true;
    }

    /** S_REMOVE_RACE_PLAYER: the owner removes a member, who gets a team of their own (1.12.2 onPlayerRemoval). */
    public boolean remove(ServerPlayer owner, String name) {
        final SpaceRace race = of(owner.getUUID());
        if (race == null || !race.isOwner(owner.getUUID())) {
            return false;
        }
        final SpaceRace.Member member = race.member(name);
        if (member == null || member.id().equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.translatable("gui.galacticraftcore.space_race.chat.not_found", name));
            return false;
        }
        race.members().remove(member);
        final MinecraftServer server = owner.server;
        for (SpaceRace.Member other : race.members()) {
            final ServerPlayer player = server.getPlayerList().getPlayer(other.id());
            if (player != null) {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.space_race.chat.remove_success",
                        Component.literal(member.name()).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        final SpaceRace own = new SpaceRace(this.nextId++);
        own.members().add(member);
        this.races.add(own);
        setDirty();
        broadcast(server, race);
        broadcast(server, own);
        return true;
    }

    /**
     * S_ADD_RACE_PLAYER: the invited player joins (1.12.2 deleted every team the player was in; here they only leave
     * it, and it goes once empty).
     */
    public boolean join(ServerPlayer player, int id) {
        final SpaceRace race = byId(id);
        final GCPlayerData data = GCPlayerData.get(player);
        if (race == null || data.spaceRaceInvite != id) {
            return false;
        }
        if (race.has(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.space_race.chat.already_part").withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        final SpaceRace old = of(player.getUUID());
        if (old != null) {
            old.members().removeIf(member -> member.id().equals(player.getUUID()));
            if (old.members().isEmpty()) {
                this.races.remove(old);
                RaceNetwork.removed(player.server, old.id());
            } else {
                broadcast(player.server, old);
            }
        }
        race.members().add(new SpaceRace.Member(player.getUUID(), player.getGameProfile().getName()));
        data.spaceRaceInvite = 0;
        setDirty();
        broadcast(player.server, race);
        for (SpaceRace.Member member : race.members()) {
            final ServerPlayer online = player.server.getPlayerList().getPlayer(member.id());
            if (online != null) {
                online.sendSystemMessage(Component.translatable("gui.galacticraftcore.space_race.chat.add_success",
                        Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        return true;
    }

    /** 1.12.2 teamUnlockSchematic: teammates get a newly unlocked page, now or when they next log in. */
    public void teamUnlock(ServerPlayer player, String page) {
        final SpaceRace race = of(player.getUUID());
        if (race == null) {
            return;
        }
        for (SpaceRace.Member member : race.members()) {
            if (member.id().equals(player.getUUID())) {
                continue;
            }
            final ServerPlayer online = player.server.getPlayerList().getPlayer(member.id());
            if (online != null) {
                GCPlayerData.get(online).unlockSchematic(page);
            } else {
                race.addPendingSchematic(member.id(), page);
                setDirty();
            }
        }
    }

    /** 1.12.2 ConnectionEvents#onPlayerLogin: the teams, and schematics unlocked while away; the name it now has. */
    public void onLogin(ServerPlayer player) {
        final SpaceRace race = of(player.getUUID());
        if (race != null) {
            final List<String> pages = race.takePendingSchematics(player.getUUID());
            pages.forEach(page -> GCPlayerData.get(player).unlockSchematic(page));
            final String name = player.getGameProfile().getName();
            for (int i = 0; i < race.members().size(); i++) {
                if (race.members().get(i).id().equals(player.getUUID()) && !race.members().get(i).name().equals(name)) {
                    race.members().set(i, new SpaceRace.Member(player.getUUID(), name));
                }
            }
            setDirty();
        }
        for (SpaceRace each : this.races) {
            RaceNetwork.send(player, each);
        }
    }

    /** 1.12.2 SpaceRaceManager#tick: a team's clock runs while a member is online; the first visit to each world. */
    public void tick(MinecraftServer server) {
        for (SpaceRace race : this.races) {
            boolean online = false;
            for (SpaceRace.Member member : race.members()) {
                final ServerPlayer player = server.getPlayerList().getPlayer(member.id());
                if (player != null) {
                    online = true;
                    final Level level = player.level();
                    if (level.dimension() == Level.OVERWORLD || CelestialPhysics.of(level.dimension()) != null) {
                        if (!race.worldsReached().containsKey(level.dimension().location())) {
                            race.reached(level.dimension().location());
                            setDirty();
                        }
                    }
                }
            }
            if (online) {
                race.tick();
                if (race.ticksSpent() % 1200 == 0) {
                    setDirty();
                }
            }
        }
    }

    /** The team's name, or "Unnamed Team" in the reader's language. */
    public static Component displayName(SpaceRace race) {
        return displayName(race.name());
    }

    public static Component displayName(String name) {
        return SpaceRace.DEFAULT_NAME.equals(name) ? Component.translatable("gui.galacticraftcore.space_race.unnamed") : Component.literal(name);
    }

    private static void broadcast(MinecraftServer server, SpaceRace race) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RaceNetwork.send(player, race);
        }
    }
}
