package io.github.reiko45.galacticraft.client.race;

import io.github.reiko45.galacticraft.race.RaceNetwork;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * The Space Race teams as the client knows them (1.12.2 kept them in the same SpaceRaceManager on both sides). The
 * server sends every team on login and whenever one changes.
 */
public final class ClientSpaceRaces {
    private static final Map<Integer, RaceNetwork.RaceData> RACES = new LinkedHashMap<>();
    /** 1.12.2 TickHandlerClient.spaceRaceGuiScheduled: the popup waits for the current screen to close. */
    private static boolean managerScheduled;

    private ClientSpaceRaces() {
    }

    public static void update(RaceNetwork.RaceData race) {
        RACES.put(race.id(), race);
    }

    public static void remove(int id) {
        RACES.remove(id);
    }

    public static void reset() {
        RACES.clear();
        managerScheduled = false;
    }

    public static Collection<RaceNetwork.RaceData> all() {
        return RACES.values();
    }

    @Nullable
    public static RaceNetwork.RaceData byId(int id) {
        return RACES.get(id);
    }

    /** 1.12.2 getSpaceRaceFromPlayer. */
    @Nullable
    public static RaceNetwork.RaceData of(UUID player) {
        for (RaceNetwork.RaceData race : RACES.values()) {
            if (race.memberIds().contains(player)) {
                return race;
            }
        }
        return null;
    }

    /** 1.12.2 ClientUtil#updateTeamColor: the player's team colour, white without a team. */
    public static int teamColor(UUID player) {
        final RaceNetwork.RaceData race = of(player);
        return race == null ? 0xFFFFFF : race.color();
    }

    /** C_OPEN_JOIN_RACE_GUI (a team) or C_OPEN_SPACE_RACE_GUI (0). */
    public static void open(int joinId) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (joinId > 0) {
            minecraft.setScreen(new JoinSpaceRaceScreen(joinId));
        } else if (minecraft.screen == null) {
            minecraft.setScreen(new SpaceRaceManagerScreen());
        } else {
            managerScheduled = true;
        }
    }

    /** Called every client tick: a scheduled popup opens once nothing else is on screen. */
    public static void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (managerScheduled && minecraft.screen == null && minecraft.player != null) {
            managerScheduled = false;
            minecraft.setScreen(new SpaceRaceManagerScreen());
        }
    }
}
