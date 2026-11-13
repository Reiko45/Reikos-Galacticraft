/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.fx.ParticleSmokeSmall). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
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
 * 1.12.2 ParticleSmokeSmall ("whiteSmokeTiny"): a small pale wisp, a little see-through, sinking slowly for 110 ticks and
 * spreading out along whatever it settles on.
 */
public class SmallSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected SmallSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.xd = this.xd * 0.01D + dx;
        this.yd = this.yd * 0.01D + dy;
        this.zd = this.zd * 0.01D + dz;
        setSize(0.05F, 0.05F);
        this.alpha = 0.8F;
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.2D) + 0.7F;
        this.quadSize *= 0.3F;
        this.baseSize = this.quadSize;
        this.lifetime = 110;
        this.hasPhysics = true;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

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
        move(this.xd, this.yd, this.zd);
        if (this.y == this.yo) {
            this.xd *= 1.03D;
            this.zd *= 1.03D;
        }
        this.xd *= 0.99D;
        this.yd *= 0.99D;
        this.zd *= 0.99D;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SmallSmokeParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
