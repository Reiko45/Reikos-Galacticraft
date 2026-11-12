/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.entities.EntityEntryPodVenus on
 * core.entities.EntityLanderBase). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Venus Entry Pod (1.12.2 EntityEntryPodVenus): how a player arrives on Venus, from y 900. After 40 ticks for the ground
 * to load it plunges at 2.5 blocks a tick, burning through the atmosphere (a flame around it above y 382) while the air
 * slows it to 0.7; from y 242 a parachute opens and brakes it to about 0.4, folding away for the last 5 blocks. It
 * settles without a crash, and landed, opens as a chest with the rocket's cargo like the other landers.
 */
public class VenusEntryPodEntity extends LanderEntity {
    /** 1.12.2 getInitialMotionY. */
    private static final double INITIAL_SPEED = -2.5D;
    /** Below this the parachute is out (1.12.2 tickInAir and RenderEntryPodVenus). */
    public static final double PARACHUTE_HEIGHT = 242.0D;
    /** Above this the pod burns (RenderEntryPodVenus). */
    public static final double FLAME_HEIGHT = 382.0D;

    public VenusEntryPodEntity(EntityType<? extends VenusEntryPodEntity> type, Level level) {
        super(type, level);
    }

    public static VenusEntryPodEntity create(Level level, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel) {
        return load(new VenusEntryPodEntity(GCEntities.VENUS_ENTRY_POD.get(), level), x, y, z, cargo, fuel);
    }

    /** 1.12.2 getMotionVec + tickInAir: -2.5 for ticks 40 to 44, then gravity of 0.002 against the drag of the air. */
    @Override
    protected void fly() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.ticks < 40) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double motionY = this.ticks < 45 ? INITIAL_SPEED : getDeltaMovement().y;
        motionY -= 0.002D;
        if (motionY < -0.7D) {
            motionY *= 0.994D;
        }
        if (getY() <= PARACHUTE_HEIGHT) {
            motionY *= parachuteOpen() ? 0.995D : 0.9995D;
        }
        setDeltaMovement(0.0D, motionY, 0.0D);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** The parachute is out below y 242 until 5 blocks over the ground (1.12.2 groundPosY). */
    public boolean parachuteOpen() {
        if (getY() > PARACHUTE_HEIGHT || onGround()) {
            return false;
        }
        final int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        return getY() - ground > 5.0D;
    }

    /** 1.12.2 shouldSpawnParticles: none (the flame is drawn by the renderer). */
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
