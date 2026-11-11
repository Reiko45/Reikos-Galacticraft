/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.fx.ParticleDrip). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * 1.12.2 ParticleDrip: a dark green drop that hangs for 40 ticks, then falls (gravity 0.06) until it lands or reaches
 * a liquid or solid block. It uses the water drip's hanging and falling sprites (1.12.2 particle indices 113 and 112).
 */
public class BacterialDripParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private int bobTimer = 40;

    protected BacterialDripParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.lifetime = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
        this.xd = this.yd = this.zd = 0.0D;
        setColor(0.0F, 0.2F, 0.1F);
        setSprite(sprites.get(0, 1));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.yd -= this.gravity;
        if (this.bobTimer-- > 0) {
            this.xd *= 0.02D;
            this.yd *= 0.02D;
            this.zd *= 0.02D;
            setSprite(this.sprites.get(0, 1));
        } else {
            setSprite(this.sprites.get(1, 1));
        }
        move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;
        if (this.lifetime-- <= 0 || this.onGround) {
            remove();
            return;
        }
        final BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        final BlockState state = this.level.getBlockState(pos);
        final FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() || state.isSolid()) {
            final double surface = Mth.floor(this.y) + (fluid.isEmpty() ? 1.0D : fluid.getHeight(this.level, pos));
            if (this.y < surface) {
                remove();
            }
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new BacterialDripParticle(level, x, y, z, this.sprites);
        }
    }
}
