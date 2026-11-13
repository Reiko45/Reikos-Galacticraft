/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.tile.TileEntityScreen). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.block.DisplayScreenBlock;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.12.2 TileEntityScreen: one block of a Display Screen. Screens facing the same way next to each other join into one
 * whole rectangle of up to 9 blocks along a side (it must be whole, with no gaps, and may not cut into another joined
 * screen); right-clicking a screen joins it with one more neighbour (right, left, up or down, in that order) and shows
 * the next picture: 0 blank, 1 the Earth, 2 the solar system, 3 the Earth and its Moon, 4 a planet test, 5 telemetry.
 * The joining code is 1.12.2's; the client is sent the result (which 1.12.2's client worked out again itself).
 */
public class DisplayScreenBlockEntity extends BlockEntity {
    /** 1.12.2 GalacticraftRegistry.getMaxScreenTypes with the core screens and the telemetry text. */
    public static final int SCREEN_TYPES = 6;

    public int imageType;
    public boolean connectedUp;
    public boolean connectedDown;
    public boolean connectedLeft;
    public boolean connectedRight;
    public int connectionsUp;
    public int connectionsDown;
    public int connectionsLeft;
    public int connectionsRight;
    public boolean isMultiscreen;
    public int screenOffsetX;
    public int screenOffsetZ;
    private boolean doneClientUpdate;

    public DisplayScreenBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.DISPLAY_SCREEN.get(), pos, state);
    }

    private Direction facing() {
        return getBlockState().getValue(DisplayScreenBlock.FACING);
    }

    /** The other screen on {@code side}, if it faces the same way (1.12.2 compared the metadata). */
    @Nullable
    private DisplayScreenBlockEntity screenAt(BlockPos pos) {
        return this.level != null && this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof DisplayScreenBlockEntity screen
                && !screen.isRemoved() && screen.facing() == facing() ? screen : null;
    }

    private Direction left() {
        return facing().getCounterClockWise();
    }

    private Direction right() {
        return facing().getClockWise();
    }

    @Nullable
    private DisplayScreenBlockEntity setVerifiedConnectedUp() {
        final DisplayScreenBlockEntity up = screenAt(this.worldPosition.above());
        this.connectedUp = up != null;
        return up;
    }

    @Nullable
    private DisplayScreenBlockEntity setVerifiedConnectedDown() {
        final DisplayScreenBlockEntity down = screenAt(this.worldPosition.below());
        this.connectedDown = down != null;
        return down;
    }

    @Nullable
    private DisplayScreenBlockEntity setVerifiedConnectedLeft() {
        final DisplayScreenBlockEntity left = screenAt(this.worldPosition.relative(left()));
        this.connectedLeft = left != null;
        return left;
    }

    @Nullable
    private DisplayScreenBlockEntity setVerifiedConnectedRight() {
        final DisplayScreenBlockEntity right = screenAt(this.worldPosition.relative(right()));
        this.connectedRight = right != null;
        return right;
    }

    /** The screen at {@code x} to the right and {@code z} down from this one. */
    private BlockPos offset(int x, int z) {
        return this.worldPosition.relative(right(), x).relative(Direction.DOWN, z);
    }

    /** 1.12.2 breakScreen: this screen (broken or turned) leaves its big screen, and the rest re-forms. */
    public void breakScreen(BlockState state) {
        final int left = this.connectionsLeft;
        final int right = this.connectionsRight;
        final int up = this.connectionsUp;
        final int down = this.connectionsDown;
        final boolean doUp = this.connectedUp;
        final boolean doDown = this.connectedDown;
        final boolean doLeft = this.connectedLeft;
        final boolean doRight = this.connectedRight;
        for (int x = -left; x <= right; x++) {
            for (int z = -up; z <= down; z++) {
                if (x == 0 && z == 0) {
                    resetToSingle();
                } else {
                    final DisplayScreenBlockEntity screen = screenAt(offset(x, z));
                    if (screen != null) {
                        screen.resetToSingle();
                    }
                }
            }
        }
        this.connectedUp = this.connectedDown = this.connectedLeft = this.connectedRight = false;
        if (doUp) {
            final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.above());
            if (screen != null) {
                screen.connectedLeft |= doLeft;
                screen.connectedRight |= doRight;
                screen.connectedUp = true;
                screen.refreshConnections(true);
            }
        }
        if (doDown) {
            final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.below());
            if (screen != null) {
                screen.connectedLeft |= doLeft;
                screen.connectedRight |= doRight;
                screen.connectedDown = true;
                screen.refreshConnections(true);
            }
        }
        if (doLeft) {
            final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.relative(left()));
            if (screen != null) {
                screen.connectedUp |= doUp;
                screen.connectedDown |= doDown;
                screen.connectedLeft = true;
                screen.refreshConnections(true);
            }
        }
        if (doRight) {
            final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.relative(right()));
            if (screen != null) {
                screen.connectedUp |= doUp;
                screen.connectedDown |= doDown;
                screen.connectedRight = true;
                screen.refreshConnections(true);
            }
        }
    }

    /**
     * 1.12.2 refreshConnections: check each side this screen thinks it is joined on still is, and (with
     * {@code doScreen}) form the big screen. Can call itself through its neighbours.
     */
    public void refreshConnections(boolean doScreen) {
        if (this.level == null || this.level.isClientSide || isRemoved()) {
            return;
        }
        DisplayScreenBlockEntity up = null;
        DisplayScreenBlockEntity down = null;
        DisplayScreenBlockEntity left = null;
        DisplayScreenBlockEntity right = null;
        if (this.connectedUp) {
            up = setVerifiedConnectedUp();
        }
        if (this.connectedDown) {
            down = setVerifiedConnectedDown();
        }
        if (this.connectedLeft) {
            left = setVerifiedConnectedLeft();
        }
        if (this.connectedRight) {
            right = setVerifiedConnectedRight();
        }
        if (this.connectedUp) {
            this.connectedUp = tryConnectUp(up);
        }
        if (this.connectedDown) {
            this.connectedDown = tryConnectDown(down);
        }
        if (this.connectedLeft) {
            this.connectedLeft = tryConnectLeft(left);
        }
        if (this.connectedRight) {
            this.connectedRight = tryConnectRight(right);
        }
        if (doScreen) {
            checkScreenSize();
            sync();
        }
    }

    /** 1.12.2 changeChannel: the next picture, joining one more neighbour on the way. */
    public void changeChannel() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (++this.imageType >= SCREEN_TYPES) {
            this.imageType = 0;
        }
        if (!this.connectedRight && canJoinRight()) {
            joinRight();
        } else if (!this.connectedLeft && canJoinLeft()) {
            joinLeft();
        } else if (!this.connectedUp && canJoinUp()) {
            joinUp();
        } else if (!this.connectedDown && canJoinDown()) {
            joinDown();
        }
        this.doneClientUpdate = false;
        refreshConnections(true);
        if (!this.doneClientUpdate) {
            sync();
        }
    }

    /** 1.12.2 checkScreenSize: how far the screen reaches each way, then checkWholeScreen. */
    public void checkScreenSize() {
        int up = 0;
        int down = 0;
        int left = 0;
        int right = 0;
        DisplayScreenBlockEntity tile = this;
        BlockPos pos = this.worldPosition;
        while (up < 8) {
            if (!tile.connectedUp) {
                break;
            }
            up++;
            final DisplayScreenBlockEntity next = this.level.getBlockEntity(pos.above()) instanceof DisplayScreenBlockEntity found ? found : null;
            if (next == null) {
                this.connectedUp = false;
                up--;
                break;
            }
            tile = next;
            pos = pos.above();
        }
        tile = this;
        pos = this.worldPosition;
        while (down < 8 - up) {
            if (!tile.connectedDown) {
                break;
            }
            down++;
            final DisplayScreenBlockEntity next = this.level.getBlockEntity(pos.below()) instanceof DisplayScreenBlockEntity found ? found : null;
            if (next == null) {
                this.connectedDown = false;
                down--;
                break;
            }
            tile = next;
            pos = pos.below();
        }
        tile = this;
        pos = this.worldPosition;
        while (left < (up + down == 0 ? 1 : 8)) {
            if (!tile.connectedLeft) {
                break;
            }
            left++;
            final DisplayScreenBlockEntity next = this.level.getBlockEntity(pos.relative(left())) instanceof DisplayScreenBlockEntity found ? found : null;
            if (next == null) {
                this.connectedLeft = false;
                left--;
                break;
            }
            tile = next;
            pos = pos.relative(left());
        }
        tile = this;
        pos = this.worldPosition;
        while (right < (up + down == 0 ? 1 : 8) - left) {
            if (!tile.connectedRight) {
                break;
            }
            right++;
            final DisplayScreenBlockEntity next = this.level.getBlockEntity(pos.relative(right())) instanceof DisplayScreenBlockEntity found ? found : null;
            if (next == null) {
                this.connectedRight = false;
                right--;
                break;
            }
            tile = next;
            pos = pos.relative(right());
        }
        final DisplayScreenBlockEntity tileUp = this.level.getBlockEntity(this.worldPosition.above()) instanceof DisplayScreenBlockEntity f ? f : null;
        final DisplayScreenBlockEntity tileDown = this.level.getBlockEntity(this.worldPosition.below()) instanceof DisplayScreenBlockEntity f ? f : null;
        final DisplayScreenBlockEntity tileLeft = this.level.getBlockEntity(this.worldPosition.relative(left())) instanceof DisplayScreenBlockEntity f ? f : null;
        final DisplayScreenBlockEntity tileRight = this.level.getBlockEntity(this.worldPosition.relative(right())) instanceof DisplayScreenBlockEntity f ? f : null;
        // a single row or column of two: only join the pair
        if (left + right == 0 && up + down >= 1) {
            if (up > 0 && tileUp != null && !tileUp.connectedUp) {
                up = 1;
                down = 0;
            } else {
                up = 0;
                down = tileDown != null && !tileDown.connectedDown ? 1 : 0;
            }
        }
        if (up + down == 0 && left + right >= 1) {
            if (left > 0 && tileLeft != null && !tileLeft.connectedLeft) {
                if (right == 0 || tileRight == null || tileRight.connectionsLeft == 0) {
                    left = 1;
                    right = 0;
                } else {
                    left = 0;
                    right = 1;
                }
            } else {
                left = 0;
                right = tileRight != null && !tileRight.connectedRight ? 1 : 0;
            }
        }
        // (1.12.2 cleared the opposite flags on the screen above in each case, kept as it was)
        if (up == 0) {
            this.connectedUp = false;
            if (tileUp != null) {
                tileUp.connectedDown = false;
            }
        }
        if (down == 0) {
            this.connectedDown = false;
            if (tileUp != null) {
                tileUp.connectedUp = false;
            }
        }
        if (left == 0) {
            this.connectedLeft = false;
            if (tileUp != null) {
                tileUp.connectedRight = false;
            }
        }
        if (right == 0) {
            this.connectedRight = false;
            if (tileUp != null) {
                tileUp.connectedLeft = false;
            }
        }
        checkWholeScreen(up, down, left, right);
    }

    /**
     * 1.12.2 checkWholeScreen: is the rectangle {@code up}, {@code down}, {@code left}, {@code right} from here all
     * screens? Then it becomes one big screen showing this screen's picture; otherwise every screen in it goes back to
     * a single one. It shrinks to leave out any big screen it would cut into.
     */
    private boolean checkWholeScreen(int up, int down, int left, int right) {
        if (up + down + left + right == 0 || up < 0 || down < 0 || left < 0 || right < 0) {
            this.doneClientUpdate = true;
            resetToSingle();
            return true;
        }
        boolean whole = true;
        boolean existing = false;
        int barrierUp = up;
        int barrierDown = down;
        int barrierLeft = left;
        int barrierRight = right;
        final List<DisplayScreenBlockEntity> screens = new ArrayList<>();
        for (int x = -left; x <= right; x++) {
            for (int z = -up; z <= down; z++) {
                final DisplayScreenBlockEntity screen = x == 0 && z == 0 ? this : screenAt(offset(x, z));
                if (screen != null) {
                    screens.add(screen);
                    if (screen.isMultiscreen) {
                        if (screen.connectionsUp > z + up) {
                            barrierUp = -z - 1;
                            existing = true;
                        }
                        if (screen.connectionsDown > down - z) {
                            barrierDown = z - 1;
                            existing = true;
                        }
                        if (screen.connectionsLeft > x + left) {
                            barrierLeft = -x - 1;
                            existing = true;
                        }
                        if (screen.connectionsRight > right - x) {
                            barrierRight = x - 1;
                            existing = true;
                        }
                    }
                } else {
                    whole = false;
                }
            }
        }
        if (!whole) {
            for (DisplayScreenBlockEntity screen : screens) {
                screen.resetToSingle();
            }
            return false;
        }
        if (existing) {
            return checkWholeScreen(barrierUp, barrierDown, barrierLeft, barrierRight);
        }
        final Iterator<DisplayScreenBlockEntity> it = screens.iterator();
        for (int x = -left; x <= right; x++) {
            for (int z = -up; z <= down; z++) {
                final DisplayScreenBlockEntity screen = it.next();
                screen.screenOffsetX = x + left;
                screen.screenOffsetZ = z + up;
                screen.connectionsLeft = x + left;
                screen.connectionsRight = right - x;
                screen.connectionsUp = z + up;
                screen.connectionsDown = down - z;
                screen.isMultiscreen = true;
                screen.imageType = this.imageType;
                screen.sync();
                screen.refreshConnections(false);
            }
        }
        this.connectionsUp = up;
        this.connectionsDown = down;
        this.connectionsLeft = left;
        this.connectionsRight = right;
        return true;
    }

    /** 1.12.2 resetToSingle: a 1 x 1 screen again. */
    public void resetToSingle() {
        this.screenOffsetX = 0;
        this.screenOffsetZ = 0;
        this.connectionsUp = 0;
        this.connectionsDown = 0;
        this.connectionsLeft = 0;
        this.connectionsRight = 0;
        this.isMultiscreen = false;
        this.connectedUp = this.connectedDown = this.connectedLeft = this.connectedRight = false;
        sync();
    }

    private boolean canJoinRight() {
        final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.relative(right()));
        if (screen == null || screen.connectionsUp != this.connectionsUp || screen.connectionsDown != this.connectionsDown) {
            return false;
        }
        return this.connectionsUp + this.connectionsDown > 0 || this.connectionsLeft == 0 && screen.connectionsRight == 0;
    }

    private boolean canJoinLeft() {
        final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.relative(left()));
        if (screen == null || screen.connectionsUp != this.connectionsUp || screen.connectionsDown != this.connectionsDown) {
            return false;
        }
        return this.connectionsUp + this.connectionsDown > 0 || this.connectionsRight == 0 && screen.connectionsLeft == 0;
    }

    private boolean canJoinUp() {
        final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.above());
        if (screen == null || screen.connectionsLeft != this.connectionsLeft || screen.connectionsRight != this.connectionsRight) {
            return false;
        }
        return this.connectionsLeft + this.connectionsRight > 0 || this.connectionsDown == 0 && screen.connectionsUp == 0;
    }

    private boolean canJoinDown() {
        final DisplayScreenBlockEntity screen = screenAt(this.worldPosition.below());
        if (screen == null || screen.connectionsLeft != this.connectionsLeft || screen.connectionsRight != this.connectionsRight) {
            return false;
        }
        return this.connectionsLeft + this.connectionsRight > 0 || this.connectionsUp == 0 && screen.connectionsDown == 0;
    }

    private void joinRight() {
        for (int z = -this.connectionsUp; z <= this.connectionsDown; z++) {
            final BlockPos pos = offset(0, z);
            final DisplayScreenBlockEntity screen = z == 0 ? this : screenAt(pos);
            if (screen != null) {
                screen.connectedRight = true;
                final DisplayScreenBlockEntity other = screenAt(pos.relative(right()));
                if (other != null) {
                    screen.tryConnectRight(other);
                }
            }
        }
    }

    private void joinLeft() {
        for (int z = -this.connectionsUp; z <= this.connectionsDown; z++) {
            final BlockPos pos = offset(0, z);
            final DisplayScreenBlockEntity screen = z == 0 ? this : screenAt(pos);
            if (screen != null) {
                screen.connectedLeft = true;
                final DisplayScreenBlockEntity other = screenAt(pos.relative(left()));
                if (other != null) {
                    screen.tryConnectLeft(other);
                }
            }
        }
    }

    private void joinUp() {
        for (int x = -this.connectionsLeft; x <= this.connectionsRight; x++) {
            final BlockPos pos = offset(x, 0);
            final DisplayScreenBlockEntity screen = x == 0 ? this : screenAt(pos);
            if (screen != null) {
                screen.connectedUp = true;
                final DisplayScreenBlockEntity other = screenAt(pos.above());
                if (other != null) {
                    screen.tryConnectUp(other);
                }
            }
        }
    }

    private void joinDown() {
        for (int x = -this.connectionsLeft; x <= this.connectionsRight; x++) {
            final BlockPos pos = offset(x, 0);
            final DisplayScreenBlockEntity screen = x == 0 ? this : screenAt(pos);
            if (screen != null) {
                screen.connectedDown = true;
                final DisplayScreenBlockEntity other = screenAt(pos.below());
                if (other != null) {
                    screen.tryConnectDown(other);
                }
            }
        }
    }

    private boolean tryConnectUp(@Nullable DisplayScreenBlockEntity screen) {
        if (screen == null) {
            return false;
        }
        if (screen.connectedDown) {
            return true;
        }
        screen.connectedDown = true;
        if (this.connectedLeft) {
            screen.setVerifiedConnectedLeft();
        }
        if (this.connectedRight) {
            screen.setVerifiedConnectedRight();
        }
        screen.refreshConnections(false);
        if (this.connectedLeft ^ screen.connectedLeft || this.connectedRight ^ screen.connectedRight) {
            screen.connectedDown = false;
            return false;
        }
        return true;
    }

    private boolean tryConnectDown(@Nullable DisplayScreenBlockEntity screen) {
        if (screen == null) {
            return false;
        }
        if (screen.connectedUp) {
            return true;
        }
        screen.connectedUp = true;
        if (this.connectedLeft) {
            screen.setVerifiedConnectedLeft();
        }
        if (this.connectedRight) {
            screen.setVerifiedConnectedRight();
        }
        screen.refreshConnections(false);
        if (this.connectedLeft ^ screen.connectedLeft || this.connectedRight ^ screen.connectedRight) {
            screen.connectedUp = false;
            return false;
        }
        return true;
    }

    private boolean tryConnectLeft(@Nullable DisplayScreenBlockEntity screen) {
        if (screen == null) {
            return false;
        }
        if (screen.connectedRight) {
            return true;
        }
        if (screen.connectedUp && !this.connectedUp || screen.connectedDown && !this.connectedDown) {
            return false;
        }
        screen.connectedRight = true;
        if (this.connectedUp) {
            screen.setVerifiedConnectedUp();
        }
        if (this.connectedDown) {
            screen.setVerifiedConnectedDown();
        }
        screen.refreshConnections(false);
        if (this.connectedUp ^ screen.connectedUp || this.connectedDown ^ screen.connectedDown) {
            screen.connectedRight = false;
            return false;
        }
        return true;
    }

    private boolean tryConnectRight(@Nullable DisplayScreenBlockEntity screen) {
        if (screen == null) {
            return false;
        }
        if (screen.connectedLeft) {
            return true;
        }
        if (screen.connectedUp && !this.connectedUp || screen.connectedDown && !this.connectedDown) {
            return false;
        }
        screen.connectedLeft = true;
        if (this.connectedUp) {
            screen.setVerifiedConnectedUp();
        }
        if (this.connectedDown) {
            screen.setVerifiedConnectedDown();
        }
        screen.refreshConnections(false);
        if (this.connectedUp ^ screen.connectedUp || this.connectedDown ^ screen.connectedDown) {
            screen.connectedLeft = false;
            return false;
        }
        return true;
    }

    /** Save, show the frame edges in the block state and tell the clients (1.12.2 markDirty + updateAllInDimension). */
    private void sync() {
        if (this.level == null || this.level.isClientSide || isRemoved()) {
            return;
        }
        setChanged();
        final BlockState state = getBlockState();
        final BlockState edges = state.setValue(DisplayScreenBlock.UP, this.connectedUp).setValue(DisplayScreenBlock.DOWN, this.connectedDown)
                .setValue(DisplayScreenBlock.LEFT, this.connectedLeft).setValue(DisplayScreenBlock.RIGHT, this.connectedRight);
        if (edges != state && this.level.getBlockState(this.worldPosition) == state) {
            this.level.setBlock(this.worldPosition, edges, Block.UPDATE_CLIENTS); // no neighbour updates: they would refresh again
        } else {
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Whether this block draws the picture: the top left one of a big screen, or a single screen. */
    public boolean drawsScreen() {
        return !this.isMultiscreen || this.connectionsLeft == 0 && this.connectionsUp == 0;
    }

    public int width() {
        return this.isMultiscreen ? 1 + this.connectionsLeft + this.connectionsRight : 1;
    }

    public int height() {
        return this.isMultiscreen ? 1 + this.connectionsUp + this.connectionsDown : 1;
    }

    /** The whole big screen, for render culling. */
    public AABB renderBox() {
        final BlockPos far = offset(width() - 1, height() - 1);
        return new AABB(Math.min(this.worldPosition.getX(), far.getX()), Math.min(this.worldPosition.getY(), far.getY()),
                Math.min(this.worldPosition.getZ(), far.getZ()), Math.max(this.worldPosition.getX(), far.getX()) + 1,
                Math.max(this.worldPosition.getY(), far.getY()) + 1, Math.max(this.worldPosition.getZ(), far.getZ()) + 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("type", this.imageType);
        tag.putInt("connectionsDown", this.connectionsDown);
        tag.putInt("connectionsUp", this.connectionsUp);
        tag.putInt("connectionsLeft", this.connectionsLeft);
        tag.putInt("connectionsRight", this.connectionsRight);
        tag.putBoolean("multiscreen", this.isMultiscreen);
        tag.putBoolean("connectedUp", this.connectedUp);
        tag.putBoolean("connectedDown", this.connectedDown);
        tag.putBoolean("connectedLeft", this.connectedLeft);
        tag.putBoolean("connectedRight", this.connectedRight);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.imageType = tag.getInt("type");
        this.connectionsDown = tag.getInt("connectionsDown");
        this.connectionsUp = tag.getInt("connectionsUp");
        this.connectionsLeft = tag.getInt("connectionsLeft");
        this.connectionsRight = tag.getInt("connectionsRight");
        this.isMultiscreen = tag.getBoolean("multiscreen");
        this.screenOffsetX = this.connectionsLeft;
        this.screenOffsetZ = this.connectionsUp;
        // 1.12.2 worked the flags out from the counts
        this.connectedUp = tag.contains("connectedUp") ? tag.getBoolean("connectedUp") : this.connectionsUp > 0;
        this.connectedDown = tag.contains("connectedDown") ? tag.getBoolean("connectedDown") : this.connectionsDown > 0;
        this.connectedLeft = tag.contains("connectedLeft") ? tag.getBoolean("connectedLeft") : this.connectionsLeft > 0;
        this.connectedRight = tag.contains("connectedRight") ? tag.getBoolean("connectedRight") : this.connectionsRight > 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
