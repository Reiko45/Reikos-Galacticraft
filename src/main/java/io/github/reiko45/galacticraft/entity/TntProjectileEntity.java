/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.entities.EntityProjectileTNT). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Creeper Boss's TNT (1.12.2 EntityProjectileTNT): a fireball that doesn't burn. It hits for 6 (9 on Normal, 12 on
 * Hard) as fireball damage, which spares creepers, and blows up with the power of a ghast's fireball (blocks only with
 * mob griefing). Like a fireball it can be punched or shot back (it is in the redirectable projectile tag), and that
 * is the only thing that hurts the boss.
 */
public class TntProjectileEntity extends AbstractHurtingProjectile {
    public TntProjectileEntity(EntityType<? extends TntProjectileEntity> type, Level level) {
        super(type, level);
    }

    /** 1.12.2 EntityFireball(world, shooter, accel): the aim gets a little gaussian spread. */
    public TntProjectileEntity(Level level, LivingEntity owner, double x, double y, double z, Vec3 aim) {
        super(GCEntities.TNT_PROJECTILE.get(), x, y, z, aim.add(owner.getRandom().nextGaussian() * 0.4D,
                owner.getRandom().nextGaussian() * 0.4D, owner.getRandom().nextGaussian() * 0.4D), level);
        setOwner(owner);
        setRot(owner.getYRot(), owner.getXRot());
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        final Entity hit = result.getEntity();
        if (!level().isClientSide && !(hit instanceof Creeper)) {
            final Difficulty difficulty = level().getDifficulty();
            final float extra = difficulty == Difficulty.HARD ? 2.0F : difficulty == Difficulty.NORMAL ? 1.0F : 0.0F;
            final Entity owner = getOwner();
            // DamageSources#fireball, which only takes a vanilla Fireball
            hit.hurt(owner == null ? damageSources().source(DamageTypes.UNATTRIBUTED_FIREBALL, this)
                    : damageSources().source(DamageTypes.FIREBALL, this, owner), 6.0F + 3.0F * extra);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(null, getX(), getY(), getZ(), 1.0F, false, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
