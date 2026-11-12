/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityEntryPod on
 * core.entities.EntityLanderBase). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * Entry Pod (1.12.2 EntityEntryPod): how a player arrives among the asteroids. It drops from y 310 over a chosen
 * asteroid, waits 40 ticks for the ground to load, then falls at half a block per tick, a little faster every tick
 * (0.002), with no thrusters and no crash: it simply settles on the rock and lights up the spot with Bright Air two
 * blocks above. Landed, the rider can get out and it opens as a chest with the rocket's cargo, like the Moon lander.
 */
public class EntryPodEntity extends LanderEntity {
    /** 1.12.2 getInitialMotionY. */
    private static final double INITIAL_SPEED = -0.5D;

    private boolean wasOnGround;

    public EntryPodEntity(EntityType<? extends EntryPodEntity> type, Level level) {
        super(type, level);
    }

    public static EntryPodEntity create(Level level, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel) {
        return load(new EntryPodEntity(GCEntities.ENTRY_POD.get(), level), x, y, z, cargo, fuel);
    }

    /** 1.12.2 getMotionVec + tickInAir: -0.5 for ticks 40 to 44, then 0.002 faster each tick. */
    @Override
    protected void fly() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            if (!this.wasOnGround) {
                onGroundHit();
            }
            this.wasOnGround = true;
            return;
        }
        this.wasOnGround = false;
        if (this.ticks < 40) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        final double motionY = this.ticks < 45 ? INITIAL_SPEED : getDeltaMovement().y - 0.002D;
        setDeltaMovement(0.0D, motionY, 0.0D);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** 1.12.2 onGroundHit: a light two blocks up, where the rider's head is. */
    private void onGroundHit() {
        final BlockPos light = blockPosition().above(2);
        if (level().getBlockState(light).isAir()) {
            level().setBlock(light, GCBlocks.BRIGHT_AIR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 1.12.2 shouldSpawnParticles: none. */
    @Override
    protected boolean hasThrusters() {
        return false;
    }

    /** 1.12.2 getMountedYOffset: height - 2 = 1, with the player's -0.35 (1.21 players sit 0.6 lower). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, 1.0D - 0.35D + 0.6D, 0.0D);
    }
}
