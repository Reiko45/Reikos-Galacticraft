package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper;
import io.github.reiko45.galacticraft.entity.monster.EvolvedEnderman;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSkeleton;
import io.github.reiko45.galacticraft.entity.monster.EvolvedSpider;
import io.github.reiko45.galacticraft.entity.monster.EvolvedZombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Attributes and spawn rules of the mod's mobs. */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GCEntityEvents {
    private GCEntityEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(GCEntities.EVOLVED_ZOMBIE.get(), EvolvedZombie.createAttributes().build());
        event.put(GCEntities.EVOLVED_SKELETON.get(), EvolvedSkeleton.createAttributes().build());
        event.put(GCEntities.EVOLVED_SPIDER.get(), EvolvedSpider.createAttributes().build());
        event.put(GCEntities.EVOLVED_CREEPER.get(), EvolvedCreeper.createAttributes().build());
        event.put(GCEntities.EVOLVED_ENDERMAN.get(), EvolvedEnderman.createAttributes().build());
        event.put(GCEntities.EVOLVED_WITCH.get(), io.github.reiko45.galacticraft.entity.monster.EvolvedWitch.createAttributes().build());
        event.put(GCEntities.SKELETON_BOSS.get(), io.github.reiko45.galacticraft.entity.SkeletonBossEntity.createAttributes().build());
        event.put(GCEntities.CREEPER_BOSS.get(), io.github.reiko45.galacticraft.entity.CreeperBossEntity.createAttributes().build());
        event.put(GCEntities.SPIDER_QUEEN.get(), io.github.reiko45.galacticraft.entity.SpiderQueenEntity.createAttributes().build());
        event.put(GCEntities.JUICER.get(), io.github.reiko45.galacticraft.entity.JuicerEntity.createAttributes().build());
        event.put(GCEntities.SLUDGELING.get(), io.github.reiko45.galacticraft.entity.SludgelingEntity.createAttributes().build());
        event.put(GCEntities.SLIMELING.get(), io.github.reiko45.galacticraft.entity.SlimelingEntity.createAttributes().build());
        event.put(GCEntities.ALIEN_VILLAGER.get(), io.github.reiko45.galacticraft.entity.AlienVillagerEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (EntityType<? extends Monster> type : java.util.List.<EntityType<? extends Monster>>of(GCEntities.EVOLVED_ZOMBIE.get(),
                GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_SPIDER.get(), GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_ENDERMAN.get())) {
            register(event, type);
        }
    }

    private static <T extends Monster> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
