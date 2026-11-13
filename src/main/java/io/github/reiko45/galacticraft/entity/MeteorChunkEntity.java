/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityMeteorChunk). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 1.12.2 EntityMeteorChunk: thrown like an arrow (1.5 blocks a tick, a little spread), tumbling as it flies, sticking in
 * blocks for a minute and picked up again as a (cold) chunk. It hurts what it hits for its speed times 1.6, rounded up,
 * as "meteor debris"; a hot one is alight for 20 seconds and sets what it hits alight for 2. It cools after 20 seconds
 * or in water, which slows it to 0.8 a tick. It falls with the world's gravity, like arrows here.
 */
public class MeteorChunkEntity extends AbstractArrow {
    private static final EntityDataAccessor<Boolean> HOT = SynchedEntityData.defineId(MeteorChunkEntity.class, EntityDataSerializers.BOOLEAN);
    /** 1.12.2's tumble each tick, in degrees. */
    private final float spinPitch = this.random.nextFloat() * 20 - 10;
    private final float spinYaw = this.random.nextFloat() * 20 - 10;
    public float tumblePitch;
    public float tumbleYaw;
    public float tumblePitchO;
    public float tumbleYawO;

    public MeteorChunkEntity(EntityType<? extends MeteorChunkEntity> type, Level level) {
        super(type, level);
        setBaseDamage(io.github.reiko45.galacticraft.GCConfig.hardMode() ? 3.2D : 1.6D);
        setSoundEvent(SoundEvents.EMPTY);
    }

    /** Thrown by {@code thrower} (1.12.2: from the eyes, 0.16 to the side and 0.1 down). */
    public MeteorChunkEntity(Level level, LivingEntity thrower, boolean hot) {
        super(GCEntities.METEOR_CHUNK.get(), thrower, level, new ItemStack(GCItems.METEOR_CHUNK.get()), null);
        setBaseDamage(io.github.reiko45.galacticraft.GCConfig.hardMode() ? 3.2D : 1.6D);
        setSoundEvent(SoundEvents.EMPTY);
        moveTo(thrower.getX() - Mth.cos(thrower.getYRot() * Mth.DEG_TO_RAD) * 0.16F, thrower.getEyeY() - 0.1D,
                thrower.getZ() - Mth.sin(thrower.getYRot() * Mth.DEG_TO_RAD) * 0.16F, thrower.getYRot(), thrower.getXRot());
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0.0F, 1.5F, 1.0F);
        setHot(hot);
    }

    /** From a dispenser. */
    public MeteorChunkEntity(Level level, double x, double y, double z, boolean hot) {
        super(GCEntities.METEOR_CHUNK.get(), x, y, z, level, new ItemStack(GCItems.METEOR_CHUNK.get()), null);
        setBaseDamage(io.github.reiko45.galacticraft.GCConfig.hardMode() ? 3.2D : 1.6D);
        setSoundEvent(SoundEvents.EMPTY);
        setHot(hot);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOT, false);
    }

    public boolean isHot() {
        return this.entityData.get(HOT);
    }

    public void setHot(boolean hot) {
        this.entityData.set(HOT, hot);
        if (hot) {
            igniteForSeconds(20.0F); // 1.12.2 setFire(20)
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isHot() && (this.tickCount > 400 || isInWater())) {
            this.entityData.set(HOT, false);
        }
        this.tumblePitchO = this.tumblePitch;
        this.tumbleYawO = this.tumbleYaw;
        if (!this.inGround) {
            this.tumblePitch += this.spinPitch;
            this.tumbleYaw += this.spinYaw;
        }
    }

    @Override
    protected float getWaterInertia() {
        return 0.8F;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(GCItems.METEOR_CHUNK.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.EMPTY;
    }

    /** 1.12.2 onUpdate's entity hit: damage, fire, an arrow stuck in it, thorns; bounces off what it couldn't hurt. */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        final Entity target = result.getEntity();
        final double speed = getDeltaMovement().length();
        final int damage = Mth.ceil(Mth.clamp(speed * getBaseDamage(), 0.0D, Integer.MAX_VALUE));
        final Entity owner = getOwner();
        final DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GCDamageTypes.METEOR_CHUNK),
                this, owner != null ? owner : this);
        if (isOnFire() && target.getType() != EntityType.ENDERMAN) {
            target.igniteForSeconds(2.0F);
        }
        if (target.hurt(source, damage)) {
            if (target instanceof LivingEntity living) {
                if (!level().isClientSide) {
                    living.setArrowCount(living.getArrowCount() + 1);
                }
                if (level() instanceof ServerLevel server) {
                    EnchantmentHelper.doPostAttackEffects(server, living, source);
                }
                if (owner instanceof ServerPlayer shooter && target != owner && target instanceof Player) {
                    shooter.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                }
            }
            if (target.getType() != EntityType.ENDERMAN) {
                discard();
            }
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.1D));
            setYRot(getYRot() + 180.0F);
            this.yRotO += 180.0F;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hot", isHot());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(HOT, tag.getBoolean("Hot"));
    }
}
