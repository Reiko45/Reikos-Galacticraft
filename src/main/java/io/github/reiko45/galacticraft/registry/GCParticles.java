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
    /** 1.12.2 ParticleOxygen as Sensor Glasses show a leak: a pale blue mote swinging out along its motion and back. */
    public static final Supplier<SimpleParticleType> OXYGEN = PARTICLES.register("oxygen", () -> new SimpleParticleType(false));

    /** 1.12.2 "launchFlameLaunched" (ParticleLaunchFlame): a rocket's exhaust once it has lifted off. */
    public static final Supplier<SimpleParticleType> LAUNCH_FLAME = PARTICLES.register("launch_flame", () -> new SimpleParticleType(true));
    /** 1.12.2 "launchFlameIdle": the small flames round a rocket counting down. */
    public static final Supplier<SimpleParticleType> LAUNCH_FLAME_IDLE = PARTICLES.register("launch_flame_idle", () -> new SimpleParticleType(true));
    /** 1.12.2 "whiteSmokeLaunched" / "whiteSmokeLargeLaunched" (ParticleLaunchSmoke): what a launch flame leaves behind. */
    public static final Supplier<SimpleParticleType> LAUNCH_SMOKE = PARTICLES.register("launch_smoke", () -> new SimpleParticleType(true));
    public static final Supplier<SimpleParticleType> LAUNCH_SMOKE_LARGE = PARTICLES.register("launch_smoke_large", () -> new SimpleParticleType(true));
    /** 1.12.2 "whiteSmokeIdle" / "whiteSmokeLargeIdle": the smoke rolling off a rocket on the pad. */
    public static final Supplier<SimpleParticleType> LAUNCH_SMOKE_IDLE = PARTICLES.register("launch_smoke_idle", () -> new SimpleParticleType(true));
    public static final Supplier<SimpleParticleType> LAUNCH_SMOKE_LARGE_IDLE = PARTICLES.register("launch_smoke_large_idle",
            () -> new SimpleParticleType(true));
    /** 1.12.2 ParticleLanderFlame: the lander's blue-white jet. */
    public static final Supplier<SimpleParticleType> LANDER_FLAME = PARTICLES.register("lander_flame", () -> new SimpleParticleType(true));
    /** 1.12.2 "whiteSmokeTiny" (ParticleSmokeSmall): the cold mist falling off a working Gas Liquefier. */
    public static final Supplier<SimpleParticleType> SMALL_SMOKE = PARTICLES.register("small_smoke", () -> new SimpleParticleType(false));

    private GCParticles() {
    }
}
