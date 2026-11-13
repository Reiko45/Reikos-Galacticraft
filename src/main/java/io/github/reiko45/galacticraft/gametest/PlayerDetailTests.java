package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.FallenMeteorBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.SidedMachineBlock;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.HoldableItem;
import io.github.reiko45.galacticraft.machine.StorageSides;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.world.station.Freefall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10 part 3: Fuel Loader faces, the zero-g jetpack push, the Fallen Meteor's mining rate, carrying and waving. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerDetailTests {
    private static final String AIR = "air_room";

    private PlayerDetailTests() {
    }

    private static boolean power(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null;
    }

    private static IFluidHandler fuel(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    /**
     * 1.12.2 IMachineSides on the Fuel Loader: power in on the left and fuel in on the right of the front by default,
     * and sneak + wrench steps them (L,R) -> (B,R) -> (B,L) -> (R,L) -> (R,B) -> (L,B) -> (L,R).
     */
    @GameTest(template = AIR)
    public static void fuelLoaderFacesCycle(GameTestHelper helper) {
        final BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final BlockPos pos = helper.absolutePos(rel);
        final SidedMachineBlock block = GCBlocks.FUEL_LOADER.get();
        helper.assertBlockProperty(rel, SidedMachineBlock.SIDES, StorageSides.LEFT_RIGHT);
        // Facing north, left is east and right is west
        helper.assertTrue(power(helper, pos, Direction.EAST) && !power(helper, pos, Direction.SOUTH) && !power(helper, pos, Direction.WEST),
                "power in on the left only");
        helper.assertTrue(fuel(helper, pos, Direction.WEST) != null && fuel(helper, pos, Direction.EAST) == null, "fuel in on the right only");

        block.cycleSides(helper.getLevel(), pos, helper.getLevel().getBlockState(pos));
        helper.assertBlockProperty(rel, SidedMachineBlock.SIDES, StorageSides.BACK_RIGHT);
        helper.assertTrue(power(helper, pos, Direction.SOUTH) && !power(helper, pos, Direction.EAST), "power in moves to the back");
        helper.assertTrue(fuel(helper, pos, Direction.WEST) != null, "fuel stays on the right");

        block.cycleSides(helper.getLevel(), pos, helper.getLevel().getBlockState(pos));
        helper.assertBlockProperty(rel, SidedMachineBlock.SIDES, StorageSides.BACK_LEFT);
        helper.assertTrue(fuel(helper, pos, Direction.WEST) == null, "fuel has left the right");
        final IFluidHandler left = fuel(helper, pos, Direction.EAST);
        helper.assertTrue(left != null, "fuel in on the left");
        left.fill(new FluidStack(GCFluids.FUEL.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
        final FuelLoaderBlockEntity loader = helper.getBlockEntity(rel);
        helper.assertValueEqual(loader.fuelTank().getFluidAmount(), 1_000, "fuel taken through the new face");

        final StorageSides[] rest = {StorageSides.RIGHT_LEFT, StorageSides.RIGHT_BACK, StorageSides.LEFT_BACK, StorageSides.LEFT_RIGHT};
        for (StorageSides sides : rest) {
            block.cycleSides(helper.getLevel(), pos, helper.getLevel().getBlockState(pos));
            helper.assertBlockProperty(rel, SidedMachineBlock.SIDES, sides);
        }
        helper.assertTrue(power(helper, pos, Direction.EAST) && fuel(helper, pos, Direction.WEST) != null, "round to the start");
        helper.succeed();
    }

    /**
     * 1.12.2 FreefallHandler: an upward kick while floating with no sideways change (another mod's jetpack) is turned
     * into a push the way the player faces, and jump adds no lift that tick; a kick with a sideways change is halved.
     */
    @GameTest(template = AIR)
    public static void jetpackKickBecomesForwardPush(GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final Vec3 start = helper.absoluteVec(new Vec3(4.5D, 4.0D, 2.5D));
        player.setPos(start);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.tickCount = 100;
        player.setDeltaMovement(Vec3.ZERO);
        Freefall.travel(player, false);

        // A jetpack lifts the player 0.2 between ticks: facing +Z level, all of it becomes a 0.02 push forwards
        player.tickCount++;
        player.setDeltaMovement(0.0D, 0.2D, 0.0D);
        Freefall.travel(player, true);
        Vec3 motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.y) < 1.0E-6D, "the lift is taken out, jump adds none: " + motion.y);
        helper.assertTrue(Math.abs(motion.z - 0.02D) < 1.0E-6D && Math.abs(motion.x) < 1.0E-6D, "pushed forwards: " + motion);

        // Looking straight down the kick is pushed downwards a fifth more and gives no forward push
        player.tickCount++;
        player.setXRot(90.0F);
        player.setDeltaMovement(motion.add(0.0D, 0.2D, 0.0D));
        Freefall.travel(player, false);
        motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.y + 0.04D) < 1.0E-6D, "a fifth more taken out looking down: " + motion.y);
        helper.assertTrue(Math.abs(motion.z - 0.02D) < 1.0E-6D, "no more push looking down: " + motion.z);

        // A kick that also moves the player sideways is only halved, and jump lifts as usual
        player.tickCount++;
        player.setXRot(0.0F);
        final double before = motion.y;
        player.setDeltaMovement(0.1D, 0.2D, 0.0D);
        Freefall.travel(player, true);
        motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.y - (0.2D - (0.2D - before) / 2.0D + 0.0032D)) < 1.0E-6D, "halved plus the jump lift: " + motion.y);
        helper.assertTrue(Math.abs(motion.x - 0.1D) < 1.0E-6D, "sideways kept: " + motion.x);

        // After a gap in floating the next kick counts as nothing special
        player.tickCount += 5;
        player.setDeltaMovement(0.0D, 0.2D, 0.0D);
        Freefall.travel(player, false);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - 0.2D) < 1.0E-6D, "not floating last tick: kept");
        helper.succeed();
    }

    /**
     * 1.12.2 BlockFallenMeteor#getPlayerRelativeBlockHardness: speed / 40 / 30 for anyone (vanilla: / 100 without the
     * right tool), a pickaxe at its full speed whatever its tier, and pickaxes past diamond times (level - 2).
     */
    @GameTest(template = AIR)
    public static void fallenMeteorMiningRate(GameTestHelper helper) {
        final BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, GCBlocks.FALLEN_METEOR.get());
        final BlockPos pos = helper.absolutePos(rel);
        final BlockState meteor = helper.getLevel().getBlockState(pos);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(4.5D, 1.0D, 2.5D)));
        final Object[][] tools = {
                {ItemStack.EMPTY, -1, 1}, {new ItemStack(Items.IRON_SWORD), -1, 1}, {new ItemStack(Items.WOODEN_PICKAXE), 0, 1},
                {new ItemStack(Items.IRON_PICKAXE), 2, 1}, {new ItemStack(Items.DIAMOND_PICKAXE), 3, 1},
                {new ItemStack(Items.NETHERITE_PICKAXE), 4, 2}, {new ItemStack(GCItems.HEAVY_DUTY_PICKAXE.get()), 3, 1},
                {new ItemStack(GCItems.DESH_PICKAXE.get()), 3, 1}, {new ItemStack(GCItems.VOLCANIC_PICKAXE.get()), 3, 1},
                {new ItemStack(GCItems.TITANIUM_PICKAXE.get()), 4, 2}};
        for (Object[] tool : tools) {
            final ItemStack stack = (ItemStack) tool[0];
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertValueEqual(FallenMeteorBlock.pickaxeLevel(stack), (int) tool[1], "level of " + stack);
            final float speed = player.getDigSpeed((int) tool[1] >= 0 ? Blocks.STONE.defaultBlockState() : meteor, pos);
            final float expected = (int) tool[2] * speed / 40.0F / 30.0F;
            final float progress = meteor.getDestroyProgress(player, helper.getLevel(), pos);
            helper.assertTrue(Math.abs(progress - expected) < 1.0E-7F, stack + ": " + progress + " not " + expected);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        final float hand = meteor.getDestroyProgress(player, helper.getLevel(), pos);
        helper.assertTrue(Math.abs(hand - player.getDigSpeed(meteor, pos) / 1200.0F) < 1.0E-7F, "bare hand at the 1/30 rate");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        final float iron = meteor.getDestroyProgress(player, helper.getLevel(), pos);
        helper.assertTrue(Math.abs(iron / hand - 6.0F) < 1.0E-3F, "an iron pickaxe at its full 6x speed: " + iron / hand);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.TITANIUM_PICKAXE.get()));
        final float titanium = meteor.getDestroyProgress(player, helper.getLevel(), pos);
        helper.assertTrue(Math.abs(titanium / hand - 28.0F) < 1.0E-2F, "titanium: speed 14, twice over: " + titanium / hand);
        helper.succeed();
    }

    /** 1.12.2 IHoldableItem: rockets, the buggy and the Astro Miner are carried overhead crouching, the flag standing. */
    @GameTest(template = AIR)
    public static void overheadItemsAndCrouching(GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack[] crouching = {new ItemStack(GCItems.TIER_1_ROCKET.get()), new ItemStack(GCItems.TIER_2_ROCKET.get()),
                new ItemStack(GCItems.TIER_3_ROCKET.get()), new ItemStack(GCItems.CARGO_ROCKET.get()), new ItemStack(GCItems.BUGGY.get()),
                new ItemStack(GCItems.ASTRO_MINER.get())};
        for (ItemStack stack : crouching) {
            helper.assertTrue(stack.getItem() instanceof HoldableItem holdable && holdable.holdsLeftHandUp(player)
                    && holdable.holdsRightHandUp(player) && holdable.crouches(player), stack + " carried overhead, crouching");
        }
        helper.assertTrue(GCItems.FLAG.get() instanceof HoldableItem holdable && holdable.holdsLeftHandUp(player)
                && holdable.holdsRightHandUp(player) && !holdable.crouches(player), "the flag carried overhead, standing");
        helper.assertFalse(Items.STONE instanceof HoldableItem, "ordinary items are not");
        helper.succeed();
    }

    /** 1.12.2 ModelBipedGC: onlookers within 14 blocks wave at a crewed rocket from 10 ticks into its countdown. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void onlookersWaveAtLaunchingRocket(GameTestHelper helper) {
        final BlockPos centre = helper.absolutePos(new BlockPos(6, 1, 6));
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), centre.getX() + 0.5D, centre.getY() + 0.4D,
                centre.getZ() + 0.5D, 0, Tier1RocketEntity.FUEL_CAPACITY);
        rocket.setNoGravity(true);
        helper.getLevel().addFreshEntity(rocket);
        final Player near = helper.makeMockPlayer(GameType.SURVIVAL);
        near.setPos(Vec3.atBottomCenterOf(centre).add(5.0D, 0.0D, 0.0D));
        final Player far = helper.makeMockPlayer(GameType.SURVIVAL);
        far.setPos(Vec3.atBottomCenterOf(centre).add(15.0D, 0.0D, 0.0D));
        helper.assertFalse(rocket.wavedAtBy(near), "no one aboard");
        final ServerPlayer crew = helper.makeMockServerPlayerInLevel();
        crew.startRiding(rocket, true);
        helper.assertFalse(rocket.wavedAtBy(near), "on the pad, not ignited");
        rocket.ignite();
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.IGNITED, "ignited");
        helper.assertFalse(rocket.wavedAtBy(near), "the countdown has only just started");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(rocket.countdown() < 390, "countdown " + rocket.countdown());
            helper.assertTrue(rocket.wavedAtBy(near), "a player 5 blocks off waves");
            helper.assertFalse(rocket.wavedAtBy(far), "15 blocks off is too far");
            helper.assertFalse(rocket.wavedAtBy(crew), "the crew don't wave at themselves");
            crew.stopRiding();
            rocket.discard();
            helper.succeed();
        });
    }
}
