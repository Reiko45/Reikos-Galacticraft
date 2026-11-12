package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper;
import io.github.reiko45.galacticraft.entity.monster.EvolvedEnderman;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSkeleton;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSpider;
import io.github.reiko45.galacticraft.entity.monster.EvolvedZombie;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
import io.github.reiko45.galacticraft.entity.SkeletonBossEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types (sizes from 1.12.2 setSize; tracked far and often so flights look smooth). */
public final class GCEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, GalacticraftCore.MOD_ID);

    public static final Supplier<EntityType<Tier1RocketEntity>> TIER_1_ROCKET = ENTITY_TYPES.register("tier_1_rocket",
            () -> EntityType.Builder.<Tier1RocketEntity>of(Tier1RocketEntity::new, MobCategory.MISC).sized(1.2F, 3.5F)
                    .clientTrackingRange(16).updateInterval(1).fireImmune().build("tier_1_rocket"));
    /** 1.12.2 EntityTier2Rocket setSize(1.2, 4.5). */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.Tier2RocketEntity>> TIER_2_ROCKET = ENTITY_TYPES.register("tier_2_rocket",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.Tier2RocketEntity>of(io.github.reiko45.galacticraft.entity.Tier2RocketEntity::new,
                    MobCategory.MISC).sized(1.2F, 4.5F).clientTrackingRange(16).updateInterval(1).fireImmune().build("tier_2_rocket"));
    /** 1.12.2 EntityCargoRocket setSize(0.98, 2). */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.CargoRocketEntity>> CARGO_ROCKET = ENTITY_TYPES.register("cargo_rocket",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.CargoRocketEntity>of(io.github.reiko45.galacticraft.entity.CargoRocketEntity::new,
                    MobCategory.MISC).sized(0.98F, 2.0F).clientTrackingRange(16).updateInterval(1).fireImmune().build("cargo_rocket"));
    public static final Supplier<EntityType<LanderEntity>> LANDER = ENTITY_TYPES.register("lander",
            () -> EntityType.Builder.<LanderEntity>of(LanderEntity::new, MobCategory.MISC).sized(3.0F, 4.25F)
                    .clientTrackingRange(16).updateInterval(1).fireImmune().build("lander"));
    /** 1.12.2 EntityLandingBalloons setSize(2, 2). */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.LandingBalloonsEntity>> LANDING_BALLOONS = ENTITY_TYPES.register("landing_balloons",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.LandingBalloonsEntity>of(io.github.reiko45.galacticraft.entity.LandingBalloonsEntity::new,
                    MobCategory.MISC).sized(2.0F, 2.0F).clientTrackingRange(16).updateInterval(1).fireImmune().build("landing_balloons"));
    public static final Supplier<EntityType<ParachestEntity>> PARACHEST = ENTITY_TYPES.register("parachest",
            () -> EntityType.Builder.<ParachestEntity>of(ParachestEntity::new, MobCategory.MISC).sized(1.0F, 1.0F)
                    .clientTrackingRange(10).updateInterval(2).build("parachest"));

    public static final Supplier<EntityType<MeteorEntity>> METEOR = ENTITY_TYPES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC).sized(1.0F, 1.0F)
                    .clientTrackingRange(16).updateInterval(1).fireImmune().noSave().build("meteor"));

    // Evolved mobs (sizes and tracking as vanilla; 1.12.2 drew most of them 1.2x larger)
    public static final Supplier<EntityType<EvolvedZombie>> EVOLVED_ZOMBIE = ENTITY_TYPES.register("evolved_zombie",
            () -> EntityType.Builder.<EvolvedZombie>of(EvolvedZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).build("evolved_zombie"));
    public static final Supplier<EntityType<EvolvedSkeleton>> EVOLVED_SKELETON = ENTITY_TYPES.register("evolved_skeleton",
            () -> EntityType.Builder.<EvolvedSkeleton>of(EvolvedSkeleton::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F)
                    .ridingOffset(-0.7F).clientTrackingRange(8).build("evolved_skeleton"));
    public static final Supplier<EntityType<EvolvedSpider>> EVOLVED_SPIDER = ENTITY_TYPES.register("evolved_spider",
            () -> EntityType.Builder.<EvolvedSpider>of(EvolvedSpider::new, MobCategory.MONSTER).sized(1.5F, 1.0F).eyeHeight(0.65F)
                    .passengerAttachments(0.765F).clientTrackingRange(8).build("evolved_spider"));
    public static final Supplier<EntityType<EvolvedCreeper>> EVOLVED_CREEPER = ENTITY_TYPES.register("evolved_creeper",
            () -> EntityType.Builder.<EvolvedCreeper>of(EvolvedCreeper::new, MobCategory.MONSTER).sized(0.7F, 2.2F)
                    .clientTrackingRange(8).build("evolved_creeper"));
    public static final Supplier<EntityType<EvolvedEnderman>> EVOLVED_ENDERMAN = ENTITY_TYPES.register("evolved_enderman",
            () -> EntityType.Builder.<EvolvedEnderman>of(EvolvedEnderman::new, MobCategory.MONSTER).sized(0.6F, 2.9F).eyeHeight(2.55F)
                    .passengerAttachments(2.80625F).clientTrackingRange(8).build("evolved_enderman"));

    /** 1.12.2 EntityBuggy setSize(1.4, 0.6). */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.BuggyEntity>> BUGGY = ENTITY_TYPES.register("buggy",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.BuggyEntity>of(io.github.reiko45.galacticraft.entity.BuggyEntity::new,
                    MobCategory.MISC).sized(1.4F, 0.6F).clientTrackingRange(10).updateInterval(1).fireImmune().build("buggy"));

    /** 1.12.2 EntitySkeletonBoss setSize(1.5, 4.0). */
    public static final Supplier<EntityType<SkeletonBossEntity>> SKELETON_BOSS = ENTITY_TYPES.register("skeleton_boss",
            () -> EntityType.Builder.<SkeletonBossEntity>of(SkeletonBossEntity::new, MobCategory.MONSTER).sized(1.5F, 4.0F).eyeHeight(3.5F)
                    .fireImmune().clientTrackingRange(10).build("skeleton_boss"));

    /** 1.12.2 EntityCreeperBoss setSize(2.0, 7.0), immune to fire. */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.CreeperBossEntity>> CREEPER_BOSS = ENTITY_TYPES.register("creeper_boss",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.CreeperBossEntity>of(io.github.reiko45.galacticraft.entity.CreeperBossEntity::new,
                    MobCategory.MONSTER).sized(2.0F, 7.0F).fireImmune().clientTrackingRange(10).build("creeper_boss"));
    /** 1.12.2 EntityProjectileTNT setSize(1.0, 1.0); "projectile_tnt", tracked every tick. */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.TntProjectileEntity>> TNT_PROJECTILE = ENTITY_TYPES.register("tnt_projectile",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.TntProjectileEntity>of(io.github.reiko45.galacticraft.entity.TntProjectileEntity::new,
                    MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1).build("tnt_projectile"));

    /** 1.12.2 EntitySludgeling setSize(0.3, 0.2). */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.SludgelingEntity>> SLUDGELING = ENTITY_TYPES.register("sludgeling",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.SludgelingEntity>of(io.github.reiko45.galacticraft.entity.SludgelingEntity::new,
                    MobCategory.MONSTER).sized(0.3F, 0.2F).clientTrackingRange(8).build("sludgeling"));
    /** 1.12.2 EntitySlimeling setSize(0.45, 0.7) at half size, eyes at 0.8 of its height; it grows to twice that. */
    public static final Supplier<EntityType<io.github.reiko45.galacticraft.entity.SlimelingEntity>> SLIMELING = ENTITY_TYPES.register("slimeling",
            () -> EntityType.Builder.<io.github.reiko45.galacticraft.entity.SlimelingEntity>of(io.github.reiko45.galacticraft.entity.SlimelingEntity::new,
                    MobCategory.CREATURE).sized(0.45F, 0.7F).eyeHeight(0.56F).clientTrackingRange(10).build("slimeling"));

    private GCEntities() {
    }
}
