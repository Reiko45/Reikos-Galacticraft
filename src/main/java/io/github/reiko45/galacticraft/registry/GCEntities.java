package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
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

    private GCEntities() {
    }
}
