/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.sounds.SoundUpdaterMiner and
 * the IEntityNoisy handling in core.tick.TickHandlerClient). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.sound;

import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 1.12.2 SoundUpdaterMiner: an Astro Miner's engine, a low hum (volume 0.6, pitch 0.1) while docked or docking and full
 * (1.0, 1.0) while it works, easing between the two; it stops when the miner is stuck, offline or gone.
 */
public class AstroMinerSound extends AbstractTickableSoundInstance {
    private static final Set<AstroMinerEntity> PLAYING = Collections.newSetFromMap(new WeakHashMap<>());

    private final AstroMinerEntity miner;
    private float targetVolume = 0.6F;
    private float targetPitch = 1.0F;

    private AstroMinerSound(AstroMinerEntity miner) {
        super(GCSounds.ASTRO_MINER.get(), SoundSource.AMBIENT, RandomSource.create());
        this.miner = miner;
        this.volume = 0.00001F;
        this.pitch = 1.0F;
        this.looping = true;
        this.delay = 0;
        follow();
    }

    /** Start the sound for a miner that is docked or working, if it has none yet. */
    public static void ensure(AstroMinerEntity miner) {
        if (miner.syncedState() >= AstroMinerEntity.AISTATE_ATBASE && !PLAYING.contains(miner)) {
            PLAYING.add(miner);
            Minecraft.getInstance().getSoundManager().play(new AstroMinerSound(miner));
        }
    }

    private void follow() {
        this.x = this.miner.getX();
        this.y = this.miner.getY();
        this.z = this.miner.getZ();
    }

    @Override
    public void tick() {
        final int state = this.miner.syncedState();
        if (this.miner.isRemoved() || state < AstroMinerEntity.AISTATE_ATBASE) {
            PLAYING.remove(this.miner);
            stop();
            return;
        }
        if (state == AstroMinerEntity.AISTATE_ATBASE || state == AstroMinerEntity.AISTATE_DOCKING) {
            this.targetVolume = 0.6F;
            this.targetPitch = 0.1F;
        } else {
            this.targetVolume = 1.0F;
            this.targetPitch = 1.0F;
        }
        this.volume = this.volume < this.targetVolume ? Math.min(this.targetVolume, this.volume + 0.1F) : Math.max(this.targetVolume, this.volume - 0.1F);
        this.pitch = this.pitch < this.targetPitch ? Math.min(this.targetPitch, this.pitch + 0.05F) : Math.max(this.targetPitch, this.pitch - 0.05F);
        follow();
    }
}
