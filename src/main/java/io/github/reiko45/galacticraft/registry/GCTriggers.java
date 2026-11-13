/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.advancement.GCTriggers). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.12.2 GCTriggers: the advancements' own criteria. Each fires for one player, with no conditions of its own
 * (vanilla's {@link PlayerTrigger}, as for "tick").
 */
public final class GCTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, GalacticraftCore.MOD_ID);

    /** A rocket with this player aboard has launched (1.12.2 EntitySpaceshipBase, each tick once launched). */
    public static final Supplier<PlayerTrigger> LAUNCH_ROCKET = TRIGGERS.register("launch_rocket", PlayerTrigger::new);
    /** This player came within 20 blocks of a Skeleton Boss (1.12.2 EntitySkeletonBoss#onLivingUpdate). */
    public static final Supplier<PlayerTrigger> FIND_MOON_BOSS = TRIGGERS.register("boss_moon", PlayerTrigger::new);
    /** This player built a space station (1.12.2 S_BIND_SPACE_STATION_ID). */
    public static final Supplier<PlayerTrigger> CREATE_SPACE_STATION = TRIGGERS.register("create_space_station", PlayerTrigger::new);

    private GCTriggers() {
    }
}
