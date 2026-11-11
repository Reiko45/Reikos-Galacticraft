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
    public static final Supplier<EntityType<LanderEntity>> LANDER = ENTITY_TYPES.register("lander",
            () -> EntityType.Builder.<LanderEntity>of(LanderEntity::new, MobCategory.MISC).sized(3.0F, 4.25F)
                    .clientTrackingRange(16).updateInterval(1).fireImmune().build("lander"));
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

    private GCEntities() {
    }
}
