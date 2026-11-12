/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.entities.EntityHangingSchematic and the schematic
 * order of api.recipe.SchematicRegistry). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A schematic hung on a wall like a 2 x 2 painting (1.12.2 EntityHangingSchematic): placed from a schematic item, it
 * drops that schematic again when broken (not for creative players). Abandoned asteroid bases hang the Moon Buggy and
 * Tier 2 Rocket schematics in their engineering rooms.
 */
public class HangingSchematicEntity extends HangingEntity {
    /** 1.12.2 SchematicRegistry's schematic items, in their registration order (the saved index). */
    public static final List<Supplier<? extends Item>> SCHEMATICS = List.of(GCItems.BUGGY_SCHEMATIC, GCItems.TIER_2_ROCKET_SCHEMATIC,
            GCItems.TIER_3_ROCKET_SCHEMATIC, GCItems.CARGO_ROCKET_SCHEMATIC, GCItems.ASTRO_MINER_SCHEMATIC);
    private static final EntityDataAccessor<Integer> DATA_SCHEMATIC = SynchedEntityData.defineId(HangingSchematicEntity.class,
            EntityDataSerializers.INT);
    private static final int SIZE = 2;

    public HangingSchematicEntity(EntityType<? extends HangingSchematicEntity> type, Level level) {
        super(type, level);
    }

    public HangingSchematicEntity(Level level, BlockPos pos, Direction facing, int schematic) {
        super(GCEntities.HANGING_SCHEMATIC.get(), level, pos);
        setSchematic(schematic);
        setDirection(facing);
    }

    /** The index in {@link #SCHEMATICS} of the schematic in {@code stack}, or -1. */
    public static int indexOf(ItemStack stack) {
        for (int i = 0; i < SCHEMATICS.size(); i++) {
            if (stack.is(SCHEMATICS.get(i).get())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SCHEMATIC, 0);
    }

    public int schematic() {
        return this.entityData.get(DATA_SCHEMATIC);
    }

    private void setSchematic(int schematic) {
        this.entityData.set(DATA_SCHEMATIC, Mth.clamp(schematic, 0, SCHEMATICS.size() - 1));
    }

    /** 1.12.2 SchematicRegistry#getSchematicItem (an unknown index gave the first). */
    public ItemStack schematicItem() {
        return new ItemStack(SCHEMATICS.get(schematic()).get());
    }

    /** The 32 x 32 pixel frame of a vanilla painting that size, against the block behind {@code pos}. */
    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        final Vec3 centre = Vec3.atCenterOf(pos).relative(direction, -0.46875D).relative(direction.getCounterClockWise(), 0.5D)
                .relative(Direction.UP, 0.5D);
        final Direction.Axis axis = direction.getAxis();
        return AABB.ofSize(centre, axis == Direction.Axis.X ? 0.0625D : SIZE, SIZE, axis == Direction.Axis.Z ? 0.0625D : SIZE);
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        if (level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (brokenEntity instanceof Player player && player.hasInfiniteMaterials()) {
                return;
            }
            spawnAtLocation(schematicItem());
        }
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("schem", schematic());
        tag.putByte("facing", (byte) this.direction.get2DDataValue());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setSchematic(tag.getInt("schem"));
        this.direction = Direction.from2DDataValue(tag.getByte("facing"));
        super.readAdditionalSaveData(tag);
        setDirection(this.direction);
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        setPos(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return schematicItem();
    }
}
