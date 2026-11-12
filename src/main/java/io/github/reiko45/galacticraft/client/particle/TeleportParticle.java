/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.fx.EntityFXTeleport). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import java.lang.ref.WeakReference;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 EntityFXTeleport ("portalBlue"): a portal mote that swings out along its motion and back, drifting a block
 * upwards as it goes, glowing brighter with age, coloured each tick by its telepad's state (1.12.2 getParticleColor).
 */
public class TeleportParticle extends TextureSheetParticle {
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double moveX;
    private final double moveY;
    private final double moveZ;
    private final float baseSize;
    private final boolean sending;
    private final WeakReference<ShortRangeTelepadBlockEntity> telepad;

    protected TeleportParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, boolean sending,
                               @Nullable ShortRangeTelepadBlockEntity telepad, SpriteSet sprites) {
        super(level, x, y, z);
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.moveX = dx;
        this.moveY = dy;
        this.moveZ = dz;
        this.sending = sending;
        this.telepad = new WeakReference<>(telepad);
        this.baseSize = (this.random.nextFloat() * 0.2F + 0.5F) * 0.1F;
        this.quadSize = this.baseSize;
        this.lifetime = (int) (Math.random() * 10.0D) + 40;
        this.hasPhysics = false;
        pickSprite(sprites);
        recolour();
    }

    private void recolour() {
        final ShortRangeTelepadBlockEntity pad = this.telepad.get();
        if (pad != null && !pad.isRemoved()) {
            final float[] color = pad.particleColor(this.random, this.sending);
            setColor(color[0], color[1], color[2]);
        }
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
        return this.baseSize * (1.0F - f);
    }

    /** 1.12.2 getBrightnessForRender: the sky light part climbs to full by the end of its life. */
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
        recolour();
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

    /** One provider per direction: rising motes look like the pad's receiving side, falling ones its sending side. */
    public record Provider(SpriteSet sprites, boolean sending) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            final BlockPos pad = BlockPos.containing(x, y - (this.sending ? 2.9D : 0.1D) + 0.01D, z);
            final ShortRangeTelepadBlockEntity telepad = level.getBlockEntity(pad) instanceof ShortRangeTelepadBlockEntity found ? found : null;
            return new TeleportParticle(level, x, y, z, dx, dy, dz, this.sending, telepad, this.sprites);
        }
    }
}
