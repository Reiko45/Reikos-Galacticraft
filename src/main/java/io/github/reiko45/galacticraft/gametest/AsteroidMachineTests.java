package io.github.reiko45.galacticraft.gametest;

import com.mojang.authlib.GameProfile;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BeamReceiverBlock;
import io.github.reiko45.galacticraft.block.CryogenicChamberPartBlock;
import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.block.NasaWorkbenchBlock;
import io.github.reiko45.galacticraft.block.ShortRangeTelepadBlock;
import io.github.reiko45.galacticraft.block.SpaceWartBlock;
import io.github.reiko45.galacticraft.block.WalkwayBlock;
import io.github.reiko45.galacticraft.block.entity.BeamReceiverBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CargoMachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MinerBaseBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ShortRangeTelepadBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.data.GCWorldgenProvider;
import io.github.reiko45.galacticraft.entity.AstroMinerEntity;
import io.github.reiko45.galacticraft.entity.GrappleEntity;
import io.github.reiko45.galacticraft.entity.HangingSchematicEntity;
import io.github.reiko45.galacticraft.menu.CargoLoaderMenu;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator;
import io.github.reiko45.galacticraft.world.base.AbandonedBase;
import io.github.reiko45.galacticraft.world.base.BaseDeck;
import io.github.reiko45.galacticraft.world.base.BaseHangar;
import io.github.reiko45.galacticraft.world.base.BasePiece;
import io.github.reiko45.galacticraft.world.base.BasePlate;
import io.github.reiko45.galacticraft.world.base.BaseRoom;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 7e: walkways, Space Wart, the grapple, beam power, short-range telepads, the Astro Miner and its base, the
 * abandoned asteroid base and what it needed (hanging schematics, locked Cargo Loaders, the Dark Decoration Slab).
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AsteroidMachineTests {
    private static final String AIR = "air_room";

    private AsteroidMachineTests() {
    }

    private static ServerPlayer player(GameTestHelper helper, String name, BlockPos pos, GameType mode) {
        final GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("gc-7e-" + name + "-" + System.nanoTime()).getBytes()), name);
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        final ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        final BlockPos at = helper.absolutePos(pos);
        player.teleportTo(helper.getLevel(), at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void logOut(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    // ------------------------------------------------------------------ walkways, space wart, grapple

    /** BlockWalkway: a side shows its rail unless another walkway (of any kind) or a solid face is there. */
    @GameTest(template = AIR)
    public static void walkwaysJoinUp(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 3, 5), GCBlocks.WALKWAY.get());
        helper.setBlock(new BlockPos(6, 3, 5), GCBlocks.WALKWAY_WIRE.get());
        final BlockState first = helper.getBlockState(new BlockPos(5, 3, 5));
        helper.assertTrue(first.getValue(WalkwayBlock.SIDES.get(Direction.EAST)), "joined to the walkway wire beside it");
        helper.assertFalse(first.getValue(WalkwayBlock.SIDES.get(Direction.NORTH)), "open to the north");
        helper.assertTrue(WalkwayBlock.connects(helper.getLevel(), helper.absolutePos(new BlockPos(6, 3, 5)), Direction.WEST), "and the other way");
        helper.succeed();
    }

    /** BlockSpaceWart is BlockNetherWart: it grows on soul sand only. */
    @GameTest(template = AIR)
    public static void spaceWartNeedsSoulSand(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.SOUL_SAND);
        helper.setBlock(new BlockPos(6, 1, 4), Blocks.STONE);
        final BlockState wart = GCBlocks.SPACE_WART.get().defaultBlockState();
        helper.assertTrue(wart.canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4))), "on soul sand");
        helper.assertFalse(wart.canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 4))), "not on stone");
        helper.assertValueEqual(SpaceWartBlock.MAX_AGE, 3, "four ages, like nether wart");
        helper.succeed();
    }

    /** ItemGrapple: let go and it fires a grapple (using a string, the bow one durability). */
    @GameTest(template = AIR)
    public static void grappleFires(GameTestHelper helper) {
        final ServerPlayer player = player(helper, "grappler", new BlockPos(6, 1, 6), GameType.SURVIVAL);
        final ItemStack grapple = new ItemStack(GCItems.GRAPPLE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, grapple);
        player.getInventory().add(new ItemStack(Items.STRING, 3));
        grapple.getItem().releaseUsing(grapple, helper.getLevel(), player, 0);
        final List<GrappleEntity> fired = helper.getLevel().getEntitiesOfClass(GrappleEntity.class, player.getBoundingBox().inflate(8.0D));
        helper.assertValueEqual(fired.size(), 1, "one grapple fired");
        helper.assertTrue(fired.get(0).shooter() == player, "it belongs to the player");
        helper.assertValueEqual(grapple.getDamageValue(), 1, "one use of durability");
        fired.get(0).discard();
        logOut(helper, player);
        helper.succeed();
    }

    // ------------------------------------------------------------------ beam power and telepads

    /** A receiver on a storage module's output beams its power to a receiver on another module's input. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void beamsCarryPower(GameTestHelper helper) {
        // A module facing north puts out through its left (east) face and takes in through its right (west) one
        helper.setBlock(new BlockPos(2, 1, 6), GCBlocks.ENERGY_STORAGE_MODULE.get());
        helper.setBlock(new BlockPos(10, 1, 6), GCBlocks.ENERGY_STORAGE_MODULE.get());
        final EnergyStorageBlockEntity source = (EnergyStorageBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 6));
        final EnergyStorageBlockEntity sink = (EnergyStorageBlockEntity) helper.getBlockEntity(new BlockPos(10, 1, 6));
        source.energy().setStored(100_000.0F);
        helper.setBlock(new BlockPos(3, 1, 6), GCBlocks.BEAM_RECEIVER.get().defaultBlockState().setValue(BeamReceiverBlock.FACING, Direction.WEST));
        helper.setBlock(new BlockPos(9, 1, 6), GCBlocks.BEAM_RECEIVER.get().defaultBlockState().setValue(BeamReceiverBlock.FACING, Direction.EAST));
        // nearer the sender than the receiver is, and nearer the receiver than the sender: sender -> reflector -> receiver
        helper.setBlock(new BlockPos(7, 3, 6), GCBlocks.BEAM_REFLECTOR.get());
        helper.succeedWhen(() -> {
            final BeamReceiverBlockEntity sending = (BeamReceiverBlockEntity) helper.getBlockEntity(new BlockPos(3, 1, 6));
            final BeamReceiverBlockEntity receiving = (BeamReceiverBlockEntity) helper.getBlockEntity(new BlockPos(9, 1, 6));
            helper.assertTrue(sending.mode() == BeamReceiverBlockEntity.Mode.EXTRACT, "sending end on the output");
            helper.assertTrue(receiving.mode() == BeamReceiverBlockEntity.Mode.RECEIVE, "receiving end on the input");
            helper.assertTrue(sending.target() != null && sending.targetPos().equals(helper.absolutePos(new BlockPos(7, 3, 6))), "aimed at the reflector");
            helper.assertTrue(sink.energy().getStoredGJ() > 1_000.0F, "power arrived: " + sink.energy().getStoredGJ());
            helper.assertTrue(source.energy().getStoredGJ() < 100_000.0F, "and left the source");
        });
    }

    /** Two linked telepads: something standing on one is sent to the other after the 150 tick count. */
    @GameTest(template = AIR, timeoutTicks = 260)
    public static void telepadSendsEntities(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos a = new BlockPos(3, 1, 6);
        final BlockPos b = new BlockPos(9, 1, 6);
        for (BlockPos pad : new BlockPos[]{a, b}) {
            helper.setBlock(pad, GCBlocks.SHORT_RANGE_TELEPAD.get());
            ShortRangeTelepadBlock.buildParts(level, helper.absolutePos(pad));
        }
        final int base = 70_000 + level.getRandom().nextInt(10_000) * 2;
        final ShortRangeTelepadBlockEntity from = (ShortRangeTelepadBlockEntity) helper.getBlockEntity(a);
        final ShortRangeTelepadBlockEntity to = (ShortRangeTelepadBlockEntity) helper.getBlockEntity(b);
        from.setAddress(base);
        to.setAddress(base + 1);
        from.setTargetAddress(base + 1);
        from.energy().setStored(16_000.0F);
        to.energy().setStored(16_000.0F);
        final Pig pig = helper.spawn(EntityType.PIG, a.above().getCenter().subtract(0.0D, 0.45D, 0.0D));
        pig.setNoAi(true);
        helper.succeedWhen(() -> {
            final BlockPos target = helper.absolutePos(b);
            helper.assertTrue(pig.blockPosition().getX() == target.getX() && pig.blockPosition().getZ() == target.getZ(),
                    "on the target pad: " + pig.blockPosition() + " / " + target);
            helper.assertTrue(from.energy().getStoredGJ() < 16_000.0F - 2_000.0F, "the jump took its energy");
            from.unregister();
            to.unregister();
        });
    }

    // ------------------------------------------------------------------ the Astro Miner

    private static void buildMinerBase(GameTestHelper helper, BlockPos corner) {
        for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
            helper.setBlock(part, GCBlocks.MINER_BASE.get());
        }
    }

    /** Eight Astro Miner Base blocks in a cube become one base; breaking it gives the eight back. */
    @GameTest(template = AIR, timeoutTicks = 60)
    public static void minerBaseBuildsAndBreaks(GameTestHelper helper) {
        final BlockPos corner = new BlockPos(5, 1, 5);
        buildMinerBase(helper, corner);
        helper.runAfterDelay(5, () -> {
            for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
                helper.assertBlockPresent(GCBlocks.MINER_BASE_FULL.get(), part);
            }
            final MinerBaseBlockEntity top = (MinerBaseBlockEntity) helper.getBlockEntity(corner.offset(1, 1, 1));
            helper.assertTrue(top.getMaster() != null && top.getMaster().isMaster()
                    && top.masterPos().equals(helper.absolutePos(corner)), "mastered by the lowest corner");
            helper.getLevel().destroyBlock(helper.absolutePos(corner.offset(1, 1, 0)), true);
            for (BlockPos part : MinerBaseBlockEntity.parts(corner)) {
                helper.assertBlockNotPresent(GCBlocks.MINER_BASE_FULL.get(), part);
            }
            int dropped = 0;
            for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(corner)).inflate(4.0D))) {
                if (item.getItem().is(GCBlocks.MINER_BASE.get().asItem())) {
                    dropped += item.getItem().getCount();
                }
                item.discard();
            }
            helper.assertValueEqual(dropped, 8, "the eight blocks back");
            helper.succeed();
        });
    }

    /** ItemAstroMiner on a base docks a miner there for the player; knocking it out (creative) frees the base. */
    @GameTest(template = AIR, timeoutTicks = 80)
    public static void astroMinerDocksAtBase(GameTestHelper helper) {
        final BlockPos corner = new BlockPos(5, 1, 3);
        buildMinerBase(helper, corner);
        final ServerPlayer player = player(helper, "miner", new BlockPos(2, 1, 2), GameType.SURVIVAL);
        helper.runAfterDelay(25, () -> {
            final ItemStack item = new ItemStack(GCItems.ASTRO_MINER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            final BlockPos clicked = helper.absolutePos(corner);
            final InteractionResult result = item.getItem().onItemUseFirst(item, new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(clicked.getCenter(), Direction.UP, clicked, false)));
            helper.assertTrue(result.consumesAction(), "docked: " + result);
            helper.assertTrue(item.isEmpty(), "the item is used up");
            helper.assertValueEqual(GCPlayerData.get(player).astroMinerCount, 1, "counted against the player's limit");
            final List<AstroMinerEntity> miners = helper.getLevel().getEntitiesOfClass(AstroMinerEntity.class, new AABB(clicked).inflate(6.0D));
            helper.assertValueEqual(miners.size(), 1, "one miner");
            helper.runAfterDelay(5, () -> {
                final MinerBaseBlockEntity base = (MinerBaseBlockEntity) helper.getBlockEntity(corner);
                helper.assertTrue(base.getMaster().linkedMinerId() != null, "linked to the base");
                player.setGameMode(GameType.CREATIVE);
                miners.get(0).hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
                helper.assertTrue(miners.get(0).isRemoved(), "a creative player removes it");
                helper.assertValueEqual(GCPlayerData.get(player).astroMinerCount, 0, "and the player may have another");
                helper.assertTrue(base.getMaster().linkedMinerId() == null, "and the base is free");
                logOut(helper, player);
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ the abandoned base

    private static List<BasePiece> plan(long seed, int asteroidX, int asteroidZ) {
        return AbandonedBase.plan(RandomSource.create(seed), 8, 8, new AsteroidsChunkGenerator.Asteroid(asteroidX, 120, asteroidZ, 22, false));
    }

    /**
     * MapGenAbandonedBase: three decks with rooms down both sides, or two on a hangar base (a control room deck over the
     * first) with the 42-block hangar off the end and a store among the lower rooms; within reach of the start chunk
     * even off an asteroid 64 blocks away; the same layout for the same seed; one base per 36 x 36 chunks.
     */
    @GameTest(template = "empty")
    public static void abandonedBaseLayouts(GameTestHelper helper) {
        int hangars = 0;
        for (long seed = 1; seed <= 60; seed++) {
            final List<BasePiece> pieces = plan(seed, 8 + 64, 8 - 64);
            helper.assertTrue(pieces.get(0) instanceof BaseDeck first && first.deckTier() == 1, "the lowest deck comes first");
            final boolean hangar = pieces.get(0).configuration().isHangarDeck();
            int decks = 0;
            int rooms = 0;
            int hangarPieces = 0;
            int plates = 0;
            boolean store = false;
            for (BasePiece piece : pieces) {
                final BoundingBox box = piece.getBoundingBox();
                helper.assertTrue(box.minX() >= -128 && box.maxX() < 144 && box.minZ() >= -128 && box.maxZ() < 144,
                        "within 8 chunks of the start chunk (seed " + seed + "): " + box);
                if (piece instanceof BaseDeck) {
                    decks++;
                } else if (piece instanceof BaseRoom room) {
                    rooms++;
                    store |= rooms <= 4 && room.type() == BaseRoom.RoomType.STORE;
                } else if (piece instanceof BaseHangar) {
                    hangarPieces++;
                } else if (piece instanceof BasePlate) {
                    plates++;
                }
            }
            final int roomsNo = pieces.get(0).configuration().roomsNo();
            helper.assertValueEqual(decks, hangar ? 2 : 3, "decks (seed " + seed + ")");
            helper.assertValueEqual(rooms, decks * roomsNo * 2, "rooms down both sides of each deck (seed " + seed + ")");
            helper.assertValueEqual(hangarPieces, hangar ? 1 : 0, "a hangar on hangar bases (seed " + seed + ")");
            helper.assertValueEqual(plates, hangar || pieces.get(0).configuration().deckType().name().equals("HUMANOID") ? 1 : 0, "floor plate");
            if (hangar) {
                hangars++;
                helper.assertTrue(store, "a hangar base has a store on its lower deck (seed " + seed + ")");
            }
        }
        helper.assertTrue(hangars > 5 && hangars < 40, "about a third are hangar bases: " + hangars);
        final List<BasePiece> once = plan(42L, 20, 20);
        final List<BasePiece> again = plan(42L, 20, 20);
        helper.assertValueEqual(once.size(), again.size(), "same seed, same base");
        for (int i = 0; i < once.size(); i++) {
            helper.assertTrue(once.get(i).getBoundingBox().equals(again.get(i).getBoundingBox()), "piece " + i);
        }
        final StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(GCWorldgenProvider.ABANDONED_BASES);
        helper.assertTrue(set != null && set.placement() instanceof RandomSpreadStructurePlacement spread && spread.spacing() == 36
                && spread.separation() == 0, "one per 36 x 36 chunks");
        helper.succeed();
    }

    private static List<BlockEntity> blockEntities(ServerLevel level, BoundingBox box) {
        final List<BlockEntity> found = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (box.isInside(entity.getBlockPos())) {
                        found.add(entity);
                    }
                }
            }
        }
        return found;
    }

    /**
     * A hangar base built out in the open (far from the other tests): its store's Cargo Loader is locked with the
     * spoiled supplies and the base's treasure, by a joined launch pad; the medical room's tanks hold Bacterial Sludge;
     * the NASA Workbenches and Cryogenic Chambers are whole; the engineering rooms hang the Moon Buggy and Tier 2 Rocket
     * schematics on their far walls.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void abandonedBaseIsBuilt(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(4000, 0, 4000);
        // a hangar base with an engineering room, a medical room and cryogenic chambers among its rooms
        List<BasePiece> pieces = null;
        for (long seed = 1; pieces == null; seed++) {
            final List<BasePiece> candidate = AbandonedBase.plan(RandomSource.create(seed), origin.getX(), origin.getZ(), null);
            final java.util.Set<BaseRoom.RoomType> types = java.util.EnumSet.noneOf(BaseRoom.RoomType.class);
            for (BasePiece piece : candidate) {
                if (piece instanceof BaseRoom room) {
                    types.add(room.type());
                }
            }
            if (candidate.get(0).configuration().isHangarDeck()
                    && types.containsAll(List.of(BaseRoom.RoomType.ENGINEERING, BaseRoom.RoomType.MEDICAL, BaseRoom.RoomType.CRYO))) {
                pieces = candidate;
            }
        }
        final BoundingBox everywhere = BoundingBox.encapsulatingBoxes(pieces.stream().map(BasePiece::getBoundingBox).toList()).orElseThrow().inflatedBy(1);
        for (BasePiece piece : pieces) {
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7L), everywhere,
                    new ChunkPos(origin), origin);
        }
        int loaders = 0;
        int tanks = 0;
        int workbenches = 0;
        int chambers = 0;
        int centres = 0;
        int glass = 0;
        int standIns = 0;
        for (BlockEntity entity : blockEntities(level, everywhere)) {
            final BlockState state = entity.getBlockState();
            if (entity instanceof CargoMachineBlockEntity loader && loader.loads()) {
                loaders++;
                helper.assertTrue(loader.isLocked(), "the loader is locked");
                boolean potatoes = false;
                boolean treasure = false;
                for (int slot = 1; slot < CargoMachineBlockEntity.SLOTS; slot++) {
                    final ItemStack stack = loader.inventory().getStackInSlot(slot);
                    potatoes |= stack.is(Items.POISONOUS_POTATO);
                    treasure |= ItemStack.isSameItem(stack, pieces.get(0).configuration().deckType().treasure());
                }
                helper.assertTrue(potatoes && treasure, "spoiled supplies and the treasure");
            } else if (entity instanceof FluidTankBlockEntity tank) {
                tanks++;
                helper.assertTrue(tank.tank().getFluid().getFluid() == GCFluids.BACTERIAL_SLUDGE.get()
                        && tank.tank().getFluidAmount() == FluidTankBlockEntity.CAPACITY, "a tank full of sludge");
            } else if (entity instanceof SolarPartBlockEntity part && state.is(GCBlocks.NASA_WORKBENCH_PART.get())) {
                helper.assertTrue(level.getBlockState(part.mainPos()).is(GCBlocks.NASA_WORKBENCH.get()), "a workbench part points at its workbench");
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(everywhere.minX(), everywhere.minY(), everywhere.minZ(), everywhere.maxX(), everywhere.maxY(), everywhere.maxZ())) {
            final BlockState state = level.getBlockState(pos);
            if (state.is(GCBlocks.NASA_WORKBENCH.get())) {
                workbenches++;
                for (BlockPos part : NasaWorkbenchBlock.partPositions(pos)) {
                    helper.assertTrue(level.getBlockState(part).is(GCBlocks.NASA_WORKBENCH_PART.get()), "a whole workbench at " + pos);
                }
            } else if (state.is(GCBlocks.CRYOGENIC_CHAMBER.get())) {
                chambers++;
                helper.assertTrue(level.getBlockState(pos.above()).is(GCBlocks.CRYOGENIC_CHAMBER_PART.get())
                        && level.getBlockState(pos.above(2)).getValue(CryogenicChamberPartBlock.TOP), "a whole chamber at " + pos);
            } else if (state.is(GCBlocks.LANDING_PAD.get()) && state.getValue(LandingPadBlock.PAD) == LandingPadBlock.Pad.CENTRE) {
                centres++;
            } else if (state.getBlock() instanceof io.github.reiko45.galacticraft.block.SpaceGlassBlock) {
                glass++;
            } else if (state.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN) || state.is(net.minecraft.world.level.block.Blocks.GLASS)) {
                standIns++;
            }
        }
        helper.assertTrue(glass >= 1, "space glass windows");
        helper.assertValueEqual(standIns, 0, "no stand-ins left for space glass and arc lamps");
        helper.assertTrue(loaders >= 1, "a store with its Cargo Loader");
        helper.assertTrue(centres >= 1, "beside a joined launch pad");
        helper.assertTrue(tanks >= 2, "medical tanks: " + tanks);
        helper.assertTrue(workbenches >= 2, "engineering workbenches: " + workbenches);
        helper.assertTrue(chambers >= 2, "cryogenic chambers: " + chambers);
        final List<HangingSchematicEntity> schematics = level.getEntitiesOfClass(HangingSchematicEntity.class, AABB.of(everywhere));
        helper.assertTrue(schematics.size() >= 2, "hanging schematics: " + schematics.size());
        boolean buggy = false;
        boolean rocket = false;
        for (HangingSchematicEntity schematic : schematics) {
            helper.assertTrue(schematic.survives(), "hung on a wall, in open space, at " + schematic.getPos());
            buggy |= schematic.schematicItem().is(GCItems.BUGGY_SCHEMATIC.get());
            rocket |= schematic.schematicItem().is(GCItems.TIER_2_ROCKET_SCHEMATIC.get());
            schematic.discard();
        }
        helper.assertTrue(buggy && rocket, "the Moon Buggy and Tier 2 Rocket schematics");
        helper.succeed();
    }

    // ------------------------------------------------------------------ what the base needed

    /** TileEntityCargoLoader#locked: its items can't be clicked or shift-clicked out, and are lost with the block. */
    @GameTest(template = AIR)
    public static void lockedLoaderKeepsItsCargo(GameTestHelper helper) {
        final BlockPos at = new BlockPos(6, 1, 6);
        helper.setBlock(at, GCBlocks.CARGO_LOADER.get());
        final CargoMachineBlockEntity loader = (CargoMachineBlockEntity) helper.getBlockEntity(at);
        loader.inventory().setStackInSlot(0, new ItemStack(GCItems.BATTERY.get()));
        loader.inventory().setStackInSlot(1, new ItemStack(Items.GHAST_TEAR, 3));
        loader.lock();
        final ServerPlayer player = player(helper, "looter", new BlockPos(6, 1, 4), GameType.SURVIVAL);
        final CargoLoaderMenu menu = new CargoLoaderMenu(true, 1, player.getInventory(), loader);
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && loader.inventory().getStackInSlot(1).is(Items.GHAST_TEAR), "no picking it up");
        helper.assertTrue(menu.quickMoveStack(player, 1).isEmpty() && loader.inventory().getStackInSlot(1).getCount() == 3, "no shift-clicking it out");
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        boolean battery = false;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(3.0D))) {
            helper.assertFalse(item.getItem().is(Items.GHAST_TEAR), "the locked items are lost");
            battery |= item.getItem().is(GCItems.BATTERY.get());
            item.discard();
        }
        helper.assertTrue(battery, "the battery drops");
        logOut(helper, player);
        helper.succeed();
    }

    /** ItemSchematic#onItemUse: a schematic hangs on a wall as a 2 x 2 painting, and drops again when knocked down. */
    @GameTest(template = AIR)
    public static void schematicHangsOnWalls(GameTestHelper helper) {
        for (BlockPos wall : BlockPos.betweenClosed(new BlockPos(4, 1, 8), new BlockPos(7, 4, 8))) {
            helper.setBlock(wall, Blocks.STONE);
        }
        final ServerPlayer player = player(helper, "decorator", new BlockPos(6, 1, 5), GameType.SURVIVAL);
        final ItemStack schematic = new ItemStack(GCItems.TIER_3_ROCKET_SCHEMATIC.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, schematic);
        final BlockPos wall = helper.absolutePos(new BlockPos(6, 2, 8));
        final InteractionResult result = schematic.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(wall.getCenter().relative(Direction.NORTH, 0.5D), Direction.NORTH, wall, false)));
        helper.assertTrue(result.consumesAction() && schematic.isEmpty(), "hung: " + result);
        final List<HangingSchematicEntity> hung = helper.getLevel().getEntitiesOfClass(HangingSchematicEntity.class, new AABB(wall).inflate(3.0D));
        helper.assertValueEqual(hung.size(), 1, "one schematic on the wall");
        helper.assertTrue(hung.get(0).getDirection() == Direction.NORTH && hung.get(0).survives(), "facing out from the wall");
        helper.assertTrue(hung.get(0).schematicItem().is(GCItems.TIER_3_ROCKET_SCHEMATIC.get()), "the same schematic");
        hung.get(0).hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(hung.get(0).isRemoved(), "knocked down");
        boolean dropped = false;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(wall).inflate(3.0D))) {
            dropped |= item.getItem().is(GCItems.TIER_3_ROCKET_SCHEMATIC.get());
            item.discard();
        }
        helper.assertTrue(dropped, "the schematic drops");
        logOut(helper, player);
        helper.succeed();
    }

    /** BlockSlabGC asteroids_slab: a slab of the Dark Decoration Block, cut from it in a stonecutter. */
    @GameTest(template = "empty")
    public static void darkDecorationSlab(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(GalacticraftCore.id("stonecutting/dark_decoration_slab_from_dark_decoration_block"))
                .isPresent(), "a stonecutter recipe");
        helper.assertTrue(GCBlocks.DARK_DECORATION_SLAB.get().asItem() != Items.AIR, "with an item");
        helper.succeed();
    }
}
