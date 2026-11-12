/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad#getDockedEntity and
 * TileEntityBuggyFueler). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.BuggyFuelingPadBlock;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.entity.BuggyEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Machines beside a joined launch or fueling pad (Fuel Loader, Cargo Loader / Unloader, Launch Controller) and what stands on it. */
public final class PadVehicles {
    private PadVehicles() {
    }

    /** 1.12.2: the first joined pad found on any side of the machine (any part of it); its centre. */
    @Nullable
    public static BlockPos findPad(Level level, BlockPos machine) {
        for (Direction side : Direction.values()) {
            final BlockPos centre = LandingPadBlock.findCentre(level, machine.relative(side));
            if (centre != null) {
                return centre;
            }
        }
        return null;
    }

    /** The rocket standing on the launch pad centred at {@code centre}, not yet launched. */
    @Nullable
    public static RocketEntity dockedRocket(Level level, @Nullable BlockPos centre) {
        if (centre == null || LandingPadBlock.findLaunchPadCentre(level, centre) == null) {
            return null;
        }
        final List<RocketEntity> rockets = level.getEntitiesOfClass(RocketEntity.class,
                new AABB(centre).inflate(0.5D, 0.0D, 0.5D).expandTowards(0.0D, 2.0D, 0.0D), rocket -> !rocket.launched() && rocket.isAlive());
        return rockets.isEmpty() ? null : rockets.get(0);
    }

    /** The rocket on a launch pad, or the buggy parked on a fueling pad (within 1.5 blocks of the centre, up to 1 above). */
    @Nullable
    public static Entity dockedVehicle(Level level, @Nullable BlockPos centre) {
        if (centre == null || LandingPadBlock.findCentre(level, centre) == null) {
            return null;
        }
        if (level.getBlockState(centre).getBlock() instanceof BuggyFuelingPadBlock) {
            final List<BuggyEntity> buggies = level.getEntitiesOfClass(BuggyEntity.class,
                    new AABB(centre).inflate(1.5D, 0.0D, 1.5D).expandTowards(0.0D, 1.0D, 0.0D), BuggyEntity::isAlive);
            return buggies.isEmpty() ? null : buggies.get(0);
        }
        return dockedRocket(level, centre);
    }
}
