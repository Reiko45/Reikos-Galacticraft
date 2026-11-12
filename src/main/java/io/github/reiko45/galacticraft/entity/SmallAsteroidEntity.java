/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.entities.EntitySmallAsteroid and the
 * spawning in entities.player.AsteroidsPlayerHandler). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT
 * license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Small Asteroid (1.12.2 EntitySmallAsteroid): a block of asteroid rock tumbling past. It drifts in a straight line,
 * spinning, and goes when it stops, leaves the y range -32..288 or is 3,000 ticks old. They are never saved.
 */
public class SmallAsteroidEntity extends Entity {
    private static final EntityDataAccessor<Float> SPIN_PITCH = SynchedEntityData.defineId(SmallAsteroidEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPIN_YAW = SynchedEntityData.defineId(SmallAsteroidEntity.class, EntityDataSerializers.FLOAT);

    private boolean firstUpdate = true;

    public SmallAsteroidEntity(EntityType<? extends SmallAsteroidEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPIN_PITCH, 0.0F);
        builder.define(SPIN_YAW, 0.0F);
    }

    public void setSpin(float pitch, float yaw) {
        this.entityData.set(SPIN_PITCH, pitch);
        this.entityData.set(SPIN_YAW, yaw);
    }

    @Override
    public void tick() {
        if (!this.firstUpdate && !level().isClientSide()) {
            if (Math.abs(getX() - this.xo) + Math.abs(getZ() - this.zo) <= 0.0D) {
                discard(); // stopped moving
                return;
            }
            if (getY() > 288.0D || getY() < -32.0D || this.tickCount > 3000) {
                discard();
                return;
            }
        }
        super.tick();
        setXRot(getXRot() + this.entityData.get(SPIN_PITCH));
        setYRot(getYRot() + this.entityData.get(SPIN_YAW));
        // 1.12.2: a slow one speeds up again, a little
        final Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.05D) {
            setDeltaMovement(motion.scale(1.001D));
        }
        move(MoverType.SELF, getDeltaMovement());
        this.firstUpdate = false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /**
     * 1.12.2 AsteroidsPlayerHandler#onPlayerUpdate: among the asteroids, below y 260, one tick in 50 a small asteroid
     * appears 30-89 blocks away at about the player's height and drifts past near them. (1.12.2 also looked for a closer
     * player within 100 blocks to leave it to, but that search always found the player themselves.)
     */
    public static void spawnNear(ServerPlayer player) {
        final Level level = player.level();
        final RandomSource random = level.getRandom();
        if (GCConfig.DISABLE_SMALL_ASTEROIDS.get() || level.dimension() != GCDimensions.ASTEROIDS || random.nextInt(50) != 0 || player.getY() >= 260.0D) {
            return;
        }
        final double r = random.nextInt(60) + 30.0D;
        final double theta = Math.PI * 2.0D * random.nextDouble();
        final double x = player.getX() + Math.cos(theta) * r;
        final double y = player.getY() + random.nextInt(5);
        final double z = player.getZ() + Math.sin(theta) * r;
        final double motionX = (player.getX() - x + (random.nextDouble() - 0.5D) * 40.0D) / 400.0D;
        final double motionY = (random.nextDouble() - 0.5D) * 0.4D;
        final double motionZ = (player.getZ() - z + (random.nextDouble() - 0.5D) * 40.0D) / 400.0D;
        final SmallAsteroidEntity asteroid = new SmallAsteroidEntity(GCEntities.SMALL_ASTEROID.get(), level);
        asteroid.setPos(x, y, z);
        asteroid.setDeltaMovement(motionX, motionY, motionZ);
        asteroid.setSpin(random.nextFloat() * 2.0F, random.nextFloat() * 4.0F);
        level.addFreshEntity(asteroid);
    }
}
