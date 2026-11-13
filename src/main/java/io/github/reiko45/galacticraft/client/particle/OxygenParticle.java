/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.fx.ParticleOxygen). Copyright (c) 2023
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

/**
 * 1.12.2 ParticleOxygen (as the Sensor Glasses' leak trace spawns it, coloured 0.7, 0.7, 1.0): a mote that jumps to
 * its target (start + motion) and swings back to where it was spawned, sinking from a block above, growing in and
 * glowing brighter with age.
 */
public class OxygenParticle extends TextureSheetParticle {
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double moveX;
    private final double moveY;
    private final double moveZ;

    protected OxygenParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.moveX = dx;
        this.moveY = dy;
        this.moveZ = dz;
        this.quadSize = 0.1F;
        this.lifetime = (int) (Math.random() * 10.0D) + 40;
        this.hasPhysics = false;
        setColor(0.7F, 0.7F, 1.0F);
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float f = (this.age + partialTick) / this.lifetime;
        f = 1.0F - f;
        f *= f;
        return 0.1F * (1.0F - f);
    }

    /** 1.12.2 getBrightnessForRender: the block light part climbs to full by the end of its life. */
    @Override
    protected int getLightColor(float partialTick) {
        final int light = super.getLightColor(partialTick);
        float f = (float) this.age / this.lifetime;
        f *= f;
        f *= f;
        final int sky = light & 255;
        final int block = Math.min(240, (light >> 16 & 255) + (int) (f * 15.0F * 16.0F));
        return sky | block << 16;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        final float age = (float) this.age / this.lifetime;
        float f = -age + age * age * 2.0F;
        f = 1.0F - f;
        this.x = this.startX + this.moveX * f;
        this.y = this.startY + this.moveY * f + (1.0F - age);
        this.z = this.startZ + this.moveZ * f;
        if (this.age++ >= this.lifetime) {
            remove();
        }
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new OxygenParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
