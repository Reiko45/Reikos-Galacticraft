/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.sounds.MusicTickerGC and ClientProxyCore
 * MUSIC_TYPE_MARS). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.registry.GCSounds;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/**
 * 1.12.2 MusicTickerGC: in every Galacticraft world (the Moon, Mars, the asteroids, Venus and orbit) the space music
 * plays instead of the world's own, every 10 to 20 minutes (12,000 to 24,000 ticks), a menu or the credits excepted.
 */
public final class SpaceMusic {
    private static Music music;

    private SpaceMusic() {
    }

    public static void onSelectMusic(SelectMusicEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen instanceof net.minecraft.client.gui.screens.WinScreen
                || CelestialPhysics.of(minecraft.level.dimension()) == null) {
            return;
        }
        if (music == null) {
            music = new Music(GCSounds.MUSIC_SPACE, 12_000, 24_000, true); // 1.12.2 stopped other music on arrival
        }
        event.setMusic(music);
    }
}
