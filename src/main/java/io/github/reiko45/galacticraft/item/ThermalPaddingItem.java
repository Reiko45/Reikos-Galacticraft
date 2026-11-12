/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.items.ItemThermalPadding and
 * api.item.IItemThermal). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Thermal Padding (1.12.2 ItemThermalPadding, thermal strength 1): worn in the four thermal slots of the Galacticraft
 * inventory, a full set slows the cold of Mars and the asteroids and slowly brings the body back to normal. Right-click
 * puts a piece in its empty slot.
 */
public class ThermalPaddingItem extends Item {
    private final GearSlot slot;
    private final int strength;

    public ThermalPaddingItem(Properties properties, GearSlot slot, int strength) {
        super(properties.stacksTo(1));
        this.slot = slot;
        this.strength = strength;
    }

    public GearSlot slot() {
        return this.slot;
    }

    /** 1.12.2 IItemThermal#getThermalStrength. */
    public int strength() {
        return this.strength;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            final GCPlayerData data = GCPlayerData.get(serverPlayer);
            if (data.gear(this.slot).isEmpty()) {
                data.setGear(this.slot, stack.copy());
                return InteractionResultHolder.success(ItemStack.EMPTY);
            }
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.consume(stack);
    }
}
