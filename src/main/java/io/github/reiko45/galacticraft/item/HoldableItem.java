/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.item.IHoldableItem). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import net.minecraft.world.entity.player.Player;

/**
 * 1.12.2 IHoldableItem: an item a player carries over their head (rockets, the Moon Buggy, the Astro Miner and the
 * flag). Both arms go up by default, and the player is drawn crouching unless {@link #crouches} says not.
 */
public interface HoldableItem {
    default boolean holdsLeftHandUp(Player player) {
        return true;
    }

    default boolean holdsRightHandUp(Player player) {
        return true;
    }

    /** 1.12.2 shouldCrouch: drawn crouching while standing on the ground with it in the main hand. */
    default boolean crouches(Player player) {
        return true;
    }
}
