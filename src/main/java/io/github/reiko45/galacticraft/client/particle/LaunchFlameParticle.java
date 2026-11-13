/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.fx.ParticleLaunchFlame). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

import io.github.reiko45.galacticraft.registry.GCParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 1.12.2 ParticleLaunchFlame: an orange flame, four times the size of a smoke puff once the rocket has launched (a tenth
 * on the pad), moving off exactly as the rocket sent it, slowing, turning yellower and spreading along whatever it lands
 * on. When it burns out it leaves two puffs of smoke (four on the pad). Always fully lit.
 */
public class LaunchFlameParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean launched;
    private final float baseSize;

    protected LaunchFlameParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, boolean launched, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.launched = launched;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.rCol = 1.0F;
        this.gCol = 120.0F / 255.0F + this.random.nextFloat() / 3.0F;
        this.bCol = 55.0F / 255.0F;
        this.quadSize *= launched ? 4.0F : 0.1F;
        this.baseSize = this.quadSize;
        this.hasPhysics = true;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** 1.12.2 renderParticle: full size after a thirty-second of its life. */
    @Override
    public float getQuadSize(float partialTick) {
        return this.baseSize * Mth.clamp((this.age + partialTick) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            smoke(this.launched ? GCParticles.LAUNCH_SMOKE.get() : GCParticles.LAUNCH_SMOKE_IDLE.get());
            smoke(this.launched ? GCParticles.LAUNCH_SMOKE_LARGE.get() : GCParticles.LAUNCH_SMOKE_LARGE_IDLE.get());
            if (!this.launched) {
                smoke(GCParticles.LAUNCH_SMOKE_IDLE.get());
                smoke(GCParticles.LAUNCH_SMOKE_LARGE_IDLE.get());
            }
            remove();
            return;
        }
        setSpriteFromAge(this.sprites);
        this.yd += 0.001D;
        move(this.xd, this.yd, this.zd);
        this.gCol = Math.min(1.0F, this.gCol + 0.01F);
        if (this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
    }

    /** Up to two blocks above where the flame died, carrying on as it went. */
    private void smoke(SimpleParticleType type) {
        Minecraft.getInstance().particleEngine.createParticle(type, this.x, this.y + this.random.nextDouble() * 2.0D, this.z, this.xd, this.yd, this.zd);
    }

    public record Provider(SpriteSet sprites, boolean launched) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new LaunchFlameParticle(level, x, y, z, dx, dy, dz, this.launched, this.sprites);
        }
    }
}
