package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCArmorMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2 ItemArmorMars. Desh armour had three texture sheets instead of the usual two: desh_1 for the helmet,
 * desh_2 for the chestplate and boots, desh_3 for the leggings.
 */
public class DeshArmorItem extends ArmorItem {
    private static final ResourceLocation HELMET = GalacticraftCore.id("textures/models/armor/desh_1.png");
    private static final ResourceLocation BODY = GalacticraftCore.id("textures/models/armor/desh_2.png");
    private static final ResourceLocation LEGS = GalacticraftCore.id("textures/models/armor/desh_3.png");

    public DeshArmorItem(Type type, Properties properties) {
        super(GCArmorMaterials.DESH, type, properties);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return switch (this.getType()) {
            case HELMET -> HELMET;
            case LEGGINGS -> LEGS;
            default -> BODY;
        };
    }
}
