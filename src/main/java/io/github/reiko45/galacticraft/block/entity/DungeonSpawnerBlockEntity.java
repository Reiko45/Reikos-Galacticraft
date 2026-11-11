package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.entity.SkeletonBossEntity;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2 TileEntityDungeonSpawner (TileEntityDungeonSpawner&lt;EntitySkeletonBoss&gt;): the invisible block in the
 * middle of the boss room. When a player steps into the room it summons the Skeleton Boss (unless it has been
 * beaten in the last 15 minutes), keeps other monsters out of the room, and tells the boss where the treasure is.
 */
public class DungeonSpawnerBlockEntity extends BlockEntity {
    /** 1.12.2: the boss can be fought again 15 minutes after it was killed. */
    private static final long RESPAWN_MILLIS = 900_000L;

    @Nullable
    private BoundingBox room;
    @Nullable
    private BlockPos chestPos;
    private boolean spawned;
    private boolean bossDefeated;
    private long lastKillTime;
    private boolean lastPlayerInRange;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.DUNGEON_SPAWNER.get(), pos, state);
    }

    public void setRoom(BoundingBox room, @Nullable BlockPos chestPos) {
        this.room = room;
        this.chestPos = chestPos;
        setChanged();
    }

    @Nullable
    public BlockPos chestPos() {
        return this.chestPos;
    }

    @Nullable
    public AABB roomBounds() {
        return this.room == null ? null : AABB.of(this.room);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DungeonSpawnerBlockEntity spawner) {
        final AABB room = spawner.roomBounds();
        if (room == null) {
            return;
        }
        if (spawner.lastKillTime > 0 && System.currentTimeMillis() - spawner.lastKillTime > RESPAWN_MILLIS) {
            spawner.lastKillTime = 0;
            spawner.bossDefeated = false;
        }
        final List<SkeletonBossEntity> bosses = level.getEntitiesOfClass(SkeletonBossEntity.class, new AABB(pos).inflate(15.0D), SkeletonBossEntity::isAlive);
        for (SkeletonBossEntity boss : bosses) {
            boss.setSpawner(pos);
            spawner.spawned = true;
            spawner.bossDefeated = false;
        }
        if (bosses.isEmpty()) {
            spawner.spawned = false;
        }
        // 1.12.2 getDisabledCreatures: no other monsters in the boss fight
        for (Monster mob : level.getEntitiesOfClass(Monster.class, room.inflate(3.0D), mob -> !(mob instanceof SkeletonBossEntity)
                && DISABLED.contains(mob.getType()))) {
            mob.discard();
        }
        final boolean playerInRange = !level.getEntitiesOfClass(Player.class, room, player -> !player.isSpectator()).isEmpty();
        if (playerInRange && !spawner.lastPlayerInRange && !spawner.spawned && !spawner.bossDefeated && level instanceof ServerLevel serverLevel) {
            final SkeletonBossEntity boss = GCEntities.SKELETON_BOSS.get().create(serverLevel);
            if (boss != null) {
                boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                boss.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
                boss.setSpawner(pos);
                serverLevel.addFreshEntity(boss);
                serverLevel.playSound(null, pos, GCSounds.BOSS_LAUGH.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
                spawner.spawned = true;
            }
        }
        spawner.lastPlayerInRange = playerInRange;
    }

    private static final Set<EntityType<?>> DISABLED = Set.of(GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SKELETON.get(),
            GCEntities.EVOLVED_SPIDER.get(), GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_ENDERMAN.get(),
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER, EntityType.ENDERMAN);

    /** The boss died (it fills the treasure chest itself). */
    public void onBossDefeated() {
        this.bossDefeated = true;
        this.spawned = false;
        this.lastKillTime = System.currentTimeMillis();
        setChanged();
    }

    /** The boss despawned because everyone left (1.12.2 EntityBossBase#setDead). */
    public void onBossGone() {
        this.bossDefeated = false;
        this.spawned = false;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.room != null) {
            tag.putIntArray("Room", new int[]{this.room.minX(), this.room.minY(), this.room.minZ(), this.room.maxX(), this.room.maxY(), this.room.maxZ()});
        }
        if (this.chestPos != null) {
            tag.put("ChestPos", NbtUtils.writeBlockPos(this.chestPos));
        }
        tag.putBoolean("IsBossDefeated", this.bossDefeated);
        tag.putLong("LastKillTime", this.lastKillTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final int[] room = tag.getIntArray("Room");
        this.room = room.length == 6 ? new BoundingBox(room[0], room[1], room[2], room[3], room[4], room[5]) : null;
        this.chestPos = tag.contains("ChestPos") ? NbtUtils.readBlockPos(tag, "ChestPos").orElse(null) : null;
        this.bossDefeated = tag.getBoolean("IsBossDefeated");
        this.lastKillTime = tag.getLong("LastKillTime");
    }
}
