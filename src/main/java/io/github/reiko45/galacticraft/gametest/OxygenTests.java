package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.BreathableAirBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.OxygenStorageModuleBlock;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenCollectorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDecompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenStorageModuleBlockEntity;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.machine.StorageSides;
import io.github.reiko45.galacticraft.oxygen.OxygenUtil;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.player.GCPlayerHandler;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 3: oxygen machines, sealing, bubbles and breathing. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OxygenTests {
    private static final String ROOM = "machine_room";

    private OxygenTests() {
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T machine(GameTestHelper helper, net.minecraft.world.level.block.Block block, BlockPos pos, Direction facing) {
        helper.setBlock(pos, block.defaultBlockState().setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(pos);
    }

    /** In breathable air the collector gathers 93 mB a time and pushes it out of its right face into a storage module. */
    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void collectorFeedsStorageModule(GameTestHelper helper) {
        // Collector facing north: power port east, oxygen out west. Storage module facing north takes oxygen in on its
        // left (east) face by default.
        final OxygenStorageModuleBlockEntity storage = machine(helper, GCBlocks.OXYGEN_STORAGE_MODULE.get(), new BlockPos(2, 1, 1), Direction.NORTH);
        helper.assertBlockProperty(new BlockPos(2, 1, 1), OxygenStorageModuleBlock.SIDES, StorageSides.LEFT_RIGHT);
        final OxygenCollectorBlockEntity collector = machine(helper, GCBlocks.OXYGEN_COLLECTOR.get(), new BlockPos(3, 1, 1), Direction.NORTH);
        collector.energy().setStored(16_000.0F);
        helper.succeedWhen(() -> helper.assertTrue(storage.oxygenStored() >= 93, "storage module received oxygen: " + storage.oxygenStored()));
    }

    /** The compressor fills a tank 2 mB per tick from its store. */
    @GameTest(template = ROOM, timeoutTicks = 100)
    public static void compressorFillsTank(GameTestHelper helper) {
        final OxygenCompressorBlockEntity compressor = machine(helper, GCBlocks.OXYGEN_COMPRESSOR.get(), new BlockPos(3, 1, 1), Direction.NORTH);
        compressor.energy().setStored(16_000.0F);
        compressor.setOxygenStored(1_000);
        compressor.inventory().setStackInSlot(0, new ItemStack(GCItems.LIGHT_OXYGEN_TANK.get()));
        helper.succeedWhen(() -> {
            final int filled = OxygenTankItem.oxygen(compressor.inventory().getStackInSlot(0));
            helper.assertTrue(filled >= 40, "tank filled: " + filled);
            helper.assertValueEqual(filled + compressor.oxygenStored(), 1_000, "oxygen conserved");
        });
    }

    /** The decompressor empties a tank and pushes the oxygen through a pipe into a fluid tank. */
    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void decompressorEmptiesTankIntoPipe(GameTestHelper helper) {
        // Facing north: oxygen leaves through the west face.
        final OxygenDecompressorBlockEntity decompressor = machine(helper, GCBlocks.OXYGEN_DECOMPRESSOR.get(), new BlockPos(3, 1, 1), Direction.NORTH);
        decompressor.energy().setStored(16_000.0F);
        decompressor.inventory().setStackInSlot(0, GCItems.LIGHT_OXYGEN_TANK.get().full());
        helper.setBlock(new BlockPos(2, 1, 1), GCBlocks.FLUID_PIPE.get());
        helper.setBlock(new BlockPos(1, 1, 1), GCBlocks.FLUID_TANK.get());
        final FluidTankBlockEntity tank = helper.getBlockEntity(new BlockPos(1, 1, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(tank.tank().getFluid().is(GCFluids.OXYGEN.get()) && tank.tank().getFluidAmount() >= 50,
                    "oxygen arrived in the tank: " + tank.tank().getFluidAmount());
            helper.assertTrue(OxygenTankItem.oxygen(decompressor.inventory().getStackInSlot(0)) < 900, "tank was drained");
        });
    }

    /** A sealer inside a closed stone room fills it with breathable air. */
    @GameTest(template = "stone_room", timeoutTicks = 600)
    public static void sealerSealsClosedRoom(GameTestHelper helper) {
        final OxygenSealerBlockEntity sealer = buildRoom(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(sealer.isSealed(), "room sealed");
            for (BlockPos pos : BlockPos.betweenClosed(4, 3, 4, 6, 4, 6)) {
                helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof BreathableAirBlock, "breathable air at " + pos);
            }
            helper.assertTrue(OxygenUtil.isInBreathableAir(helper.getLevel(), new AABB(helper.absolutePos(new BlockPos(5, 3, 5)))),
                    "an entity in the room can breathe");
        });
    }

    /**
     * A sealer in a 13x9x13 open room (1,521 blocks of air, more than one sealer's 1,250 checks; GameTest walls the test
     * area with barriers): "Area Too Big / Unsealed", and no breathable air is left behind.
     */
    @GameTest(template = "air_room", timeoutTicks = 500)
    public static void sealerFailsWhenSpaceTooBig(GameTestHelper helper) {
        final OxygenSealerBlockEntity sealer = machine(helper, GCBlocks.OXYGEN_SEALER.get(), new BlockPos(6, 1, 6), Direction.NORTH);
        sealer.energy().setStored(32_000.0F);
        sealer.setOxygenStored(10_000);
        helper.runAfterDelay(400, () -> {
            helper.assertFalse(sealer.isSealed(), "too big to seal");
            helper.assertTrue(sealer.status() != io.github.reiko45.galacticraft.machine.MachineStatus.SEALED, "status: " + sealer.status());
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 6));
            helper.succeed();
        });
    }

    /** Hollow out a 3x3x3 room in the stone with a powered sealer on its floor. */
    private static OxygenSealerBlockEntity buildRoom(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(4, 2, 4, 6, 4, 6)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        final OxygenSealerBlockEntity sealer = machine(helper, GCBlocks.OXYGEN_SEALER.get(), new BlockPos(5, 2, 5), Direction.NORTH);
        sealer.energy().setStored(32_000.0F);
        sealer.setOxygenStored(10_000);
        return sealer;
    }

    /** A powered distributor's bubble grows by 0.01 per tick and makes the air around it breathable. */
    @GameTest(template = ROOM, timeoutTicks = 300)
    public static void distributorBubbleGrows(GameTestHelper helper) {
        final OxygenDistributorBlockEntity distributor = machine(helper, GCBlocks.OXYGEN_DISTRIBUTOR.get(), new BlockPos(3, 1, 1), Direction.NORTH);
        distributor.energy().setStored(16_000.0F);
        distributor.setOxygenStored(6_000);
        final BlockPos centre = helper.absolutePos(new BlockPos(3, 1, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(distributor.bubbleSize() >= 1.5F, "bubble radius " + distributor.bubbleSize());
            helper.assertTrue(OxygenUtil.inOxygenBubble(helper.getLevel(), centre.getX() + 1.4D, centre.getY() + 0.5D, centre.getZ() + 0.5D),
                    "a point 0.9 blocks from the centre is inside");
            helper.assertFalse(OxygenUtil.inOxygenBubble(helper.getLevel(), centre.getX() + 5.5D, centre.getY() + 0.5D, centre.getZ() + 0.5D),
                    "a point 5 blocks away is not");
        });
    }

    /** Without gear a player on an airless world suffocates; with mask, gear and a tank they breathe from the tank. */
    @GameTest(template = "empty")
    public static void playerBreathesFromTank(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // New players are invulnerable for 60 ticks; this one never ticks
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
        final GCPlayerData data = GCPlayerData.get(player);
        final float health = player.getHealth();
        GCPlayerHandler.checkOxygen(player, data, true);
        helper.assertFalse(data.oxygenSetupValid(), "no gear, no oxygen");
        helper.assertTrue(player.getHealth() < health, "suffocation damage taken");

        // Right-clicking gear puts it into the Galacticraft inventory
        for (ItemStack gear : new ItemStack[]{new ItemStack(GCItems.OXYGEN_MASK.get()), new ItemStack(GCItems.OXYGEN_GEAR.get()),
                GCItems.LIGHT_OXYGEN_TANK.get().full()}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, gear);
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                    .getObject().isEmpty(), "equipped " + gear);
        }
        helper.assertTrue(data.gear(GearSlot.LEFT_TANK).is(GCItems.LIGHT_OXYGEN_TANK.get()), "tank in the left tank slot");
        player.tickCount = 1; // a breathing tick
        GCPlayerHandler.checkOxygen(player, data, true);
        helper.assertTrue(data.oxygenSetupValid(), "mask + gear + tank works");
        helper.assertValueEqual(OxygenTankItem.oxygen(data.gear(GearSlot.LEFT_TANK)), 899, "one unit breathed");
        helper.succeed();
    }

    /** A space torch on a world with air turns back into a vanilla torch. */
    @GameTest(template = ROOM)
    public static void spaceTorchBecomesTorchInAir(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 1), GCBlocks.UNLIT_TORCH.get());
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.TORCH, new BlockPos(3, 1, 1)));
    }
}
