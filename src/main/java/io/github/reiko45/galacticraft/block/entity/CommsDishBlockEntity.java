/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityDish). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityDish, as far as it shows: the dish turns a full circle every 4320 ticks and tips up and down between
 * 0 and 45 degrees. (Its sun-following target angle was worked out but never drawn, and its energy store never
 * filled, so neither is kept.)
 */
public class CommsDishBlockEntity extends BlockEntity {
    private int ticks;

    public CommsDishBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.COMMS_DISH.get(), pos, state);
    }

    public void clientTick() {
        this.ticks++;
    }

    /** 1.12.2 rotation: degrees round. */
    public float rotation(float partial) {
        return (this.ticks + partial) / 12.0F;
    }

    /** 1.12.2 elevation: degrees tipped. */
    public float elevation(float partial) {
        return (Mth.sin(rotation(partial) / 40.0F) + 1.0F) * 22.5F;
    }

    public AABB renderBox() {
        return new AABB(this.worldPosition.offset(-3, 0, -3).getCenter(), this.worldPosition.offset(3, 8, 3).getCenter()).inflate(0.5D);
    }
}
