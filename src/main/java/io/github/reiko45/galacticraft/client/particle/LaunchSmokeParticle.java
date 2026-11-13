/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.fx.ParticleLaunchSmoke and
 * EntityFXLaunchParticle). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.12.2 ParticleLaunchSmoke: a grey puff left by a launch flame (2.5 times as big for the large ones), falling, bouncing
 * off the ground and billowing out along it. Once launched it lives longer the bigger it is; on the pad it drifts off
 * sideways and lives 30 ticks longer.
 */
public class LaunchSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected LaunchSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float size, boolean launched,
                                  SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.xd *= 0.1D;
        this.yd *= 0.1D;
        this.zd *= 0.1D;
        setSize(0.2F, 0.2F);
        this.xd += dx;
        this.yd += dy;
        this.zd += dz;
        this.alpha = 1.0F;
        this.rCol = this.gCol = this.bCol = (float) (Math.random() * 0.3D) + 0.6F;
        this.quadSize *= 0.75F * size * 3.0F;
        this.baseSize = this.quadSize;
        if (launched) {
            this.lifetime = (int) (this.lifetime * size) + 10;
        } else {
            this.xd += this.random.nextDouble() / 2.0D - 0.25D;
            this.yd += this.random.nextDouble() / 20.0D;
            this.zd += this.random.nextDouble() / 2.0D - 0.25D;
            this.lifetime = 30 + this.lifetime;
        }
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
    }

    /**
     * 1.12.2 EntityFXLaunchParticle#move: stopped by blocks, it bounces up by more than it was stopped and slows to 0.7
     * sideways, and it falls a little faster each tick; it never sticks to the ground.
     */
    @Override
    public void move(double x, double y, double z) {
        final Vec3 allowed = Entity.collideBoundingBox(null, new Vec3(x, y, z), getBoundingBox(), this.level, List.of());
        if (allowed.y != y) {
            this.yd += (allowed.y - y) * 1.15D;
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
        this.yd -= 0.005D;
        if (allowed.lengthSqr() > 0.0D) {
            setBoundingBox(getBoundingBox().move(allowed));
            setLocationFromBoundingbox();
        }
        this.onGround = false;
    }

    public record Provider(SpriteSet sprites, float size, boolean launched) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new LaunchSmokeParticle(level, x, y, z, dx, dy, dz, this.size, this.launched, this.sprites);
        }
    }
}
