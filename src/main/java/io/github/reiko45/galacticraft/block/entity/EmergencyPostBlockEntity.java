/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityEmergencyBox). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.EmergencyPostBlock;
import io.github.reiko45.galacticraft.item.EmergencyKitItem;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 1.12.2 TileEntityEmergencyBox: the post's four doors (one open at a time, swinging 4 degrees a tick on the client),
 * which shut if something solid gets in their way; eight points of bright air six blocks round it (or one up); and every
 * fifteenth tick or so, mobs that can breathe in space within 14 blocks are sent further off.
 */
public class EmergencyPostBlockEntity extends BlockEntity {
    private static final float SPEED = 4.0F;
    /** Open doors: 1 north, 2 west, 4 south, 8 east (1.12.2 "open"). */
    private int open;
    private int cooldown;
    private boolean activated;
    private final Set<BlockPos> airToRestore = new HashSet<>();
    /** Client: the doors' angles, north, west, south, east. */
    private final float[] angles = new float[4];
    private final float[] lastAngles = new float[4];

    public EmergencyPostBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.EMERGENCY_POST.get(), pos, state);
    }

    private static int bit(Direction side) {
        return switch (side) {
            case NORTH -> 1;
            case WEST -> 2;
            case SOUTH -> 4;
            case EAST -> 8;
            default -> 0;
        };
    }

    public boolean isOpen(Direction side) {
        return (this.open & bit(side)) != 0;
    }

    public void tick() {
        if (!this.activated) {
            this.activated = true;
            if (!this.level.isClientSide) {
                setLightBlocks();
            }
        }
        if (this.level.isClientSide) {
            for (int i = 0; i < 4; i++) {
                final boolean isOpen = (this.open & 1 << i) != 0;
                this.lastAngles[i] = this.angles[i];
                this.angles[i] = Math.max(0.0F, Math.min(90.0F, this.angles[i] + (isOpen ? SPEED : -SPEED)));
            }
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        boolean changed = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (isOpen(side) && blocked(side)) {
                this.open &= ~bit(side);
                changed = true;
            }
        }
        if (changed) {
            sync();
        }
        if (this.level.random.nextInt(15) == 0) {
            scareMobs();
        }
    }

    /** 1.12.2: an open door shuts if a solid block, or a shape that crosses its swing, is in the way. */
    private boolean blocked(Direction side) {
        final BlockPos next = this.worldPosition.relative(side);
        final BlockState state = this.level.getBlockState(next);
        if (state.isAir()) {
            return false;
        }
        if (state.isCollisionShapeFullBlock(this.level, next)) {
            return true;
        }
        final VoxelShape swing = switch (side) {
            case NORTH -> Shapes.box(0.125D, 0.125D, 11 / 16D, 0.875D, 0.875D, 1.0D);
            case SOUTH -> Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 5 / 16D);
            case WEST -> Shapes.box(11 / 16D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
            default -> Shapes.box(0.0D, 0.125D, 0.125D, 5 / 16D, 0.875D, 0.875D);
        };
        return Shapes.joinIsNotEmpty(swing, state.getCollisionShape(this.level, next), net.minecraft.world.phys.shapes.BooleanOp.AND);
    }

    /** 1.12.2 scareMobs: mobs that breathe anywhere (the evolved ones), within 14 blocks, walk further off. */
    private void scareMobs() {
        final Vec3 centre = Vec3.atCenterOf(this.worldPosition);
        final List<PathfinderMob> mobs = this.level.getEntitiesOfClass(PathfinderMob.class, new AABB(this.worldPosition).inflate(14, 7, 14),
                mob -> mob instanceof Enemy && mob.getType().is(io.github.reiko45.galacticraft.registry.GCModTags.BREATHES_IN_SPACE));
        final Vec3 here = Vec3.atLowerCornerOf(this.worldPosition);
        for (PathfinderMob mob : mobs) {
            Vec3 target = DefaultRandomPos.getPosAway(mob, 12, 5, centre);
            if (target == null) {
                target = DefaultRandomPos.getPosAway(mob, 14, 7, centre);
            }
            if (target == null) {
                continue;
            }
            final double distanceNew = target.distanceToSqr(here);
            final double distanceNow = here.distanceToSqr(mob.position());
            if (distanceNew > distanceNow) {
                final var path = mob.getNavigation().getPath();
                final Vec3 old = path != null && !path.isDone() ? path.getNextEntityPos(mob) : null;
                if (old == null || distanceNow > old.distanceToSqr(here)) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, 1.1D);
                }
            }
        }
    }

    /** 1.12.2 click: open the side used, or take the kit out of the open one; a kit in hand goes in as it opens. */
    public void click(Player player, Direction side, boolean kitted) {
        if (side.getAxis() == Direction.Axis.Y) {
            return;
        }
        if (isOpen(side) && this.cooldown == 0) {
            if (kitted) {
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(GCItems.EMERGENCY_KIT.get()), 0);
                this.level.setBlock(this.worldPosition, getBlockState().setValue(EmergencyPostBlock.KIT, false), Block.UPDATE_ALL);
            }
            return;
        }
        final ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof EmergencyKitItem) {
            player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
            this.level.setBlock(this.worldPosition, getBlockState().setValue(EmergencyPostBlock.KIT, true), Block.UPDATE_ALL);
        }
        final boolean wasOpen = isOpen(side);
        this.open = wasOpen ? 0 : bit(side);
        this.cooldown = 12;
        sync();
    }

    /** 1.12.2 getAngleA-D: a door's angle, 0 shut to 90 open. */
    public float angle(Direction side, float partialTick) {
        final int i = Integer.numberOfTrailingZeros(bit(side));
        return Math.max(0.0F, Math.min(90.0F, this.angles[i] + (this.angles[i] - this.lastAngles[i]) * partialTick));
    }

    // ------------------------------------------------------------------ light

    /** 1.12.2 setLightBlocks: bright air at eight points round the post, six blocks out (or one block up). */
    private void setLightBlocks() {
        final int[][] points = {{-6, 0}, {6, 0}, {0, -6}, {0, 6}, {-5, -5}, {-5, 5}, {5, 5}, {5, -5}};
        for (int[] point : points) {
            final BlockPos pos = this.worldPosition.offset(point[0], 0, point[1]);
            if (!brighten(pos)) {
                brighten(pos.above());
            }
        }
    }

    private boolean brighten(BlockPos pos) {
        if (!this.level.isLoaded(pos)) {
            return false;
        }
        final BlockState state = this.level.getBlockState(pos);
        final BlockState bright;
        if (state.is(Blocks.AIR)) {
            bright = GCBlocks.BRIGHT_AIR.get().defaultBlockState();
        } else if (state.is(GCBlocks.BREATHABLE_AIR.get())) {
            bright = GCBlocks.BRIGHT_BREATHABLE_AIR.get().defaultBlockState();
        } else {
            return false;
        }
        this.level.setBlock(pos, bright, Block.UPDATE_CLIENTS);
        this.airToRestore.add(pos.immutable());
        setChanged();
        return true;
    }

    /** 1.12.2 revertAir: the bright air goes back to plain air when the post goes. */
    public void revertAir() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (BlockPos pos : this.airToRestore) {
            if (!this.level.isLoaded(pos)) {
                continue;
            }
            final BlockState state = this.level.getBlockState(pos);
            if (state.is(GCBlocks.BRIGHT_AIR.get())) {
                this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            } else if (state.is(GCBlocks.BRIGHT_BREATHABLE_AIR.get())) {
                this.level.setBlock(pos, GCBlocks.BREATHABLE_AIR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        this.airToRestore.clear();
    }

    public Set<BlockPos> brightAir() {
        return this.airToRestore;
    }

    // ------------------------------------------------------------------ saving and syncing

    private void sync() {
        setChanged();
        this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("open", this.open);
        final net.minecraft.nbt.ListTag air = new net.minecraft.nbt.ListTag();
        for (BlockPos pos : this.airToRestore) {
            air.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put("air", air);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.open = tag.getInt("open");
        this.airToRestore.clear();
        for (Tag pos : tag.getList("air", Tag.TAG_LONG)) {
            this.airToRestore.add(BlockPos.of(((LongTag) pos).getAsLong()));
        }
        this.activated = !this.airToRestore.isEmpty() || this.activated;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("open", this.open);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
