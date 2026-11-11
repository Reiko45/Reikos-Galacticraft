package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GCArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, GalacticraftCore.MOD_ID);

    /**
     * 1.12.2 ARMOR_STEEL: defense {boots 3, legs 6, chest 8, helmet 3}, enchantability 9, toughness 1.0,
     * repaired with Compressed Steel. Layer textures: textures/models/armor/heavy_duty_layer_{1,2}.png.
     */
    public static final Holder<ArmorMaterial> HEAVY_DUTY = ARMOR_MATERIALS.register("heavy_duty", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 8);
            }),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(GCItems.COMPRESSED_STEEL.get()),
            List.of(new ArmorMaterial.Layer(GalacticraftCore.id("heavy_duty"))),
            1.0F,
            0.0F));

    /**
     * 1.12.2 ARMORDESH: durability factor 42, protection 4/7/9/4, enchantability 12, toughness 3, repaired with Desh
     * Ingots. DeshArmorItem picks the three 1.12.2 texture sheets itself.
     */
    public static final Holder<ArmorMaterial> DESH = ARMOR_MATERIALS.register("desh", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 4);
                map.put(ArmorItem.Type.LEGGINGS, 7);
                map.put(ArmorItem.Type.CHESTPLATE, 9);
                map.put(ArmorItem.Type.HELMET, 4);
                map.put(ArmorItem.Type.BODY, 9);
            }),
            12,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(GCItems.DESH_INGOT.get()),
            List.of(new ArmorMaterial.Layer(GalacticraftCore.id("desh"))),
            3.0F,
            0.0F));

    private GCArmorMaterials() {
    }
}
