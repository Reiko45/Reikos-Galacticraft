/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemBasic, the frequency module).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 1.12.2 frequency module (basic_item 19): hitting a mob (not a player) with it tracks that mob ("linkedUUID"); using it
 * on a Telemetry Unit then links the two, and remembers the unit ("teCoord", "teDim") so that wearing the module links
 * the wearer instead. The data sits in the stack's custom data under the 1.12.2 names.
 */
public class FrequencyModuleItem extends Item {
    public FrequencyModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && !(entity instanceof Player)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putLong("linkedUUIDMost", entity.getUUID().getMostSignificantBits());
                tag.putLong("linkedUUIDLeast", entity.getUUID().getLeastSignificantBits());
            });
            player.sendSystemMessage(Component.translatable("gui.galacticraftcore.tracking.message"));
            return true;
        }
        return false;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Nullable
    public static UUID linkedEntity(ItemStack stack) {
        final CompoundTag tag = data(stack);
        return tag.contains("linkedUUIDMost") && tag.contains("linkedUUIDLeast") ? new UUID(tag.getLong("linkedUUIDMost"), tag.getLong("linkedUUIDLeast")) : null;
    }

    /** Remember the Telemetry Unit it was last used on. */
    public static void setTelemetry(ItemStack stack, Level level, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("teCoordX", pos.getX());
            tag.putInt("teCoordY", pos.getY());
            tag.putInt("teCoordZ", pos.getZ());
            tag.putString("teDim", level.dimension().location().toString());
        });
    }

    public static boolean hasTelemetry(ItemStack stack) {
        return data(stack).contains("teDim");
    }

    @Nullable
    public static ResourceKey<Level> telemetryLevel(ItemStack stack) {
        final ResourceLocation id = ResourceLocation.tryParse(data(stack).getString("teDim"));
        return id == null || !hasTelemetry(stack) ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    public static BlockPos telemetryPos(ItemStack stack) {
        final CompoundTag tag = data(stack);
        return new BlockPos(tag.getInt("teCoordX"), tag.getInt("teCoordY"), tag.getInt("teCoordZ"));
    }
}
