package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.world.item.ItemStack;

/** Tier 1 Rocket (1.12.2 ItemTier1Rocket). */
public class Tier1RocketItem extends RocketItem {
    public Tier1RocketItem(Properties properties) {
        super(properties, Tier1RocketEntity.FUEL_CAPACITY, 0.0D, Tier1RocketEntity::create);
    }

    public static ItemStack create(int storage, int fuel) {
        return GCItems.TIER_1_ROCKET.get().stack(storage, fuel);
    }
}
