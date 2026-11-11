package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tier 2 Rocket (1.12.2 EntityTier2Rocket, planets module): 1,500 mB of fuel; climbs twice as fast as the Tier 1, at up to
 * 2.4 blocks/tick (3.6 in a vacuum), burning 1 mB every 2 ticks; reaches Mars as well as the Moon and Earth orbit.
 */
public class Tier2RocketEntity extends RocketEntity {
    public static final int FUEL_CAPACITY = 1500;

    public Tier2RocketEntity(EntityType<? extends Tier2RocketEntity> type, Level level) {
        super(type, level);
    }

    public static Tier2RocketEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        return place(new Tier2RocketEntity(GCEntities.TIER_2_ROCKET.get(), level), x, y, z, storage, fuel);
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public int fuelCapacity() {
        return FUEL_CAPACITY;
    }

    @Override
    public ItemStack item(int storage, int fuel) {
        return GCItems.TIER_2_ROCKET.get().stack(storage, fuel);
    }

    /** 1.12.2: motionY = 2 x min(t / 150, 1.2), or 2 x min(1.2 t / 150, 1.8) without an atmosphere. */
    @Override
    protected double speedCap(boolean airless) {
        return airless ? 1.8D : 1.2D;
    }

    @Override
    protected double climbFactor() {
        return 2.0D;
    }

    @Override
    protected double burnTicks() {
        return 2.0D;
    }

    /** 1.12.2 getMountedYOffset 1.6 with the player's -0.35 (1.21 players sit 0.6 lower). */
    @Override
    protected double riderHeight() {
        return 1.6D - 0.35D + 0.6D;
    }

    @Override
    protected void spawnFlames() {
        RocketEffects.spawnFlames(this, 2);
    }
}
