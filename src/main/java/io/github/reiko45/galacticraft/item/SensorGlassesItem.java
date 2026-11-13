/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemSensorGlasses). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.registry.GCArmorMaterials;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2 ItemSensorGlasses: worn on the head, it shows nearby ores (client SensorGlasses), draws mobs with a glowing
 * outline and full-bright footprints, and traces the leak of the nearest unsealed Oxygen Sealer. It can't be enchanted.
 */
public class SensorGlassesItem extends ArmorItem {
    public SensorGlassesItem(Properties properties) {
        super(GCArmorMaterials.SENSOR_GLASSES, Type.HELMET, properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    /** 1.12.2 ISensorGlassesArmor: wearing any such helmet counts. */
    public static boolean isWorn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SensorGlassesItem;
    }
}
