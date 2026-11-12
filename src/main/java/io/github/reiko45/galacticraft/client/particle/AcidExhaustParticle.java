/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.fx.ParticleAcidExhaust). Copyright
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
import net.minecraft.util.Mth;

/**
 * 1.12.2 ParticleAcidExhaust ("acidExhaust"): the yellow puffs over a working Geothermal Generator. Like the vapor of a
 * spout, but it lives five times as long, swells to full size in its first thirtieth, and slows down as it rises,
 * spreading out when it hits something above.
 */
public class AcidExhaustParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected AcidExhaustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float scale, SpriteSet sprites) {
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
        this.lifetime = (int) ((int) (8.0D / (Math.random() * 0.8D + 0.2D)) * scale * 5.0F);
        this.hasPhysics = true;
        this.gravity = 0.0F;
        this.friction = 1.0F;
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
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        setSpriteFromAge(this.sprites);
        this.yd += 0.004D;
        move(this.xd, this.yd, this.zd);
        if (this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new AcidExhaustParticle(level, x, y, z, dx, dy, dz, 0.5F, this.sprites);
        }
    }
}
