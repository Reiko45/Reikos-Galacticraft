package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.OxygenDetectorBlock;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityOxygenDetector: every 50 ticks, on an airless world check for breathable air in a box 0.6 blocks
 * around the detector; elsewhere any non-solid neighbouring face counts as air.
 */
public class OxygenDetectorBlockEntity extends BlockEntity {
    private int ticks = 49;

    public OxygenDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.OXYGEN_DETECTOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenDetectorBlockEntity detector) {
        if (++detector.ticks < 50) {
            return;
        }
        detector.ticks = 0;
        boolean oxygen = false;
        if (CelestialPhysics.isAirless(level)) {
            oxygen = OxygenUtil.isInBreathableAir(level, new AABB(pos).inflate(0.6D));
        } else {
            for (Direction side : Direction.values()) {
                final BlockPos next = pos.relative(side);
                if (!level.getBlockState(next).isFaceSturdy(level, next, side.getOpposite())) {
                    oxygen = true;
                    break;
                }
            }
        }
        if (state.getBlock() instanceof OxygenDetectorBlock block) {
            block.setOxygen(level, pos, state, oxygen);
        }
    }
}
