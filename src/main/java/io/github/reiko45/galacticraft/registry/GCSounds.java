package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events (1.12.2 GCSounds; the files are in assets/galacticraftcore/sounds). */
public final class GCSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, GalacticraftCore.MOD_ID);

    public static final Supplier<SoundEvent> BOSS_LAUGH = sound("entity.boss_laugh");
    /** 1.12.2 "block.deconstructor". */
    public static final Supplier<SoundEvent> DECONSTRUCTOR = sound("block.deconstructor");
    public static final Supplier<SoundEvent> LASER_CHARGE = sound("block.laser_turret.charge");
    public static final Supplier<SoundEvent> LASER_SHOOT = sound("block.laser_turret.shoot");
    public static final Supplier<SoundEvent> BOSS_DEATH = sound("entity.boss_death");
    public static final Supplier<SoundEvent> BOSS_OOH = sound("entity.boss_ooh");
    /** The Creeper Boss's hurt sound (1.12.2 bossOuch). */
    public static final Supplier<SoundEvent> BOSS_OUCH = sound("entity.boss_ouch");
    /** The Mars boss spawner's eerie chord (1.12.2 scaryScape). */
    public static final Supplier<SoundEvent> SCARY_SCAPE = sound("ambient.scary_scape");
    /** Sludge dripping from Mars dungeon bricks (1.12.2 singleDrip). */
    public static final Supplier<SoundEvent> SINGLE_DRIP = sound("ambient.single_drip");
    public static final Supplier<SoundEvent> SLIME_DEATH = sound("entity.slime_death");
    /** The Astro Miner's engine (1.12.2 astroMiner). */
    public static final Supplier<SoundEvent> ASTRO_MINER = sound("entity.astro_miner");
    /** An air lock shutting and opening (1.12.2 openAirLock: seals going in; closeAirLock: seals coming out). */
    public static final Supplier<SoundEvent> OPEN_AIR_LOCK = sound("block.air_lock.seal");
    public static final Supplier<SoundEvent> CLOSE_AIR_LOCK = sound("block.air_lock.unseal");
    /** The Advanced Compressor finishing a cycle (1.12.2 advanced_compressor, which plays player/unlockchest). */
    public static final Supplier<SoundEvent> ADVANCED_COMPRESSOR = sound("block.advanced_compressor");
    /** A parachute opening (1.12.2 player.parachute). */
    public static final Supplier<SoundEvent> PARACHUTE = sound("item.parachute.open");
    /** A rocket's engine, from ignition to the edge of space (1.12.2 shuttle.shuttle). */
    public static final Supplier<SoundEvent> ROCKET = SOUNDS.register("entity.rocket.engine",
            () -> SoundEvent.createFixedRangeEvent(GalacticraftCore.id("entity.rocket.engine"), 96.0F));
    /**
     * The music in Galacticraft's worlds (1.12.2 galacticraft.music_space). Only the track whose terms are clear ships
     * with the mod; a resource pack can add the others to this event.
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<SoundEvent, SoundEvent> MUSIC_SPACE = SOUNDS.register("music.space",
            () -> SoundEvent.createVariableRangeEvent(GalacticraftCore.id("music.space")));

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GalacticraftCore.id(name)));
    }

    private GCSounds() {
    }
}
