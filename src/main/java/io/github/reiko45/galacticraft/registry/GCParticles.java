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

    /** 1.12.2 "portalBlue" (EntityFXTeleport): rising off a telepad, and falling from its canopy. */
    public static final Supplier<SimpleParticleType> TELEPAD_UP = PARTICLES.register("telepad_up", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> TELEPAD_DOWN = PARTICLES.register("telepad_down", () -> new SimpleParticleType(false));

    /** 1.12.2 "acidVapor" (ParticleAcidVapor): yellow vapor from spouts and the Venus Entry Pod. */
    public static final Supplier<SimpleParticleType> ACID_VAPOR = PARTICLES.register("acid_vapor", () -> new SimpleParticleType(false));
    /** 1.12.2 "acidExhaust" (ParticleAcidExhaust): the puffs over a working Geothermal Generator. */
    public static final Supplier<SimpleParticleType> ACID_EXHAUST = PARTICLES.register("acid_exhaust", () -> new SimpleParticleType(false));

    private GCParticles() {
    }
}
