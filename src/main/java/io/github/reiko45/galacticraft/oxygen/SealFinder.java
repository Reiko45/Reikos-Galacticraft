package io.github.reiko45.galacticraft.oxygen;

import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCModTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The seal check (1.12.2 ThreadFindSeal, run on the server thread instead of its own thread).
 *
 * <p>A flood fill starts at the block above the head sealer's vent and spreads through air, breathable air and
 * air-permeable blocks (torches, fences, leaves, the open sides of slabs...). Every block it looks at costs one check;
 * each sealer whose vent opens into the space adds 1,250 checks. The space is sealed if the fill runs out of places to
 * go before it runs out of checks. A sealed space is filled with breathable air and its torches light up; an unsealed
 * one loses its breathable air.
 */
public final class SealFinder {
    public static final int CHECKS_PER_SEALER = 1250;
    private static final int DOWN = Direction.DOWN.get3DDataValue();
    private static final int UP = Direction.UP.get3DDataValue();

    private final Level level;
    private final BlockPos head;
    private final Set<Long> checked = new HashSet<>();
    private final List<OxygenSealerBlockEntity> sealers = new ArrayList<>();
    private final List<BlockPos> airToReplace = new ArrayList<>();
    private final List<BlockPos> breathable = new ArrayList<>();
    private final List<BlockPos> torches = new ArrayList<>();
    private final ArrayDeque<Node> queue = new ArrayDeque<>();
    /** The side each queued block was entered by, to trace a leak back to the sealer. */
    private final Map<Long, Integer> enteredBy = new HashMap<>();
    @Nullable
    private List<BlockPos> leakTrace;
    private int checks;
    private boolean sealed;
    private boolean thermal;

    private record Node(BlockPos pos, int doneSides) {
    }

    private SealFinder(Level level, BlockPos head) {
        this.level = level;
        this.head = head;
    }

    /** Run a check for {@code sealer} and apply the result to the world and to every sealer that took part. */
    public static boolean check(OxygenSealerBlockEntity sealer) {
        final SealFinder finder = new SealFinder(sealer.getLevel(), sealer.getBlockPos().above());
        finder.run(sealer);
        return finder.sealed;
    }

    public List<OxygenSealerBlockEntity> sealers() {
        return this.sealers;
    }

    private void run(OxygenSealerBlockEntity headSealer) {
        this.sealers.add(headSealer);
        this.checks = headSealer.findSealChecks();
        this.thermal = headSealer.thermalControlEnabled();
        this.sealed = this.checks > 0;
        if (this.sealed) {
            final BlockState headState = this.level.getBlockState(this.head);
            if (headState.getBlock() instanceof BreathableAirBlock) {
                this.breathable.add(this.head);
            } else if (headState.isAir()) {
                this.airToReplace.add(this.head);
            }
            this.checked.add(this.head.asLong());
            this.queue.add(new Node(this.head, 0));
            spread();
        }
        if (this.sealed) {
            makeSealGood();
        } else {
            unseal(this.level, this.head, CHECKS_PER_SEALER * 4 * Math.max(1, this.sealers.size()));
        }
        for (OxygenSealerBlockEntity sealer : this.sealers) {
            sealer.onSealChecked(this.sealed, sealer == headSealer);
        }
        headSealer.setLeakTrace(this.sealed ? null : this.leakTrace);
    }

    private void spread() {
        while (this.sealed && !this.queue.isEmpty()) {
            final Node node = this.queue.poll();
            for (int side = 0; side < 6; side++) {
                if ((node.doneSides() & (1 << side)) != 0) {
                    continue;
                }
                final Direction travel = Direction.from3DDataValue(side);
                final BlockPos next = node.pos().relative(travel);
                if (this.checked.contains(next.asLong())) {
                    continue;
                }
                if (this.checks > 0) {
                    this.checks--;
                    if (!this.level.isInWorldBounds(next)) {
                        // Out into the void or above the build limit
                        this.sealed = false;
                        traceLeak(node.pos(), next);
                        return;
                    }
                    if (!this.level.isLoaded(next)) {
                        continue; // unloaded chunks count as solid, like 1.12.2
                    }
                    final BlockState state = this.level.getBlockState(next);
                    if (state.getBlock() instanceof BreathableAirBlock) {
                        this.checked.add(next.asLong());
                        this.queue.add(new Node(next, 0));
                        this.enteredBy.put(next.asLong(), side);
                        this.breathable.add(next);
                    } else if (state.isAir()) {
                        this.checked.add(next.asLong());
                        this.queue.add(new Node(next, 0));
                        this.enteredBy.put(next.asLong(), side);
                        this.airToReplace.add(next);
                    } else if (state.is(GCBlocks.OXYGEN_SEALER.get())) {
                        // Another sealer whose vent (top) opens into this space joins in
                        if (side == DOWN && this.level.getBlockEntity(next) instanceof OxygenSealerBlockEntity other && !this.sealers.contains(other)) {
                            this.checked.add(next.asLong());
                            this.sealers.add(other);
                            this.checks += other.findSealChecks();
                            this.thermal |= other.thermalControlEnabled();
                        }
                    } else {
                        final Node passed = enter(state, next, side, travel);
                        if (passed != null) {
                            this.queue.add(passed);
                            this.enteredBy.put(next.asLong(), side);
                        }
                    }
                } else {
                    // No checks left: any opening further out means the space is too big or open
                    if (!this.level.isInWorldBounds(next)) {
                        this.sealed = false;
                        traceLeak(node.pos(), next);
                        return;
                    }
                    final BlockState state = this.level.getBlockState(next);
                    if (state.isAir() || OxygenUtil.canBlockPassAir(this.level, state, next, travel)) {
                        this.sealed = false;
                        traceLeak(node.pos(), next);
                        return;
                    }
                }
            }
        }
    }

    /**
     * 1.12.2 traceLeak: the way from the sealer's vent to where the air gets out, at most 90 blocks, which Sensor Glasses
     * show as a stream of oxygen. (1.12.2 started its trace one step the wrong way, from the last block inside back
     * along the leak's direction, so it seldom found a path; this follows the blocks' own way in.)
     */
    private void traceLeak(BlockPos last, BlockPos opening) {
        final List<BlockPos> route = new ArrayList<>();
        route.add(opening);
        BlockPos pos = last;
        while (!pos.equals(this.head)) {
            route.add(pos);
            final Integer side = this.enteredBy.get(pos.asLong());
            if (side == null || route.size() > 90) {
                return;
            }
            pos = pos.relative(Direction.from3DDataValue(side).getOpposite());
        }
        route.add(this.head);
        Collections.reverse(route);
        this.leakTrace = route;
    }

    /** 1.12.2 canBlockPassAirCheck: can air move into this non-air block, and which of its sides are closed? */
    private Node enter(BlockState state, BlockPos pos, int side, Direction travel) {
        if (state.getBlock() instanceof PartialSealable partial) {
            if (partial.isSealed(this.level, pos, state, travel)) {
                return null; // may still be reachable from another side
            }
            // the faces it seals aren't passed through on the way out
            int done = 0;
            for (Direction face : Direction.values()) {
                if (face != travel && partial.isSealed(this.level, pos, state, face)) {
                    done |= 1 << face.getOpposite().get3DDataValue();
                }
            }
            this.checked.add(pos.asLong());
            return new Node(pos, done);
        }
        if (state.getBlock() instanceof LeavesBlock || state.is(GCModTags.AIR_PERMEABLE)) {
            this.checked.add(pos.asLong());
            return new Node(pos, 0);
        }
        if (state.getBlock() instanceof OxygenReliantBlock) {
            this.torches.add(pos);
            this.checked.add(pos.asLong());
            return new Node(pos, 0);
        }
        if (OxygenUtil.isSealingFullBlock(this.level, pos, state)) {
            this.checked.add(pos.asLong());
            return null;
        }
        if (state.getBlock() instanceof SlabBlock && state.hasProperty(SlabBlock.TYPE)) {
            final boolean top = state.getValue(SlabBlock.TYPE) == SlabType.TOP;
            if (side == DOWN && top || side == UP && !top) {
                return null; // may still be reachable from another side
            }
            this.checked.add(pos.asLong());
            return new Node(pos, 1 << (top ? UP : DOWN));
        }
        if (state.getBlock() instanceof FarmBlock || state.getBlock() instanceof EnchantingTableBlock || state.getBlock() instanceof LiquidBlock) {
            if (side == UP) {
                return null;
            }
            this.checked.add(pos.asLong());
            return new Node(pos, 1 << DOWN);
        }
        if (state.getBlock() instanceof PistonBaseBlock) {
            if (state.getValue(PistonBaseBlock.EXTENDED)) {
                final Direction facing = state.getValue(PistonBaseBlock.FACING);
                if (travel == facing) {
                    return null;
                }
                this.checked.add(pos.asLong());
                return new Node(pos, 1 << facing.getOpposite().get3DDataValue());
            }
            this.checked.add(pos.asLong());
            return null;
        }
        if (state.isFaceSturdy(this.level, pos, travel.getOpposite())) {
            if (state.blocksMotion() && state.isCollisionShapeFullBlock(this.level, pos)) {
                this.checked.add(pos.asLong());
            }
            return null;
        }
        // Partly open block (stairs, fences, doors...): air may pass, but not out through its solid faces
        int done = 0;
        for (int i = 0; i < 6; i++) {
            if (i != (side ^ 1) && state.isFaceSturdy(this.level, pos, Direction.from3DDataValue(i))) {
                done |= 1 << i;
            }
        }
        this.checked.add(pos.asLong());
        return new Node(pos, done);
    }

    private void makeSealGood() {
        final BlockState air = GCBlocks.BREATHABLE_AIR.get().defaultBlockState().setValue(BreathableAirBlock.THERMAL, this.thermal);
        for (BlockPos pos : this.airToReplace) {
            this.level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (BlockPos pos : this.breathable) {
            final BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof BreathableAirBlock && state.getValue(BreathableAirBlock.THERMAL) != this.thermal) {
                this.level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        for (BlockPos pos : this.torches) {
            final BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() instanceof OxygenReliantBlock reliant) {
                reliant.onOxygenAdded(this.level, pos, state);
            }
        }
    }

    /**
     * Turn the breathable air connected to {@code start} back into plain air (1.12.2 unseal), following it through
     * air-permeable blocks, and let the torches in it re-check. Returns the number of blocks cleared.
     */
    public static int unseal(Level level, BlockPos start, int limit) {
        final Set<Long> seen = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start.asLong());
        int cleared = 0;
        while (!queue.isEmpty() && seen.size() < limit) {
            final BlockPos pos = queue.poll();
            if (!level.isLoaded(pos)) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BreathableAirBlock) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                cleared++;
            } else if (state.getBlock() instanceof OxygenReliantBlock reliant) {
                reliant.onOxygenRemoved(level, pos, state);
            } else if (!(pos.equals(start) || state.getBlock() instanceof LeavesBlock || state.is(GCModTags.AIR_PERMEABLE)
                    || !state.isAir() && !OxygenUtil.isSealingFullBlock(level, pos, state) && !state.isCollisionShapeFullBlock(level, pos))) {
                continue;
            }
            for (Direction side : Direction.values()) {
                final BlockPos next = pos.relative(side);
                if (level.isInWorldBounds(next) && seen.add(next.asLong())) {
                    final BlockState nextState = level.getBlockState(next);
                    if (nextState.getBlock() instanceof BreathableAirBlock || nextState.getBlock() instanceof OxygenReliantBlock
                            || !nextState.isAir() && OxygenUtil.canBlockPassAir(level, nextState, next, side)) {
                        queue.add(next);
                    }
                }
            }
        }
        return cleared;
    }
}
