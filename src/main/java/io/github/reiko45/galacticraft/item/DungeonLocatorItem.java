/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemBase's "dungeonfinder",
 * core.world.gen.dungeon.MapGenDungeon#directionToNearestDungeon, core.entities.player.GCPlayerHandler and
 * core.client.render.item.TextureDungeonFinder). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license
 * (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.world.GCDimensions;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jetbrains.annotations.Nullable;

/**
 * Dungeon Locator (1.12.2 "dungeonfinder", creative only): a compass that, carried on a world with dungeons, points to
 * the nearest one (on the asteroids, the nearest abandoned base). The server works the direction out and sends it each
 * tick while it is held, every 30 ticks while it is only carried.
 */
public class DungeonLocatorItem extends Item {
    /** The dungeons of each world: their structure sets, placed by 1.12.2 MapGenDungeon's rule. */
    private static final Map<ResourceKey<Level>, ResourceKey<StructureSet>> DUNGEONS = Map.of(
            GCDimensions.MOON, set("moon_dungeons"), GCDimensions.MARS, set("mars_dungeons"),
            GCDimensions.VENUS, set("venus_dungeons"), GCDimensions.ASTEROIDS, set("abandoned_bases"));

    public DungeonLocatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static ResourceKey<StructureSet> set(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, GalacticraftCore.id(name));
    }

    public static boolean hasDungeons(ResourceKey<Level> dimension) {
        return DUNGEONS.containsKey(dimension);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.galacticraftcore.creative_only.desc").withStyle(ChatFormatting.RED));
    }

    /**
     * 1.12.2 directionToNearestDungeon: the compass bearing (degrees, 0 north, 90 east) from (x, z) to the nearest
     * dungeon's corner (two blocks into its chunk), searching the dungeon regions round the player; null on a world
     * without dungeons.
     */
    @Nullable
    public static Float direction(ServerLevel level, double x, double z) {
        final ResourceKey<StructureSet> key = DUNGEONS.get(level.dimension());
        final StructureSet set = key == null ? null : level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(key);
        if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
            return null;
        }
        return bearing(placement, level.getChunkSource().getGeneratorState().getLevelSeed(), x, z);
    }

    /** The bearing from (x, z) to the nearest of the dungeons {@code placement} puts in the 3 x 3 regions round it. */
    public static float bearing(RandomSpreadStructurePlacement placement, long seed, double x, double z) {
        final int spacing = placement.spacing();
        final int regionX = Math.floorDiv(Mth.floor(x) >> 4, spacing);
        final int regionZ = Math.floorDiv(Mth.floor(z) >> 4, spacing);
        double nearestX = 0.0D;
        double nearestZ = 0.0D;
        double nearest = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final ChunkPos chunk = placement.getPotentialStructureChunk(seed, (regionX + dx) * spacing, (regionZ + dz) * spacing);
                final double ox = chunk.getMinBlockX() + 2 - x;
                final double oz = chunk.getMinBlockZ() + 2 - z;
                if (ox * ox + oz * oz < nearest) {
                    nearest = ox * ox + oz * oz;
                    nearestX = ox;
                    nearestZ = oz;
                }
            }
        }
        // 1.12.2 GCCoreUtil#getAngleForRelativePosition
        return ((float) Math.toDegrees(Mth.atan2(nearestX, -nearestZ)) + 360.0F) % 360.0F;
    }

    /** 1.12.2 GCPlayerHandler#onPlayerUpdate: held, every tick; anywhere else in the inventory, every 30 ticks. */
    public static void tick(ServerPlayer player) {
        if (!hasDungeons(player.level().dimension())) {
            return;
        }
        final boolean held = player.getMainHandItem().getItem() instanceof DungeonLocatorItem;
        if (!held && (player.tickCount % 30 != 0 || !carries(player))) {
            return;
        }
        final Float direction = direction(player.serverLevel(), player.getX() + player.getDeltaMovement().x,
                player.getZ() + player.getDeltaMovement().z);
        if (direction != null) {
            GCNetwork.sendToPlayer(player, new GCNetwork.DungeonDirection(direction));
        }
    }

    private static boolean carries(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof DungeonLocatorItem) {
                return true;
            }
        }
        return false;
    }
}
