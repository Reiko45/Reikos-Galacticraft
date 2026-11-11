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
    public static final Supplier<SoundEvent> BOSS_DEATH = sound("entity.boss_death");
    public static final Supplier<SoundEvent> BOSS_OOH = sound("entity.boss_ooh");
    /** The Creeper Boss's hurt sound (1.12.2 bossOuch). */
    public static final Supplier<SoundEvent> BOSS_OUCH = sound("entity.boss_ouch");
    /** The Mars boss spawner's eerie chord (1.12.2 scaryScape). */
    public static final Supplier<SoundEvent> SCARY_SCAPE = sound("ambient.scary_scape");
    /** Sludge dripping from Mars dungeon bricks (1.12.2 singleDrip). */
    public static final Supplier<SoundEvent> SINGLE_DRIP = sound("ambient.single_drip");
    public static final Supplier<SoundEvent> SLIME_DEATH = sound("entity.slime_death");

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GalacticraftCore.id(name)));
    }

    private GCSounds() {
    }
}
