package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.block.entity.DungeonSpawnerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A dungeon boss (1.12.2 EntityBossBase): it belongs to the boss room's spawner, shows a boss bar, leaves when nobody is
 * left in its room, and dies over 200 ticks of explosions and experience. Then it refills the dungeon's treasure chest
 * (its tier's loot plus one guaranteed schematic) and drops the key that opens that chest.
 */
public abstract class DungeonBossEntity extends Monster {
    public static final int DEATH_TICKS = 200;

    private final ServerBossEvent bossEvent;
    @Nullable
    private BlockPos spawnerPos;
    private int playersWithinLast;

    protected DungeonBossEntity(EntityType<? extends DungeonBossEntity> type, Level level, BossEvent.BossBarColor barColor) {
        super(type, level);
        this.bossEvent = (ServerBossEvent) new ServerBossEvent(getDisplayName(), barColor, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(false);
    }

    /** 1.12.2 getChestTier: the treasure chest (and key) tier. */
    public abstract int chestTier();

    /** 1.12.2 dropKey: the key that opens this boss's treasure chest. */
    protected abstract ItemStack key();

    /** 1.12.2 getGuaranteedLoot: one schematic, hidden in the refilled chest. */
    protected abstract ItemStack guaranteedLoot();

    public void setSpawner(BlockPos pos) {
        this.spawnerPos = pos;
    }

    @Nullable
    protected DungeonSpawnerBlockEntity spawner() {
        return this.spawnerPos != null && level().getBlockEntity(this.spawnerPos) instanceof DungeonSpawnerBlockEntity spawner ? spawner : null;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide) {
            checkRoom();
            this.bossEvent.setProgress(getHealth() / getMaxHealth());
        }
        super.aiStep();
    }

    /** 1.12.2 EntityBossBase#onLivingUpdate: everyone left the room, so the boss goes. */
    private void checkRoom() {
        final DungeonSpawnerBlockEntity spawner = spawner();
        final AABB room = spawner == null ? null : spawner.roomBounds();
        if (room == null || this.deathTime > 0) {
            return;
        }
        final int within = level().getEntitiesOfClass(Player.class, room, player -> !player.isSpectator()).size();
        if (within == 0 && this.playersWithinLast != 0) {
            for (Player player : level().getEntitiesOfClass(Player.class, room.inflate(11.0D))) {
                player.sendSystemMessage(Component.translatable("gui.galacticraftcore.skeleton_boss.message"));
            }
            spawner.onBossGone();
            discard();
            return;
        }
        this.playersWithinLast = within;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /** A boss's own sounds while dying (1.12.2 played them from each boss's onDeathUpdate). */
    protected void deathSound(int deathTime) {
    }

    /** 1.12.2 EntityBossBase#onDeathUpdate: 200 ticks of explosions and experience, then the reward. */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 180 && this.deathTime <= 200) {
            final float x = (this.random.nextFloat() - 0.5F) * getBbWidth();
            final float y = (this.random.nextFloat() - 0.5F) * (getBbHeight() / 2.0F);
            final float z = (this.random.nextFloat() - 0.5F) * getBbWidth();
            level().addParticle(ParticleTypes.EXPLOSION_EMITTER, getX() + x, getY() + 2.0D + y, getZ() + z, 0.0D, 0.0D, 0.0D);
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        deathSound(this.deathTime);
        if (this.deathTime >= 180 && this.deathTime % 5 == 0) {
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 10.0F, 1.0F);
        }
        if (this.deathTime > 150 && this.deathTime % 5 == 0) {
            ExperienceOrb.award(serverLevel, position(), 30);
        }
        if (this.deathTime >= DEATH_TICKS) {
            ExperienceOrb.award(serverLevel, position(), 20);
            rewardTreasure();
            final ItemEntity key = new ItemEntity(serverLevel, getX(), getY() + 0.5D, getZ(), key());
            key.setDeltaMovement(0.0D, -2.0D, 0.0D);
            key.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(key);
            final DungeonSpawnerBlockEntity spawner = spawner();
            if (spawner != null) {
                spawner.onBossDefeated();
            }
            remove(RemovalReason.KILLED);
        }
    }

    /** Fill the dungeon's treasure chest (or the closest chest of the boss's tier nearby) with loot and a schematic. */
    private void rewardTreasure() {
        TreasureChestBlockEntity chest = null;
        final DungeonSpawnerBlockEntity spawner = spawner();
        if (spawner != null && spawner.chestPos() != null && level().getBlockEntity(spawner.chestPos()) instanceof TreasureChestBlockEntity found) {
            chest = found;
        }
        if (chest == null) {
            chest = closestChest();
        }
        if (chest != null) {
            chest.refill(guaranteedLoot(), this.random.nextLong());
        }
    }

    /** 1.12.2 TileEntityTreasureChest#findClosest (within 32 blocks here, not 1,000). */
    @Nullable
    private TreasureChestBlockEntity closestChest() {
        TreasureChestBlockEntity best = null;
        double bestDistance = 100.0D * 100.0D;
        final BlockPos centre = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-32, -8, -32), centre.offset(32, 8, 32))) {
            if (level().getBlockEntity(pos) instanceof TreasureChestBlockEntity found && found.tier() == chestTier()) {
                final double distance = pos.distSqr(centre);
                if (distance < bestDistance) {
                    best = found;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.spawnerPos != null) {
            tag.put("SpawnerPos", NbtUtils.writeBlockPos(this.spawnerPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.spawnerPos = tag.contains("SpawnerPos") ? NbtUtils.readBlockPos(tag, "SpawnerPos").orElse(null) : null;
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }
}
