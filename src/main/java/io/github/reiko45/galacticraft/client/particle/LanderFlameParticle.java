/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.fx.ParticleLanderFlame). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

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
 * 1.12.2 ParticleLanderFlame: a big pale flame, eight times a smoke puff, shot down from the lander and gone in five
 * ticks, turning bluer as it goes. Always fully lit.
 */
public class LanderFlameParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected LanderFlameParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = this.xd * 0.1D + dx;
        this.yd = dy;
        this.zd = this.zd * 0.1D + dz;
        this.rCol = 200.0F / 255.0F;
        this.gCol = 200.0F / 255.0F;
        this.bCol = Math.min(1.0F, 200.0F / 255.0F + this.random.nextFloat() / 3.0F);
        this.quadSize *= 8.0F;
        this.baseSize = this.quadSize;
        this.lifetime = 5;
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
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
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
        this.gCol = Math.max(0.0F, this.gCol - 0.09F);
        this.rCol = Math.max(0.0F, this.rCol - 0.09F);
        if (this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new LanderFlameParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
