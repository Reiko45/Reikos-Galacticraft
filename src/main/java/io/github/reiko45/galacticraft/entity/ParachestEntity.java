package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.block.entity.ParachestBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Parachest (1.12.2 EntityParachest): a chest that floats down at 0.35 blocks/tick under the player's parachute colour
 * and becomes a Parachest block with the rocket's cargo and fuel where it lands (or the nearest free spot).
 */
public class ParachestEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(ParachestEntity.class, EntityDataSerializers.INT);
    private NonNullList<ItemStack> cargo = NonNullList.create();
    private int fuel;

    public ParachestEntity(EntityType<? extends ParachestEntity> type, Level level) {
        super(type, level);
    }

    public static ParachestEntity create(Level level, double x, double y, double z, NonNullList<ItemStack> cargo, int fuel, int color) {
        final ParachestEntity chest = new ParachestEntity(GCEntities.PARACHEST.get(), level);
        chest.setPos(x, y, z);
        chest.cargo = NonNullList.withSize(cargo.size(), ItemStack.EMPTY);
        for (int i = 0; i < cargo.size(); i++) {
            chest.cargo.set(i, cargo.get(i).copy());
        }
        chest.fuel = fuel;
        chest.entityData.set(COLOR, color);
        return chest;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0);
    }

    /** 1.12.2 parachute colour index (0 plain ... 15 yellow). */
    public int color() {
        return this.entityData.get(COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (onGround()) {
            land();
            return;
        }
        setDeltaMovement(0.0D, -0.35D, 0.0D);
        move(MoverType.SELF, getDeltaMovement());
    }

    private void land() {
        final BlockPos base = blockPosition();
        for (int i = 0; i < 100; i++) {
            if (tryPlace(base.above(i))) {
                return;
            }
        }
        for (int size = 1; size < 5; size++) {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-size, -size, -size), base.offset(size, size, size))) {
                if (tryPlace(pos.immutable())) {
                    return;
                }
            }
        }
        for (ItemStack stack : this.cargo) {
            this.level().addFreshEntity(new ItemEntity(this.level(), getX(), getY(), getZ(), stack));
        }
        discard();
    }

    private boolean tryPlace(BlockPos pos) {
        if (!this.level().isEmptyBlock(pos) || !this.level().getBlockState(pos.below()).isFaceSturdy(this.level(), pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }
        this.level().setBlockAndUpdate(pos, GCBlocks.PARACHEST.get().defaultBlockState()
                .setValue(io.github.reiko45.galacticraft.block.ParachestBlock.COLOR, Math.floorMod(color(), 16)));
        if (this.level().getBlockEntity(pos) instanceof ParachestBlockEntity chest) {
            chest.fill(this.cargo, this.fuel);
        }
        discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.cargo = NonNullList.withSize(tag.getInt("CargoSize"), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.cargo, this.level().registryAccess());
        this.fuel = tag.getInt("Fuel");
        this.entityData.set(COLOR, tag.getInt("Color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CargoSize", this.cargo.size());
        ContainerHelper.saveAllItems(tag, this.cargo, this.level().registryAccess());
        tag.putInt("Fuel", this.fuel);
        tag.putInt("Color", color());
    }
}
