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

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(GalacticraftCore.id(name)));
    }

    private GCSounds() {
    }
}
