/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.sounds.SoundUpdaterRocket). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.sound;

import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 1.12.2 SoundUpdaterRocket: the engine's roar, silent until the rocket is ignited, its pitch rising through the
 * countdown, fading out between y 200 and 1000 and stopping above that (unless landing). 1.12.2 played it at full volume
 * wherever the rocket was; here it is heard within 96 blocks of the rocket, and at full volume only while riding it.
 */
public class RocketSound extends AbstractTickableSoundInstance {
    private static final Set<RocketEntity> PLAYING = Collections.newSetFromMap(new WeakHashMap<>());
    /** 1.12.2 Constants.OVERWORLD_SKYPROVIDER_STARTHEIGHT. */
    private static final double FADE_START = 200.0D;
    private static final double FADE_END = 1000.0D;

    private final RocketEntity rocket;
    private boolean ignition;

    private RocketSound(RocketEntity rocket) {
        super(GCSounds.ROCKET.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.rocket = rocket;
        this.volume = 0.00001F; // at 0 it would never start
        this.pitch = 0.0F;
        this.looping = true;
        this.delay = 0;
        follow();
    }

    /** Start the sound for a rocket that has none yet (1.12.2 TickHandlerClient, for every noisy entity). */
    public static void ensure(RocketEntity rocket) {
        if (!PLAYING.contains(rocket)) {
            PLAYING.add(rocket);
            Minecraft.getInstance().getSoundManager().play(new RocketSound(rocket));
        }
    }

    private void follow() {
        this.x = this.rocket.getX();
        this.y = this.rocket.getY();
        this.z = this.rocket.getZ();
        final boolean riding = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() == this.rocket;
        this.attenuation = riding ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
        this.relative = riding;
        if (riding) {
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }
    }

    @Override
    public void tick() {
        if (this.rocket.isRemoved()) {
            PLAYING.remove(this.rocket);
            stop();
            return;
        }
        final RocketEntity.Phase phase = this.rocket.phase();
        if (phase == RocketEntity.Phase.IGNITED) {
            if (!this.ignition) {
                this.pitch = 0.0F;
                this.ignition = true;
            }
            if (this.rocket.countdown() < RocketEntity.PRE_LAUNCH_WAIT) {
                this.pitch = Math.min(1.0F, this.pitch + 0.0025F);
            }
        } else {
            this.pitch = 1.0F;
        }
        if (phase.ordinal() >= RocketEntity.Phase.IGNITED.ordinal()) {
            final double height = this.rocket.getY();
            if (height > FADE_END) {
                this.volume = 0.0F;
                if (phase != RocketEntity.Phase.LANDING) {
                    stop(); // and not started again: it stays in PLAYING until the rocket is gone
                    return;
                }
            } else if (height > FADE_START) {
                this.volume = 1.0F - (float) ((height - FADE_START) / (FADE_END - FADE_START));
            } else {
                this.volume = 1.0F;
            }
        } else {
            this.volume = 0.00001F;
        }
        follow();
    }
}
