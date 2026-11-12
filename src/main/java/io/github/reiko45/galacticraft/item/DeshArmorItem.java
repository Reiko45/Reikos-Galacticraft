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
    private final ResourceLocation helmet;
    private final ResourceLocation body;
    private final ResourceLocation legs;

    public DeshArmorItem(Type type, Properties properties) {
        this(GCArmorMaterials.DESH, "desh", type, properties);
    }

    /** Armour drawn from three 1.12.2 sheets {@code <sheet>_1} (helmet), {@code _2} (chest and boots) and {@code _3} (legs). */
    public DeshArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, String sheet, Type type, Properties properties) {
        super(material, type, properties);
        this.helmet = GalacticraftCore.id("textures/models/armor/" + sheet + "_1.png");
        this.body = GalacticraftCore.id("textures/models/armor/" + sheet + "_2.png");
        this.legs = GalacticraftCore.id("textures/models/armor/" + sheet + "_3.png");
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return switch (this.getType()) {
            case HELMET -> this.helmet;
            case LEGGINGS -> this.legs;
            default -> this.body;
        };
    }
}
