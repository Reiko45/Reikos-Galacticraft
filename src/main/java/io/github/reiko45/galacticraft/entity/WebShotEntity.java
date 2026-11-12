/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.entities.EntityWebShot). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Web Shot (1.12.2 EntityWebShot): the Spider Queen's ball of web. It flies straight, unaffected by gravity or the air,
 * and wraps the first creature it hits in Slowness III for 9 seconds; anything else it bounces off, and it falls apart
 * on the first block it hits (or after 50 seconds). It is aimed like a skeleton's arrow, a fifth of the distance high to
 * allow for a fall it never makes, so far off it flies over all but tall targets, as in 1.12.2.
 */
public class WebShotEntity extends Projectile {
    public WebShotEntity(EntityType<? extends WebShotEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    /** 1.12.2 EntityWebShot(world, shooter, target, velocity, inaccuracy): aimed like a skeleton's arrow. */
    public WebShotEntity(Level level, LivingEntity shooter, LivingEntity target, float velocity, float inaccuracy) {
        this(GCEntities.WEB_SHOT.get(), level);
        setOwner(shooter);
        final double y = shooter.getEyeY() - 0.10000000149011612D;
        final double dX = target.getX() - shooter.getX();
        final double dY = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - y;
        final double dZ = target.getZ() - shooter.getZ();
        final double flat = Math.sqrt(dX * dX + dZ * dZ);
        if (flat >= 1.0E-7D) {
            moveTo(shooter.getX() + dX / flat, y, shooter.getZ() + dZ / flat, (float) (Mth.atan2(dZ, dX) * Mth.RAD_TO_DEG) - 90.0F,
                    (float) (Mth.atan2(dY, flat) * -Mth.RAD_TO_DEG));
            shoot(dX, dY + flat * 0.20000000298023224D, dZ, velocity, inaccuracy);
        } else {
            setPos(shooter.getX(), y, shooter.getZ());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 1000) {
            discard();
            return;
        }
        final HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(hit);
        }
        if (isRemoved()) {
            return;
        }
        final Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        checkInsideBlocks();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide || result.getEntity() == getOwner()) {
            return;
        }
        if (result.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 2, true, true), getOwner());
            discard();
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.10000000149011612D));
            setYRot(getYRot() + 180.0F);
            this.yRotO += 180.0F;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }
}
