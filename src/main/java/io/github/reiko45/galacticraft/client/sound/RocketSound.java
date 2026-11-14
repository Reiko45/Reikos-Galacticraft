/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.sounds.SoundUpdaterRocket). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.sound;

import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12.2 SoundUpdaterRocket: the engine's roar, silent until the rocket is ignited, its pitch rising through the
 * countdown, fading out between y 200 and 1000 and stopping above that (unless landing). 1.12.2 played it at full volume
 * wherever the rocket was; here it is heard within 96 blocks of the rocket (sounds.json), and at full volume while
 * riding it: then it is kept where the camera is.
 * <p>
 * Minecraft fixes a sound's attenuation and whether it is placed relative to the listener when it starts playing, so
 * the sound always stays in world coordinates and only its position moves.
 */
public class RocketSound extends AbstractTickableSoundInstance {
    /** Each rocket's sound, while it should be playing. */
    private static final Map<RocketEntity, RocketSound> PLAYING = new WeakHashMap<>();
    /** 1.12.2 Constants.OVERWORLD_SKYPROVIDER_STARTHEIGHT. */
    private static final double FADE_START = 200.0D;
    private static final double FADE_END = 1000.0D;

    private final RocketEntity rocket;
    private boolean ignition;
    /** Stopped for good (out of the atmosphere): not started again for this rocket. */
    private boolean finished;
    private int startedAt;

    private RocketSound(RocketEntity rocket) {
        super(GCSounds.ROCKET.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.rocket = rocket;
        this.volume = 0.00001F; // at 0 it would never start
        this.pitch = 0.0F;
        this.looping = true;
        this.delay = 0;
        this.startedAt = rocket.tickCount;
        follow();
    }

    /**
     * Start the sound for a rocket that has none (1.12.2 TickHandlerClient, for every noisy entity), or start it again if
     * the sound engine dropped it (a reload, too many sounds at once); at most once a second.
     */
    public static void ensure(RocketEntity rocket) {
        final RocketSound sound = PLAYING.get(rocket);
        if (sound != null && (sound.finished || Minecraft.getInstance().getSoundManager().isActive(sound)
                || rocket.tickCount - sound.startedAt < 20)) {
            return;
        }
        final RocketSound fresh = new RocketSound(rocket);
        if (sound != null) {
            fresh.ignition = sound.ignition;
            fresh.pitch = sound.pitch;
        }
        PLAYING.put(rocket, fresh);
        Minecraft.getInstance().getSoundManager().play(fresh);
    }

    /**
     * Whether the rocket is being heard at full volume where the camera is (for the screenshot tour's check): playing,
     * loud, placed in the world (not relative to the listener) and no more than a tick's flight from the camera.
     */
    public static boolean audible(RocketEntity rocket) {
        final RocketSound sound = PLAYING.get(rocket);
        if (sound == null || sound.finished || sound.volume < 0.5F || sound.isRelative() || !Minecraft.getInstance().getSoundManager().isActive(sound)) {
            return false;
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < 36.0D;
    }

    /** At the rocket, or at the camera while riding it, so the rider hears it at full volume. */
    private void follow() {
        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 at = minecraft.player != null && minecraft.player.getVehicle() == this.rocket
                ? minecraft.gameRenderer.getMainCamera().getPosition() : this.rocket.position();
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
    }

    @Override
    public void tick() {
        if (this.rocket.isRemoved()) {
            this.finished = true;
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
                    this.finished = true;
                    stop();
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
