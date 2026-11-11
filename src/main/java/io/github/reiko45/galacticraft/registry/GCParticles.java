package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GCParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, GalacticraftCore.MOD_ID);

    /** 1.12.2 "bacterialDrip" (ParticleDrip): dark green drips seeping through the floor under bacterial sludge. */
    public static final Supplier<SimpleParticleType> BACTERIAL_DRIP = PARTICLES.register("bacterial_drip", () -> new SimpleParticleType(false));

    private GCParticles() {
    }
}
