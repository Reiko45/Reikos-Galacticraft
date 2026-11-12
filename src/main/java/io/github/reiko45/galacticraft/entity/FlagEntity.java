/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityFlag). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A planted flag (1.12.2 EntityFlag): it shows its owner's Space Race flag on a 2.5 block pole. Right-click turns it 3
 * degrees; hits dent it (ten times the damage) until it breaks at more than 40 and drops the flag, and a creative player
 * knocks it down at once. With air under it, it falls.
 */
public class FlagEntity extends Entity {
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(FlagEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(FlagEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(FlagEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FACING_ANGLE = SynchedEntityData.defineId(FlagEntity.class, EntityDataSerializers.INT);

    /** 1.12.2 indestructable: set by NBT only (for maps). */
    private boolean indestructible;

    public FlagEntity(EntityType<? extends FlagEntity> type, Level level) {
        super(type, level);
    }

    /** A flag standing at (x, y, z), facing {@code angle} degrees, flying its owner's team flag. */
    public static FlagEntity create(Level level, double x, double y, double z, int angle, Player owner) {
        final FlagEntity flag = new FlagEntity(GCEntities.FLAG.get(), level);
        flag.setPos(x, y, z);
        flag.setFacingAngle(angle);
        flag.setOwner(owner.getUUID(), owner.getGameProfile().getName());
        return flag;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_NAME, "");
        builder.define(OWNER, Optional.empty());
        builder.define(DAMAGE, 0.0F);
        builder.define(FACING_ANGLE, -1);
    }

    public void setOwner(@Nullable UUID id, String name) {
        this.entityData.set(OWNER, Optional.ofNullable(id));
        this.entityData.set(OWNER_NAME, name);
    }

    public String ownerName() {
        return this.entityData.get(OWNER_NAME);
    }

    @Nullable
    public UUID owner() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public float damage() {
        return this.entityData.get(DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DAMAGE, damage);
    }

    public int facingAngle() {
        return this.entityData.get(FACING_ANGLE);
    }

    public void setFacingAngle(int angle) {
        this.entityData.set(FACING_ANGLE, angle);
    }

    public boolean indestructible() {
        return this.indestructible;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved() || this.indestructible) {
            return true;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        final boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        markHurt();
        setDamage(damage() + amount * 10.0F);
        playSound(SoundType.METAL, 1.0F);
        if (creative) {
            setDamage(100.0F);
        }
        if (creative || damage() > 40.0F) {
            ejectPassengers();
            discard();
            if (!creative) {
                spawnAtLocation(new ItemStack(GCItems.FLAG.get()), 0.0F);
            }
        }
        return true;
    }

    private void playSound(SoundType sound, float pitchBoost) {
        level().playSound(null, getX(), getY(), getZ(), sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch() + pitchBoost);
    }

    @Override
    public void tick() {
        super.tick();
        // 1.12.2: it falls while there is air under it (a fence holds it up)
        final BlockPos below = BlockPos.containing(getX(), getY() - 1.0D, getZ());
        final var state = level().getBlockState(below);
        if (!(state.getBlock() instanceof FenceBlock) && state.isAir()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.02D, 0.0D));
        }
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            setFacingAngle(facingAngle() + 3);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(GCItems.FLAG.get());
    }

    /** 1.12.2 RenderFlag grew the box by (1, 2, 1) so the cloth stays drawn when only it is in view. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.0D, 2.0D, 1.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setOwner(tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null, tag.getString("Owner"));
        this.indestructible = tag.getBoolean("Indestructable");
        setFacingAngle(tag.getInt("AngleI"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Owner", ownerName());
        if (owner() != null) {
            tag.putUUID("OwnerUUID", owner());
        }
        tag.putBoolean("Indestructable", this.indestructible);
        tag.putInt("AngleI", facingAngle());
    }
}
