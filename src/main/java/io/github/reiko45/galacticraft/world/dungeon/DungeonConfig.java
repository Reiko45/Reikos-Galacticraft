/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.world.gen.dungeon.DungeonConfiguration and the
 * DungeonConfiguration instances of ChunkProviderMoon and ChunkProviderMars). Copyright (c) 2023 Team Galacticraft.
 * Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.dungeon;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * One planet's dungeon (1.12.2 DungeonConfiguration): the brick, the floor height, corridor lengths and heights, room
 * height, the boss room's kind and the loot. Everything else about the layout is shared.
 */
public enum DungeonConfig implements StringRepresentable {
    /** ChunkProviderMoon: moon dungeon brick, y 25, corridors 8-16 long and 5 high, rooms 6 high; RoomBoss, RoomTreasure. */
    MOON("moon", 25, 5, 6, GCBlocks.MOON_DUNGEON_BRICK, GCBlocks.MOON_DUNGEON_BRICK, GCBlocks.BOSS_SPAWNER, GCBlocks.TREASURE_CHEST_TIER_1,
            "dungeon_tier_1", 3),
    /** ChunkProviderMars: Mars dungeon brick, y 30, corridors 8-16 long and 7 high, rooms 7 high; RoomBossMars, RoomTreasureMars. */
    MARS("mars", 30, 7, 7, GCBlocks.MARS_DUNGEON_BRICK, GCBlocks.MARS_DUNGEON_BRICK, GCBlocks.MARS_BOSS_SPAWNER, GCBlocks.TREASURE_CHEST_TIER_2,
            "dungeon_tier_2", 3),
    /**
     * ChunkProviderVenus (DungeonConfigurationVenus): Venus Dungeon Brick I walls and Brick II floors and ceilings, y 30,
     * corridors 8-16 long, 5 wide and 7 high, rooms 7 high; RoomBossVenus, RoomTreasureVenus.
     */
    VENUS("venus", 30, 7, 7, GCBlocks.VENUS_DUNGEON_BRICK_1, GCBlocks.VENUS_DUNGEON_BRICK_2, GCBlocks.VENUS_BOSS_SPAWNER,
            GCBlocks.TREASURE_CHEST_TIER_3, "dungeon_tier_3", 5);

    public static final Codec<DungeonConfig> CODEC = StringRepresentable.fromEnum(DungeonConfig::values);

    public static final int HALL_MIN = 8;
    public static final int HALL_MAX = 16;

    private final String name;
    private final int y;
    private final int hallHeight;
    private final int roomHeight;
    private final DeferredBlock<? extends Block> brick;
    private final DeferredBlock<? extends Block> floorBrick;
    private final DeferredBlock<? extends Block> bossSpawner;
    private final DeferredBlock<? extends Block> treasureChest;
    private final ResourceKey<LootTable> loot;
    private final int hallWidth;

    DungeonConfig(String name, int y, int hallHeight, int roomHeight, DeferredBlock<? extends Block> brick, DeferredBlock<? extends Block> floorBrick,
                  DeferredBlock<? extends Block> bossSpawner, DeferredBlock<? extends Block> treasureChest, String loot, int hallWidth) {
        this.name = name;
        this.y = y;
        this.hallHeight = hallHeight;
        this.roomHeight = roomHeight;
        this.brick = brick;
        this.floorBrick = floorBrick;
        this.hallWidth = hallWidth;
        this.bossSpawner = bossSpawner;
        this.treasureChest = treasureChest;
        this.loot = ResourceKey.create(Registries.LOOT_TABLE, GalacticraftCore.id(loot));
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** The floor of the dungeon. */
    public int y() {
        return this.y;
    }

    public int hallHeight() {
        return this.hallHeight;
    }

    public int roomHeight() {
        return this.roomHeight;
    }

    public BlockState brick() {
        return this.brick.get().defaultBlockState();
    }

    /** Floors and ceilings (Venus has a brick of its own for them). */
    public BlockState floorBrick() {
        return this.floorBrick.get().defaultBlockState();
    }

    /** Corridor width, walls included (1.12.2 DirectionalPiece#getCorridor: 3, Venus 5 for its double walls). */
    public int hallWidth() {
        return this.hallWidth;
    }

    public Block bossSpawner() {
        return this.bossSpawner.get();
    }

    public Block treasureChest() {
        return this.treasureChest.get();
    }

    /** 1.12.2 IGalacticraftWorldProvider#getDungeonChestType: chest rooms and the treasure chest. */
    public ResourceKey<LootTable> loot() {
        return this.loot;
    }
}
