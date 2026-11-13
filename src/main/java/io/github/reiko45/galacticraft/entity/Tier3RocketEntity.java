/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityTier3Rocket).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tier 3 Rocket (1.12.2 EntityTier3Rocket, planets module): 1,500 mB of fuel; climbs at up to 3.5 blocks/tick (5 in a
 * vacuum), burning 1 mB every 2 ticks; the only rocket that reaches the asteroid belt.
 */
public class Tier3RocketEntity extends RocketEntity {
    public static final int FUEL_CAPACITY = 1500;

    public Tier3RocketEntity(EntityType<? extends Tier3RocketEntity> type, Level level) {
        super(type, level);
    }

    public static Tier3RocketEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        return place(new Tier3RocketEntity(GCEntities.TIER_3_ROCKET.get(), level), x, y, z, storage, fuel);
    }

    @Override
    public int tier() {
        return 3;
    }

    @Override
    public int fuelCapacity() {
        return FUEL_CAPACITY;
    }

    @Override
    public ItemStack item(int storage, int fuel) {
        return GCItems.TIER_3_ROCKET.get().stack(storage, fuel);
    }

    /** 1.12.2: motionY = 2.5 x min(t / 150, 1.4), or 2.5 x min(1.2 t / 150, 2) without an atmosphere. */
    @Override
    protected double speedCap(boolean airless) {
        return airless ? 2.0D : 1.4D;
    }

    @Override
    protected double climbFactor() {
        return 2.5D;
    }

    @Override
    protected double burnTicks() {
        return 2.0D;
    }

    /** 1.12.2 getMountedYOffset 1.75 with the player's -0.35 (1.21 players sit 0.6 lower). */
    @Override
    protected double riderHeight() {
        return 1.75D - 0.35D + 0.6D;
    }

    @Override
    protected void spawnFlames() {
        RocketEffects.tier3(this);
    }
}
