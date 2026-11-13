/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.world.gen.base.BaseRoom).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.world.base;

import io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.NasaWorkbenchBlock;
import io.github.reiko45.galacticraft.block.SolarPartBlock;
import io.github.reiko45.galacticraft.block.SpaceWartBlock;
import io.github.reiko45.galacticraft.block.entity.CargoMachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.entity.HangingSchematicEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCStructures;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A room off a deck's corridor, entered from the corridor side (local z 0). Its contents depend on its type: an
 * engineering room of NASA Workbenches with two schematics on the far wall, a power room of storage modules and wires,
 * a store with a launch pad and a locked Cargo Loader (the base's treasure, freed by loading it into a rocket), an empty
 * room with pillars and Space Wart, a medical room, crew quarters, a control room, or cryogenic chambers.
 */
public class BaseRoom extends BasePiece {
    /** 1.12.2 EnumRoomType: the floor and entrance blocks, and whether the contents also fill the entry wall. */
    public enum RoomType {
        ENGINEERING(BaseBlocks::dark, BaseBlocks::dark, false),
        POWER(null, null, false),
        STORE(null, null, false),
        EMPTY(null, null, false),
        MEDICAL(() -> Blocks.IRON_TRAPDOOR.defaultBlockState(), () -> Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), false),
        CREW(null, null, false),
        CRYO(BaseBlocks::dark, BaseBlocks::dark, true),
        CONTROL(BaseBlocks::darkSlab, BaseBlocks::dark, false);

        @Nullable
        private final Supplier<BlockState> floor;
        @Nullable
        private final Supplier<BlockState> entrance;
        private final boolean doEntryWallsToo;

        RoomType(@Nullable Supplier<BlockState> floor, @Nullable Supplier<BlockState> entrance, boolean doEntryWallsToo) {
            this.floor = floor;
            this.entrance = entrance;
            this.doEntryWallsToo = doEntryWallsToo;
        }
    }

    /** A block of a multiblock whose main block is elsewhere in the room, and where that main block is. */
    private record Part(BlockState state, BlockPos master) {
    }

    private static final List<BlockPos> WORKBENCH_PARTS = NasaWorkbenchBlock.partPositions(BlockPos.ZERO);

    private RoomType type;
    private boolean nearEnd;
    private boolean farEnd;
    private int deckTier;

    public BaseRoom(BaseConfiguration configuration, int blockPosX, int yPos, int blockPosZ, int sizeX, int sizeY, int sizeZ, Direction entranceDir,
                    RoomType roomType, boolean near, boolean far, int deckTier) {
        super(GCStructures.BASE_ROOM.get(), configuration, sizeX, sizeY, sizeZ, entranceDir.getOpposite());
        setOrientation(this.direction);
        this.type = roomType;
        this.nearEnd = near;
        this.farEnd = far;
        this.deckTier = deckTier;
        setBox(blockPosX, yPos, blockPosZ, blockPosX + this.sizeX, yPos + this.sizeY, blockPosZ + this.sizeZ);
    }

    public BaseRoom(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GCStructures.BASE_ROOM.get(), tag);
        final int typeNo = tag.getInt("brT");
        this.type = typeNo >= 0 && typeNo < RoomType.values().length ? RoomType.values()[typeNo] : RoomType.EMPTY;
        final int details = tag.getInt("brD");
        this.deckTier = details & 15;
        this.nearEnd = (details & 16) == 16;
        this.farEnd = (details & 32) == 32;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("brT", this.type.ordinal());
        tag.putInt("brD", this.deckTier + (this.nearEnd ? 16 : 0) + (this.farEnd ? 32 : 0));
    }

    public RoomType type() {
        return this.type;
    }

    /** The room's far corner inside it: x across the entrance side, z from the entrance (0) to the far wall. */
    public int localMaxX() {
        return maxX();
    }

    public int localMaxZ() {
        return maxZ();
    }

    private int maxX() {
        return this.direction.getAxis() == Direction.Axis.X ? this.sizeZ : this.sizeX;
    }

    private int maxZ() {
        return this.direction.getAxis() == Direction.Axis.X ? this.sizeX : this.sizeZ;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        final Block blockStair = BaseBlocks.moonStairs();
        final BlockState blockAir = Blocks.AIR.defaultBlockState();
        final BlockState wall = this.configuration.wallBlock();
        final BaseConfiguration.BaseType deckType = this.configuration.deckType();
        final int maxX = maxX();
        final int maxZ = maxZ();
        for (int xx = 0; xx <= maxX; xx++) {
            final boolean near = this.nearEnd && xx == maxX;
            final boolean far = this.farEnd && xx == 0;
            for (int yy = 0; yy <= this.sizeY; yy++) {
                for (int zz = 0; zz <= maxZ; zz++) {
                    // 5 outer walls of the room
                    if (xx == 0 || xx == maxX || yy == 0 || yy == this.sizeY || zz == maxZ) {
                        final boolean xEntrance = maxX > 6 ? xx > 2 && xx < maxX - 2 : xx > 1 && xx < maxX - 1;
                        if (this.type.entrance != null && yy == 0 && zz == 0 && xEntrance && deckType != BaseConfiguration.BaseType.TUNNELER) {
                            place(level, this.type.entrance.get(), xx, yy, zz, box);
                            place(level, wall, xx, yy - 1, zz, box);
                        }
                        // Shave the top and bottom corners
                        else if (!((zz == maxZ || near || far) && (yy == 0 && (this.deckTier & 1) == 1 || yy == this.sizeY && (this.deckTier & 2) == 2
                                || zz == maxZ && (near || far))) || zz == 0 && yy == 0) {
                            place(level, wall, xx, yy, zz, box);
                        }
                        // Special case, fill in some corners on the hangar base's top deck
                        else if (yy == this.sizeY && (this.deckTier & 2) == 2 && this.configuration.isHangarDeck() && zz < 3) {
                            if (xx == 0 || xx == maxX) {
                                place(level, wall, xx, yy, zz, box);
                            }
                        }
                        continue;
                    }
                    // Room internals. 1.12.2 placed multiblocks' parts and then overwrote most of them with what came
                    // later in the room; here they stay.
                    final BlockPos pos = worldPos(xx, yy, zz);
                    final Part part = box.isInside(pos) ? partAt(xx, yy, zz) : null;
                    if (part != null) {
                        setRaw(level, pos, part.state());
                        if (level.getBlockEntity(pos) instanceof SolarPartBlockEntity solarPart) {
                            solarPart.setMainPos(part.master());
                        }
                    } else if (isContentPos(xx, yy, zz)) {
                        if (box.isInside(pos)) {
                            buildRoomContents(level, xx, yy, zz, maxX - 1, maxZ - 1, pos);
                        }
                    } else if (deckType == BaseConfiguration.BaseType.TUNNELER && (yy == 1 || yy == this.sizeY - 1)) {
                        int meta = 1;
                        if (xx == 1) {
                            meta = 3;
                        } else if (xx == maxX - 1) {
                            meta = 2;
                        } else if (zz == 0) {
                            meta = 0;
                        }
                        if (this.direction == Direction.NORTH) {
                            meta ^= 3 ^ meta / 2;
                        } else if (this.direction == Direction.SOUTH) {
                            meta ^= 2 + meta / 2;
                        } else if (this.direction == Direction.EAST) {
                            meta ^= 1;
                        }
                        meta += yy == 1 ? 0 : 4;
                        place(level, BaseBlocks.stairs(blockStair, meta), xx, yy, zz, box);
                    } else if (yy == 1 && this.type.floor != null) {
                        final boolean xEntrance = maxX > 6 ? xx > 2 && xx < maxX - 2 : xx > 1 && xx < maxX - 1;
                        place(level, zz == 0 && this.type.entrance != null && xEntrance ? blockAir : this.type.floor.get(), xx, yy, zz, box);
                    } else {
                        place(level, blockAir, xx, yy, zz, box);
                    }
                }
            }
        }
    }

    /** An inside position whose block the room's type decides (1.12.2 called buildRoomContents for it). */
    private boolean isContentPos(int xx, int yy, int zz) {
        final int maxX = maxX();
        final int maxZ = maxZ();
        if (xx <= 0 || xx >= maxX || yy <= 0 || yy >= this.sizeY || zz < 0 || zz >= maxZ) {
            return false;
        }
        return xx > 1 && xx < maxX - 1 && zz > 0 && zz < maxZ - 1 || yy > 1 && yy < this.sizeY - 1 || this.type.doEntryWallsToo;
    }

    /** The part of a NASA Workbench or Cryogenic Chamber at this inside position, if one claims it. */
    @Nullable
    private Part partAt(int xx, int yy, int zz) {
        final int maxX = maxX() - 1;
        final int maxZ = maxZ() - 1;
        if (this.type == RoomType.ENGINEERING) {
            for (BlockPos offset : WORKBENCH_PARTS) {
                final int mx = xx - offset.getX();
                final int my = yy - offset.getY();
                final int mz = zz - offset.getZ();
                if (isContentPos(mx, my, mz) && contentAt(mx, my, mz, maxX, maxZ, worldPos(mx, my, mz)).is(GCBlocks.NASA_WORKBENCH.get())) {
                    return new Part(GCBlocks.NASA_WORKBENCH_PART.get().defaultBlockState().setValue(SolarPartBlock.PART, SolarPartBlock.Part.WORKBENCH),
                            worldPos(mx, my, mz));
                }
            }
        } else if (this.type == RoomType.CRYO) {
            for (int dy = 1; dy <= 2; dy++) {
                if (isContentPos(xx, yy - dy, zz)) {
                    final BlockState master = contentAt(xx, yy - dy, zz, maxX, maxZ, worldPos(xx, yy - dy, zz));
                    if (master.is(GCBlocks.CRYOGENIC_CHAMBER.get())) {
                        return new Part(GCBlocks.CRYOGENIC_CHAMBER_PART.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, master.getValue(HorizontalDirectionalBlock.FACING))
                                .setValue(CryogenicChamberPartBlock.TOP, dy == 2), worldPos(xx, yy - dy, zz));
                    }
                }
            }
        }
        return null;
    }

    private static int semirand(BlockPos pos) {
        return ((pos.getY() * 379 + pos.getX()) * 373 + pos.getZ()) * 7 & 15;
    }

    private static BlockState facing(Block block, int horizontalIndex) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue(horizontalIndex & 3));
    }

    /**
     * 1.12.2 buildRoomContents' choice of block: contents span x 1 to maxX, y 1 to sizeY - 1 and z 1 to maxZ. These are
     * placed as they are, without the piece's mirroring and rotation, so facings are worked out from the room's
     * direction.
     */
    private BlockState contentAt(int x, int y, int z, int maxX, int maxZ, BlockPos blockpos) {
        BlockState state = Blocks.AIR.defaultBlockState();
        final int semirand = semirand(blockpos);
        int facing = 0;
        int facing1 = 0;
        int facing2 = 0;
        switch (this.direction) {
            case WEST -> {
                facing1 = 3;
                facing2 = 2;
            }
            case EAST -> {
                facing = 2;
                facing1 = 1;
            }
            case NORTH -> {
                facing = 3;
                facing1 = 2;
                facing2 = 1;
            }
            case SOUTH -> {
                facing = 1;
                facing2 = 3;
            }
            default -> {
            }
        }
        // Offset from centre - used for some rooms
        final int ox = maxX / 2 - 3;
        final int xx = x - ox;
        final boolean humanoid = this.configuration.deckType() == BaseConfiguration.BaseType.HUMANOID;

        switch (this.type) {
            case EMPTY -> {
                // Pillars in the corners
                if ((z == 1 || z == maxZ - 1) && (x == 2 || x == maxX - 1)) {
                    state = BaseBlocks.wall(2);
                } else if (y == 1 && semirand < 2) {
                    // Some random Space Wart
                    state = GCBlocks.SPACE_WART.get().defaultBlockState().setValue(SpaceWartBlock.AGE, semirand);
                }
            }
            case STORE -> {
                if (maxX >= 6 && y == 1 && maxZ >= 5) {
                    // An actual landing pad (indoors!), joined as 1.12.2's single pads joined themselves on their first tick
                    if (xx >= 3 && xx <= 5 && z >= 2 && z <= 4) {
                        state = GCBlocks.LANDING_PAD.get().defaultBlockState()
                                .setValue(LandingPadBlock.PAD, xx == 4 && z == 3 ? LandingPadBlock.Pad.CENTRE : LandingPadBlock.Pad.PART);
                    } else if (xx == 2) {
                        // Cargo loaders and unloaders, these will contain treasure items but only accessible when powered
                        switch (z) {
                            case 2 -> state = facing(GCBlocks.CARGO_LOADER.get(), facing);
                            case 3 -> state = GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState();
                            case 4 -> state = facing(GCBlocks.CARGO_UNLOADER.get(), facing ^ 2);
                            default -> {
                            }
                        }
                    }
                }
            }
            case POWER -> {
                facing = (facing + 1) % 4;
                switch (y) {
                    case 1 -> {
                        // Layer 1: wall blocks with sealable alu wire in the centre
                        if (z == 3 && xx >= 4 && x <= maxX - 2) {
                            state = BaseBlocks.enclosedHeavyWire();
                        } else if (xx >= 3 && z > 1 && (z < maxZ - 1 || z == 4)) {
                            state = this.configuration.wallBlock();
                        }
                    }
                    case 2 -> {
                        // Layer 2: tier 1 storage and alu wire and a switch
                        if (z == 2) {
                            if (xx == 3) {
                                state = facing(GCBlocks.ENERGY_STORAGE_MODULE.get(), facing);
                            } else if (xx > 3 && x < maxX - 2) {
                                state = GCBlocks.ALUMINUM_WIRE.get().defaultBlockState();
                            } else if (x == maxX - 2) {
                                state = GCBlocks.ALUMINUM_WIRE_SWITCH.get().defaultBlockState();
                            } else if (x == maxX - 1) {
                                state = GCBlocks.MINER_BASE.get().defaultBlockState();
                            }
                        } else if (z == 3 && x == maxX - 2) {
                            state = GCBlocks.ALUMINUM_WIRE.get().defaultBlockState();
                        } else if (z == 4 && (xx == 3 || x == maxX - 1)) {
                            // An industrial looking frame for the whole structure
                            state = GCBlocks.MINER_BASE.get().defaultBlockState();
                        }
                    }
                    case 3 -> {
                        // Layer 3: tier 2 storage and alu wire
                        if (z == 2 && xx == 4) {
                            state = GCBlocks.ALUMINUM_WIRE.get().defaultBlockState();
                        } else if (z == 3 && x == maxX - 2) {
                            state = facing(GCBlocks.ENERGY_STORAGE_CLUSTER.get(), facing ^ 2);
                        } else if (z == 3 && x == maxX - 1) {
                            state = GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState();
                        } else if (z == 3 && xx >= 4 && x < maxX - 2) {
                            state = GCBlocks.ALUMINUM_WIRE.get().defaultBlockState();
                        } else if ((z == 2 || z == 4) && (xx == 3 || x == maxX - 1)) {
                            state = GCBlocks.MINER_BASE.get().defaultBlockState();
                        }
                    }
                    case 4 -> {
                        // Layer 4: tier 2 storage and alu wire
                        if (z == 3) {
                            if (x == maxX - 3) {
                                state = facing(GCBlocks.ENERGY_STORAGE_CLUSTER.get(), facing);
                            } else if (x == maxX - 1 || x == maxX - 2) {
                                state = GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState();
                            }
                        } else if ((z == 2 || z == 4) && xx >= 3 && x < maxX) {
                            state = GCBlocks.MINER_BASE.get().defaultBlockState();
                        }
                    }
                    default -> {
                    }
                }
            }
            case ENGINEERING -> {
                if (y == 1) {
                    final int closerz = maxZ <= 5 ? 2 : 3;
                    if ((z == maxZ - 2 && x == maxX / 2 + 1 || z == closerz && (x == 2 || x == maxX - 1) && maxX > 5) && maxZ > 3) {
                        state = GCBlocks.NASA_WORKBENCH.get().defaultBlockState();
                    } else {
                        state = BaseBlocks.dark();
                    }
                }
            }
            case MEDICAL -> {
                int zTable = maxZ - 3;
                int zTank = maxZ - 1;
                int zLight = zTable;
                if (zTable <= 1) {
                    if (zTable < 0) {
                        // Too small to build the room
                        break;
                    }
                    // Small medical room: bring everything closer together
                    zTable++;
                    zLight++;
                    zTank = maxZ;
                }
                if (y == 1) {
                    // Operating table at y 1, on a tiled floor
                    state = z == zTable && x <= maxX - 1 && x >= maxX - 3 ? BaseBlocks.magneticTable() : Blocks.IRON_TRAPDOOR.defaultBlockState();
                } else if (y == 2 || y == 3) {
                    if (y == 2 && z == zTable && x <= maxX - 1 && x >= maxX - 3) {
                        // Operating table at y 2
                        state = GCBlocks.LANDING_PAD.get().defaultBlockState();
                    } else if (z == zTank && (maxX - x) % 2 == 1) {
                        // Fluid tanks, 2 tanks high, filled with Bacterial Sludge
                        state = GCBlocks.FLUID_TANK.get().defaultBlockState();
                    }
                } else if ((y == 4 || y == 5) && (z == zLight || z == zLight - 1)) {
                    // Lighting
                    if (x == maxX) {
                        state = BaseBlocks.concealedDetector(humanoid, facing);
                    } else if (x == maxX - 1) {
                        // 1.12.2 brightLamp meta facingLamp: mounted on the wall it lights the room from
                        state = BaseBlocks.arcLamp(switch (this.direction) {
                            case WEST -> 3;
                            case NORTH -> 5;
                            case SOUTH -> 4;
                            default -> 2;
                        });
                    }
                }
            }
            case CREW -> {
                if (y == 1) {
                    if (x == 2 || x == maxX - 1) {
                        if (z % 2 == 1) {
                            state = BaseBlocks.wall(z == 1 ? 3 : 2);
                        }
                    } else {
                        state = Blocks.GRAY_CARPET.defaultBlockState();
                    }
                } else if (y == 2 && (x == 2 || x == maxX - 1)) {
                    if (z == 1) {
                        state = Blocks.BREWING_STAND.defaultBlockState();
                    } else if (z > 2 && z < maxZ) {
                        state = GCBlocks.BUGGY_FUELING_PAD.get().defaultBlockState();
                    }
                }
            }
            case CONTROL -> {
                if (y == 1) {
                    state = x == maxX / 2 + 1 && z == maxZ - 2 ? BaseBlocks.dark() : BaseBlocks.darkSlab();
                } else if (y <= 3) {
                    if ((x == 1 || x == maxX) && !(z == 0 || z == maxZ)) {
                        state = BaseBlocks.telemetry();
                    } else if ((x == 2 || x == maxX - 1) && !(z == 0 || z == maxZ)) {
                        state = BaseBlocks.screen();
                    } else if (z == maxZ && x > 2 && x < maxX - 1) {
                        state = BaseBlocks.screen();
                    } else if (x == maxX / 2 + 1 && z == maxZ - 2 && y == 2) {
                        state = GCBlocks.BUGGY_FUELING_PAD.get().defaultBlockState();
                    }
                }
            }
            case CRYO -> {
                final boolean xEntrance = maxX > 5 ? x > 2 && x < maxX - 1 : x > 1 && x < maxX;
                final boolean highEntrance = this.configuration.isHangarDeck() && this.configuration.deckType() == BaseConfiguration.BaseType.AVIAN;
                if (y == 1) {
                    // A dark plinth for it all at y 1
                    if (z == 1 && x > 1 && x < maxX) {
                        state = BaseBlocks.darkSlab();
                    } else if (z != 0 || !xEntrance || highEntrance) {
                        state = BaseBlocks.dark();
                    }
                } else if (z == 0 && (y > (highEntrance ? 5 : 3) || !xEntrance)) {
                    // Dark ceiling and entrance wall
                    state = BaseBlocks.dark();
                } else if (y <= 4 && (z == maxZ || x == 1 || x == maxX)) {
                    // Around the walls: Cryogenic Chambers alternated with dark blocks
                    if (z == maxZ && x % 2 == 0 && x < maxX) {
                        if (y == 2) {
                            state = facing(GCBlocks.CRYOGENIC_CHAMBER.get(), facing1);
                        }
                    } else if (z < maxZ && z > 1 && (maxZ - z) % 2 == 0) {
                        if (y == 2) {
                            state = facing(GCBlocks.CRYOGENIC_CHAMBER.get(), x == 1 ? facing : facing2);
                        }
                    } else {
                        state = BaseBlocks.dark();
                    }
                } else if (y > 5 || y == this.configuration.roomHeight() || z == maxZ || x == 1 || x == maxX) {
                    // Dark top section of walls and ceiling
                    state = BaseBlocks.dark();
                }
            }
            default -> {
            }
        }
        return state;
    }

    /** 1.12.2 buildRoomContents: place the block, fill what it holds, and hang the engineering room's schematics. */
    private void buildRoomContents(WorldGenLevel level, int x, int y, int z, int maxX, int maxZ, BlockPos blockpos) {
        final BlockState state = contentAt(x, y, z, maxX, maxZ, blockpos);
        final int semirand = semirand(blockpos);
        setRaw(level, blockpos, state);
        if (state.hasBlockEntity()) {
            final BlockEntity tile = level.getBlockEntity(blockpos);
            if (tile instanceof FluidTankBlockEntity tank) {
                tank.tank().fill(new FluidStack(GCFluids.BACTERIAL_SLUDGE.get(), FluidTankBlockEntity.CAPACITY), IFluidHandler.FluidAction.EXECUTE);
            } else if (tile instanceof CargoMachineBlockEntity loader && loader.loads()) {
                // Looks like the food supplies have gone off!
                loader.lock();
                final ItemStack[] cargo = {
                        new ItemStack(Items.POISONOUS_POTATO, 64), new ItemStack(Items.POISONOUS_POTATO, 64),
                        new ItemStack(Items.POISONOUS_POTATO, 64), new ItemStack(Items.POISONOUS_POTATO, 64),
                        new ItemStack(Items.ROTTEN_FLESH, 64),
                        new ItemStack(GCItems.STEEL_POLE.get(), semirand % 31 + 2),
                        new ItemStack(GCItems.SLIMELING_CARGO.get(), semirand % 2 + 1),
                        new ItemStack(GCItems.THERMAL_CLOTH.get(), semirand % 23 + 41),
                        new ItemStack(GCItems.OIL_CANISTER.get()), new ItemStack(GCItems.OIL_CANISTER.get()), new ItemStack(GCItems.OIL_CANISTER.get()),
                        new ItemStack(GCItems.OIL_CANISTER.get()), new ItemStack(GCItems.OIL_CANISTER.get()),
                        this.configuration.deckType().treasure()};
                for (ItemStack stack : cargo) {
                    ItemStack rest = stack;
                    for (int slot = 1; slot < CargoMachineBlockEntity.SLOTS && !rest.isEmpty(); slot++) {
                        rest = loader.inventory().insertItem(slot, rest, false);
                    }
                }
            } else if (tile instanceof BrewingStandBlockEntity stand) {
                stand.setItem(0, potion(Potions.POISON));
                stand.setItem(1, potion(Potions.WEAKNESS));
                stand.setItem(2, potion(Potions.HARMING));
            } else if (tile instanceof EnergyStorageBlockEntity store && semirand % 3 == 1) {
                // 1.12.2 battery damage 100: flat
                store.inventory().setStackInSlot(1, new ItemStack(GCItems.BATTERY.get()));
            }
        }

        if (this.type == RoomType.ENGINEERING && y == (this.configuration.roomHeight() <= 4 ? 2 : 3) && z == maxZ && (x == 3 || x == 6)) {
            // Facing back into the room, away from the far wall
            final BlockPos towardsEntrance = worldPos(x, y, z - 1).subtract(blockpos);
            final Direction facing = Direction.getNearest(towardsEntrance.getX(), 0, towardsEntrance.getZ());
            level.addFreshEntity(new HangingSchematicEntity(level.getLevel(), blockpos, facing, x / 3 - 1));
        }
    }

    private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        final ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return stack;
    }
}
