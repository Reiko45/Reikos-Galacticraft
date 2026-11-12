/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.fx.ParticleAcidVapor). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 1.12.2 ParticleAcidVapor ("acidVapor"): a yellow puff that rises, swelling as it goes and fading slowly, from vapor
 * spouts and from the Venus Entry Pod's exhaust.
 */
public class AcidVaporParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected AcidVaporParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float scale, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.xd = this.xd * 0.1D + dx / 10.0D;
        this.yd = this.yd * 0.1D + dy;
        this.zd = this.zd * 0.1D + dz / 10.0D;
        this.rCol = (float) (Math.random() * 0.1D + 0.8D);
        this.gCol = this.rCol;
        this.bCol = (float) (Math.random() * 0.1D);
        this.alpha = 1.0F;
        this.quadSize *= scale;
        this.baseSize = this.quadSize;
        this.lifetime = (int) ((int) (8.0D / (Math.random() * 0.8D + 0.2D)) * scale);
        this.hasPhysics = true;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** 1.12.2 renderParticle: grows as (age / 11)^2 (at least a tenth of its size). */
    @Override
    public float getQuadSize(float partialTick) {
        final float f = (float) Math.max(Math.pow((this.age + partialTick) / 11.0D, 2.0D), 0.1D);
        return this.baseSize * f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        setSpriteFromAge(this.sprites);
        this.alpha *= 0.994F;
        this.yd += 0.004D;
        move(this.xd, this.yd, this.zd);
        this.yd *= 0.99D;
        this.xd *= 1.001D;
        this.zd *= 1.001D;
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new AcidVaporParticle(level, x, y, z, dx, dy, dz, 0.95F, this.sprites);
        }
    }
}
