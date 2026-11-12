/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.BaseConfiguration
 * and BaseDeck.EnumBaseType). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One abandoned base's random choices: its height, which of the three alien designs it is, whether it is a hangar base,
 * room depth, how many rooms a side, and the shuffled list of room types.
 */
public final class BaseConfiguration {
    private static final int HANGAR_AIRLOCK_HEIGHT = 6;
    private static final int HANGAR_AIRLOCK_WIDTH = 7;

    /** 1.12.2 BaseDeck.EnumBaseType: room height, corridor width, wall block and the treasure in its Cargo Loader. */
    public enum BaseType {
        HUMANOID(5, 3, () -> GCBlocks.TIN_DECORATION_BLOCK_2.get().defaultBlockState(), () -> new ItemStack(Items.GHAST_TEAR, 3)),
        AVIAN(4, 3, () -> GCBlocks.MOON_ROCK.get().defaultBlockState(), () -> new ItemStack(GCItems.STRANGE_SEED_1.get())),
        TUNNELER(4, 4, () -> GCBlocks.MOON_ROCK.get().defaultBlockState(), () -> new ItemStack(GCItems.STRANGE_SEED_0.get()));

        public final int height;
        public final int width;
        private final Supplier<BlockState> wall;
        private final Supplier<ItemStack> treasure;

        BaseType(int height, int width, Supplier<BlockState> wall, Supplier<ItemStack> treasure) {
            this.height = height;
            this.width = width;
            this.wall = wall;
            this.treasure = treasure;
        }

        public BlockState wall() {
            return this.wall.get();
        }

        public ItemStack treasure() {
            return this.treasure.get();
        }
    }

    private int yPosition;
    private int baseType;
    private boolean hangar;
    private int roomDepth;
    private int roomsNo;
    private int[] randomRoomTypes = new int[0];

    private BaseConfiguration() {
    }

    public BaseConfiguration(int yPosition, RandomSource rand) {
        final BaseType[] types = BaseType.values();
        this.yPosition = yPosition - 2 + rand.nextInt(5);
        this.baseType = rand.nextInt(types.length);
        this.hangar = rand.nextInt(3) == 0;
        this.roomDepth = this.hangar ? 7 : rand.nextInt(3) + 5;
        this.roomsNo = rand.nextInt(2) + 2;
        createRandomRoomList(rand);
    }

    /** 1.12.2 createRandomRoomList: each room type in turn, shuffled; a hangar base gets a store on its lower deck. */
    private void createRandomRoomList(RandomSource rand) {
        final int range = BaseRoom.RoomType.values().length;
        final int size = this.hangar ? 8 : this.roomsNo * 6;
        this.randomRoomTypes = new int[size];
        for (int i = 0; i < size; i++) {
            this.randomRoomTypes[i] = i % range;
        }
        for (int i = size - 1; i > 0; i--) {
            final int index = rand.nextInt(i + 1);
            if (i == index) {
                continue;
            }
            final int temp = this.randomRoomTypes[index];
            this.randomRoomTypes[index] = this.randomRoomTypes[i];
            this.randomRoomTypes[i] = temp;
        }
        // Make sure there's a Cargo Loader on the lower tier (50/50 chance this causes one other room to be missed
        // completely, that's OK!)
        if (this.hangar) {
            boolean storeFound = false;
            for (int i = 0; i < 4; i++) {
                if (this.randomRoomTypes[i] == BaseRoom.RoomType.STORE.ordinal()) {
                    storeFound = true;
                    break;
                }
            }
            if (!storeFound) {
                this.randomRoomTypes[rand.nextInt(4)] = BaseRoom.RoomType.STORE.ordinal();
            }
        }
    }

    /** 1.12.2 writeToNBT (the room list is not saved: each room keeps its own type). */
    public void save(CompoundTag tag) {
        tag.putInt("yPos", this.yPosition);
        tag.putInt("dT", this.baseType + (this.hangar ? 16 : 0));
        tag.putInt("rmD", this.roomDepth);
        tag.putInt("rmN", this.roomsNo);
    }

    public static BaseConfiguration load(CompoundTag tag) {
        final BaseConfiguration config = new BaseConfiguration();
        config.yPosition = tag.getInt("yPos");
        int type = tag.getInt("dT");
        if (type >= 16) {
            config.hangar = true;
            type -= 16;
        }
        config.baseType = Math.min(Math.max(type, 0), BaseType.values().length - 1);
        config.roomDepth = tag.getInt("rmD");
        config.roomsNo = tag.getInt("rmN");
        return config;
    }

    public int yPosition() {
        return this.yPosition;
    }

    public BlockState wallBlock() {
        return deckType().wall();
    }

    public int roomHeight() {
        return this.hangar ? HANGAR_AIRLOCK_HEIGHT : deckType().height;
    }

    public int roomDepth() {
        return this.roomDepth;
    }

    public BaseType deckType() {
        return BaseType.values()[this.baseType];
    }

    public boolean isHangarDeck() {
        return this.hangar;
    }

    public int corridorWidth() {
        return this.hangar ? HANGAR_AIRLOCK_WIDTH : deckType().width;
    }

    public int roomsNo() {
        return this.hangar ? 2 : this.roomsNo;
    }

    public int corridorLength() {
        if (roomsNo() == 1) {
            return BaseDeck.ROOMLARGE;
        }
        if (roomsNo() == 2) {
            return BaseDeck.ROOMLARGE + BaseDeck.ROOMLARGE;
        }
        return roomsNo() * BaseDeck.ROOMSMALL + 2 * (BaseDeck.ROOMLARGE - BaseDeck.ROOMSMALL);
    }

    public BaseRoom.RoomType randomRoom(int i) {
        return BaseRoom.RoomType.values()[this.randomRoomTypes[i % this.randomRoomTypes.length]];
    }
}
