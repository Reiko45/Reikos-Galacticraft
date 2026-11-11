/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.entities.EntityCreeperBoss). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import io.github.reiko45.galacticraft.GCConfig;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Creeper Boss (1.12.2 EntityCreeperBoss), the guardian of Mars dungeons: a 7-block, three-headed creeper with 200
 * health that lobs TNT every 25 ticks at players within 20 blocks. Nothing hurts it but fireball damage, so the way to
 * beat it is to punch its own TNT back at it. It loses a head at two thirds and at one third of its health. Beaten, it
 * refills the Tier 2 Treasure Chest (with a Tier 3 Rocket or Cargo Rocket schematic) and drops the Tier 2 Dungeon Key.
 */
public class CreeperBossEntity extends DungeonBossEntity implements RangedAttackMob {
    public static final double MAX_HEALTH = 200.0D;

    private int headsRemaining = 3;

    public CreeperBossEntity(EntityType<? extends CreeperBossEntity> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH).add(Attributes.MOVEMENT_SPEED, 0.05D)
                .add(Attributes.FOLLOW_RANGE, 40.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 25, 20.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    /** 1.12.2 applyEntityAttributes: 200 health times the dungeon boss health config. */
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        final var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(MAX_HEALTH * GCConfig.DUNGEON_BOSS_HEALTH_MOD.get());
            setHealth(getMaxHealth());
        }
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    public int chestTier() {
        return 2;
    }

    @Override
    protected ItemStack key() {
        return new ItemStack(GCItems.TIER_2_DUNGEON_KEY.get());
    }

    /** 1.12.2 attackEntityFrom: only fireball damage (its own TNT, punched back) hurts it. /kill still works. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypes.FIREBALL) && !source.is(DamageTypes.UNATTRIBUTED_FIREBALL)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /**
     * 1.12.2's fire immunity kept it out of fire and lava but not off fireball damage, which 1.21 counts as fire: let
     * fireballs through.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.FIREBALL) || source.is(DamageTypes.UNATTRIBUTED_FIREBALL)) {
            return isRemoved() || isInvulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer();
        }
        return super.isInvulnerableTo(source);
    }

    /** How many heads it has left: 3, then 2 at two thirds of its health, 1 at one third, none once dead. */
    public int headsRemaining() {
        return this.headsRemaining;
    }

    /** 1.12.2 onLivingUpdate (both sides, from the synced health; heads never grow back). */
    @Override
    public void aiStep() {
        if (getHealth() <= 0.0F) {
            this.headsRemaining = 0;
        } else if (getHealth() <= getMaxHealth() / 3.0F) {
            this.headsRemaining = 1;
        } else if (getHealth() <= 2.0F * (getMaxHealth() / 3.0F)) {
            this.headsRemaining = Math.min(this.headsRemaining, 2);
        }
        super.aiStep();
    }

    /** 1.12.2 attackEntityWithRangedAttack: the wither's shooting sound and a TNT from 5.5 blocks up. */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        level().levelEvent(null, LevelEvent.SOUND_WITHER_BOSS_SHOOT, blockPosition(), 0);
        final double x = getX();
        final double y = getY() + 5.5D;
        final double z = getZ();
        final Vec3 aim = new Vec3(target.getX() - x, target.getY() + target.getEyeHeight() * 0.5D - y, target.getZ() - z).scale(0.5D);
        level().addFreshEntity(new TntProjectileEntity(level(), this, x, y, z, aim));
    }

    /**
     * 1.12.2 getGuaranteedLoot: GalacticraftRegistry#getDungeonLoot(2) is the Cargo Rocket, Tier 3 Rocket and Astro Miner
     * schematics. Schematics the nearest player has already unlocked are taken out (but never both, so there is still a
     * choice), and until they have the Tier 3 Rocket only the first two can come: a half chance of it.
     */
    @Override
    protected ItemStack guaranteedLoot() {
        final Player player = level().getNearestPlayer(this, 20.0D);
        final boolean hasT3Rocket = player != null && GCPlayerData.get(player).hasSchematic("rocket_t3");
        final boolean hasAstroMiner = player != null && GCPlayerData.get(player).hasSchematic("astro_miner");
        return guaranteedLoot(this.random, hasT3Rocket, hasAstroMiner);
    }

    public static ItemStack guaranteedLoot(RandomSource random, boolean hasT3Rocket, boolean hasAstroMiner) {
        final List<ItemStack> loot = new ArrayList<>(List.of(new ItemStack(GCItems.CARGO_ROCKET_SCHEMATIC.get()),
                new ItemStack(GCItems.TIER_3_ROCKET_SCHEMATIC.get()), new ItemStack(GCItems.ASTRO_MINER_SCHEMATIC.get())));
        if (hasT3Rocket && hasAstroMiner) {
            loot.remove(1 + random.nextInt(2));
        } else if (hasT3Rocket) {
            loot.remove(1);
        } else if (hasAstroMiner) {
            loot.remove(2);
        }
        final int range = hasT3Rocket ? loot.size() : 2;
        return loot.get(random.nextInt(range)).copy();
    }

    /** 1.12.2 sent C_PLAY_SOUND_BOSS_DEATH on the first tick of dying. */
    @Override
    protected void deathSound(int deathTime) {
        if (deathTime == 1) {
            playSound(GCSounds.BOSS_DEATH.get(), 10.0F, super.getVoicePitch() - 0.1F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return GCSounds.BOSS_OUCH.get();
    }

    /** 1.12.2 played its hurt sound 0.15 lower than usual. */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() - 0.15F;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
