package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A falling meteor (1.12.2 EntityMeteor). It flies through blocks until the ray along its next move touches a block
 * or an entity, then explodes (strength size / 3 + 2), drops a hot Fallen Meteor on top of what it hit and deals 6
 * damage to an entity it struck. Size 1 meteors are common, size 6 ones a third as common.
 */
public class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static MeteorEntity create(Level level, double x, double y, double z, double motionX, double motionY, double motionZ, int size) {
        final MeteorEntity meteor = new MeteorEntity(GCEntities.METEOR.get(), level);
        meteor.setPos(x, y, z);
        meteor.setDeltaMovement(motionX, motionY, motionZ);
        meteor.entityData.set(SIZE, size);
        return meteor;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1);
    }

    public int size() {
        return this.entityData.get(SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        setYRot(getYRot() + 2.0F);
        setXRot(getXRot() + 2.0F);
        setDeltaMovement(getDeltaMovement().add(0.0D, -0.03999999910593033D, 0.0D));
        move(MoverType.SELF, getDeltaMovement());
        if (this.level().isClientSide) {
            return;
        }
        final Vec3 current = position();
        final Vec3 next = current.add(getDeltaMovement());
        HitResult hit = null;
        final BlockHitResult blockHit = this.level().clip(new ClipContext(current, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        Vec3 end = next;
        if (blockHit.getType() != HitResult.Type.MISS) {
            hit = blockHit;
            end = blockHit.getLocation();
        }
        final EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, current, end,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(2.0D), entity -> entity.isPickable() && !entity.isSpectator(), 0.01F);
        if (entityHit != null) {
            hit = entityHit;
        }
        if (hit != null) {
            onImpact(hit);
        } else if (getY() <= this.level().getMinBuildHeight() - 20 || getY() >= 400.0D) {
            discard();
        }
    }

    private void onImpact(HitResult hit) {
        this.level().explode(this, getX(), getY(), getZ(), size() / 3 + 2, false,
                GCConfig.METEOR_BLOCK_DAMAGE.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        final BlockPos above;
        if (hit instanceof BlockHitResult blockHit) {
            above = blockHit.getBlockPos().above();
        } else {
            final Entity target = ((EntityHitResult) hit).getEntity();
            above = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target.blockPosition()).above();
            target.hurt(this.damageSources().explosion(this, null), io.github.reiko45.galacticraft.GCConfig.hardMode() ? 12.0F : 6.0F);
        }
        if (this.level().getBlockState(above).isAir()) {
            this.level().setBlockAndUpdate(above, GCBlocks.FALLEN_METEOR.get().defaultBlockState());
        }
        discard();
    }

    /** Meteors are not saved (1.12.2 wrote nothing for them). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
