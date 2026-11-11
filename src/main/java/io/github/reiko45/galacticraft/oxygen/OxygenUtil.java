package io.github.reiko45.galacticraft.oxygen;

import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCModTags;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

/** Breathing and air-tightness rules (1.12.2 OxygenUtil and OxygenPressureProtocol). */
public final class OxygenUtil {
    private OxygenUtil() {
    }

    // ------------------------------------------------------------------ what lets air through

    /** Full blocks that seal: everything opaque except gravel, wool, sponge (tag) and leaves; plus glass and the sealable tag. */
    public static boolean isSealingFullBlock(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LeavesBlock || state.is(GCModTags.AIR_PERMEABLE)) {
            return false;
        }
        return state.isSolidRender(level, pos) || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(GCModTags.SEALABLE);
    }

    /**
     * 1.12.2 OxygenPressureProtocol#canBlockPassAir: can air move into {@code pos} when travelling in direction
     * {@code travel}? (the entry face is the one facing back the way the air came).
     */
    public static boolean canBlockPassAir(Level level, BlockState state, BlockPos pos, Direction travel) {
        if (state.isAir() || state.getBlock() instanceof LeavesBlock || state.is(GCModTags.AIR_PERMEABLE)) {
            return true;
        }
        if (isSealingFullBlock(level, pos, state)) {
            return false;
        }
        if (state.getBlock() instanceof SlabBlock && state.hasProperty(SlabBlock.TYPE)) {
            final SlabType type = state.getValue(SlabBlock.TYPE);
            return !(travel == Direction.DOWN && type == SlabType.TOP || travel == Direction.UP && type == SlabType.BOTTOM);
        }
        if (state.getBlock() instanceof FarmBlock || state.getBlock() instanceof EnchantingTableBlock || state.getBlock() instanceof LiquidBlock) {
            return travel != Direction.UP;
        }
        if (state.getBlock() instanceof PistonBaseBlock) {
            return state.getValue(PistonBaseBlock.EXTENDED) && travel != state.getValue(PistonBaseBlock.FACING);
        }
        return !state.isFaceSturdy(level, pos, travel.getOpposite());
    }

    /** 1.12.2 OxygenUtil#canBlockPassAirOnSide: can air leave this (non-air) block through {@code side}? */
    private static boolean canPassAirOut(Level level, BlockState state, BlockPos pos, Direction side) {
        if (state.getBlock() instanceof SlabBlock && state.hasProperty(SlabBlock.TYPE)) {
            final SlabType type = state.getValue(SlabBlock.TYPE);
            return !(side == Direction.DOWN && type == SlabType.BOTTOM || side == Direction.UP && type == SlabType.TOP);
        }
        if (state.getBlock() instanceof FarmBlock || state.getBlock() instanceof EnchantingTableBlock || state.getBlock() instanceof LiquidBlock) {
            return side != Direction.UP;
        }
        if (state.getBlock() instanceof PistonBaseBlock) {
            return state.getValue(PistonBaseBlock.EXTENDED) && side != state.getValue(PistonBaseBlock.FACING);
        }
        return !state.isFaceSturdy(level, pos, side);
    }

    // ------------------------------------------------------------------ breathing

    /** 1.12.2 OxygenUtil#isAABBInBreathableAirBlock(entity): a small box around the eyes. */
    public static boolean isInBreathableAir(LivingEntity entity) {
        final AABB box = entity.getBoundingBox();
        final double half = Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) / 2.0D;
        final double y = entity.getEyeY();
        return isInBreathableAir(entity.level(), new AABB(entity.getX() - half, y - half, entity.getZ() - half,
                entity.getX() + half, y + half, entity.getZ() + half));
    }

    public static boolean isInBreathableAir(Level level, AABB box) {
        if (inOxygenBubble(level, (box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D)) {
            return true;
        }
        final int x0 = Mth.floor(box.minX + 0.001D);
        final int x1 = Mth.floor(box.maxX - 0.001D);
        final int y0 = Mth.floor(box.minY + 0.001D);
        final int y1 = Mth.floor(box.maxY - 0.001D);
        final int z0 = Mth.floor(box.minZ + 0.001D);
        final int z1 = Mth.floor(box.maxZ - 0.001D);
        if (!level.hasChunksAt(x0, y0, z0, x1, y1, z1)) {
            return false;
        }
        final Set<BlockPos> checked = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            if (touchesBreathableAir(level, pos.immutable(), 0, checked)) {
                return true;
            }
        }
        return false;
    }

    /** Inside a running Oxygen Distributor's bubble. */
    public static boolean inOxygenBubble(Level level, double x, double y, double z) {
        for (OxygenDistributorBlockEntity distributor : OxygenDistributorBlockEntity.loaded(level)) {
            if (distributor.inBubble(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** 1.12.2 OxygenUtil#checkTorchHasOxygen: a torch passes air on every side. */
    public static boolean torchHasOxygen(Level level, BlockPos pos) {
        if (inOxygenBubble(level, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D)) {
            return true;
        }
        final Set<BlockPos> checked = new HashSet<>();
        checked.add(pos);
        for (Direction side : Direction.values()) {
            final BlockPos next = pos.relative(side);
            if (level.isLoaded(next) && touchesBreathableAir(level, next, 1, checked)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.12.2 OxygenUtil#testContactWithBreathableAir: breathable air itself, or an air-permeable block (torch, fence,
     * leaves...) with breathable air reachable through it within 5 steps. Plain air and sealing blocks are not.
     */
    private static boolean touchesBreathableAir(Level level, BlockPos pos, int depth, Set<BlockPos> checked) {
        checked.add(pos);
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BreathableAirBlock) {
            return true;
        }
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || isSealingFullBlock(level, pos, state)) {
            return false;
        }
        final boolean permeable = state.getBlock() instanceof LeavesBlock || state.is(GCModTags.AIR_PERMEABLE);
        if (depth < 5) {
            for (Direction side : Direction.values()) {
                if (permeable || canPassAirOut(level, state, pos, side)) {
                    final BlockPos next = pos.relative(side);
                    if (!checked.contains(next) && level.isLoaded(next) && touchesBreathableAir(level, next, depth + 1, checked)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ the player's oxygen setup

    public static boolean isTank(ItemStack stack) {
        return stack.getItem() instanceof OxygenTankItem || stack.getItem() instanceof InfiniteOxygenTankItem;
    }

    /** Oxygen left in a tank slot; an infinite tank counts as full. */
    public static int tankOxygen(ItemStack stack) {
        if (stack.getItem() instanceof InfiniteOxygenTankItem) {
            return 900;
        }
        return stack.getItem() instanceof OxygenTankItem ? OxygenTankItem.oxygen(stack) : 0;
    }

    public static int tankCapacity(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem tank) {
            return tank.capacity();
        }
        return stack.getItem() instanceof InfiniteOxygenTankItem ? 900 : 0;
    }

    /** 1.12.2 OxygenUtil#hasValidOxygenSetup: mask, gear and at least one tank. */
    public static boolean hasValidOxygenSetup(GCPlayerData data) {
        return GCPlayerData.accepts(GearSlot.MASK, data.gear(GearSlot.MASK))
                && GCPlayerData.accepts(GearSlot.GEAR, data.gear(GearSlot.GEAR))
                && (isTank(data.gear(GearSlot.LEFT_TANK)) || isTank(data.gear(GearSlot.RIGHT_TANK)));
    }

    /** 1.12.2 OxygenUtil#getDrainSpacing: 9 ticks per unit when a tank holds oxygen, else 0. */
    public static int drainSpacing(ItemStack tank1, ItemStack tank2) {
        return tankOxygen(tank1) > 0 || tankOxygen(tank2) > 0 ? 9 : 0;
    }
}
