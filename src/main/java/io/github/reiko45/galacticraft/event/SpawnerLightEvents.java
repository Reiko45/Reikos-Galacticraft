/*
 * Derived from Galacticraft Legacy (the light check its spawners made through Minecraft 1.12.2's EntityMob, with
 * core.blocks.BlockUnlitTorch's light). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.event;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Galacticraft's evolved mobs come out of spawners in 1.12.2's light: the dungeons' corridors are lined with unlit space
 * torches, which still give light 3 (1.12.2 BlockUnlitTorch), and 1.12.2 let a spawner's monster appear in light up to
 * 7, the brighter the less often (EntityMob#isValidLightLevel). 1.21 allows no block light at all, so the dungeons'
 * spawners near that torchlight spawned nothing.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class SpawnerLightEvents {
    private SpawnerLightEvents() {
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != MobSpawnType.SPAWNER || event.getEntityType().getCategory() != MobCategory.MONSTER
                || !event.getEntityType().is(GCModTags.BREATHES_IN_SPACE)) {
            return;
        }
        event.setResult(lightEnough(event.getLevel(), event.getPos(), event.getRandom())
                ? MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED : MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }

    /** 1.12.2 EntityMob#getCanSpawnHere and isValidLightLevel, for a spawner (which skips the ground check). */
    public static boolean lightEnough(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        }
        final int light = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
        return light <= random.nextInt(8);
    }
}
