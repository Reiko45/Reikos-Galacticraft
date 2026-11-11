package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tier 1 Rocket (1.12.2 EntityTier1Rocket): 1,000 mB of fuel; climbs at up to 1 block/tick (1.6 in a vacuum), burning
 * 1 mB every 3 ticks; reaches the Moon and Earth orbit.
 */
public class Tier1RocketEntity extends RocketEntity {
    public static final int FUEL_CAPACITY = 1000;

    public Tier1RocketEntity(EntityType<? extends Tier1RocketEntity> type, Level level) {
        super(type, level);
    }

    public static Tier1RocketEntity create(Level level, double x, double y, double z, int storage, int fuel) {
        return place(new Tier1RocketEntity(GCEntities.TIER_1_ROCKET.get(), level), x, y, z, storage, fuel);
    }

    @Override
    public int tier() {
        return 1;
    }

    @Override
    public int fuelCapacity() {
        return FUEL_CAPACITY;
    }

    @Override
    public ItemStack item(int storage, int fuel) {
        return Tier1RocketItem.create(storage, fuel);
    }

    @Override
    protected double speedCap(boolean airless) {
        return airless ? 1.6D : 1.0D;
    }

    @Override
    protected double burnTicks() {
        return 3.0D;
    }

    /** 1.12.2 getMountedYOffset 0.3 with the player's -0.35: the rider stands at the rocket's floor (1.21 players sit 0.6 lower). */
    @Override
    protected double riderHeight() {
        return 0.3D - 0.35D + 0.6D;
    }

    @Override
    protected void spawnFlames() {
        RocketEffects.spawnFlames(this, 1);
    }
}
