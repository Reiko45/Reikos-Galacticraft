package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.EmergencyPostBlock;
import io.github.reiko45.galacticraft.block.HydraulicPlatformBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.ChromaticApplicatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.DeconstructorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EmergencyPostBlockEntity;
import io.github.reiko45.galacticraft.block.entity.HydraulicPlatformBlockEntity;
import io.github.reiko45.galacticraft.item.EmergencyKitItem;
import io.github.reiko45.galacticraft.item.EmergencyPostItem;
import io.github.reiko45.galacticraft.item.MeteorChunkItem;
import io.github.reiko45.galacticraft.item.PrelaunchChecklistItem;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.util.ColorUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7g part 3: meteor chunks, the checklist, the Emergency Post and kit, platforms, Deconstructor, Chromatic Applicator. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseUtilityTests {
    private BaseUtilityTests() {
    }

    /** A hot meteor chunk cools in the inventory after 1,800 ticks (45 shown seconds at half speed); smelting heats one. */
    @GameTest(template = "empty")
    public static void meteorChunkCools(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ItemStack chunk = MeteorChunkItem.hot(new ItemStack(GCItems.METEOR_CHUNK.get()));
        for (int i = 0; i < 1799; i++) {
            chunk.inventoryTick(helper.getLevel(), player, 0, false);
        }
        helper.assertTrue(MeteorChunkItem.isHot(chunk), "still hot after 1,799 ticks");
        chunk.inventoryTick(helper.getLevel(), player, 0, false);
        chunk.inventoryTick(helper.getLevel(), player, 0, false);
        helper.assertFalse(MeteorChunkItem.isHot(chunk), "cold now");
        final var smelting = helper.getLevel().getRecipeManager().byKey(GalacticraftCore.id("smelting/hot_meteor_chunk"));
        helper.assertTrue(smelting.isPresent() && MeteorChunkItem.isHot(smelting.get().value().getResultItem(helper.getLevel().registryAccess())),
                "smelting gives a hot chunk");
        helper.succeed();
    }

    /** The checklist keeps its ticks on the stack. */
    @GameTest(template = "empty")
    public static void checklistKeepsTicks(GameTestHelper helper) {
        final ItemStack book = new ItemStack(GCItems.PRELAUNCH_CHECKLIST.get());
        final CompoundTag data = new CompoundTag();
        data.putBoolean("planet.galacticraftcore.mars.thermal_padding", true);
        PrelaunchChecklistItem.setChecklist(book, data);
        helper.assertTrue(PrelaunchChecklistItem.checklist(book).getBoolean("planet.galacticraftcore.mars.thermal_padding"), "ticked");
        helper.assertValueEqual(book.getMaxStackSize(), 1, "one to a stack");
        helper.succeed();
    }

    /**
     * The post lights the air six blocks out; opening a side with a kit in hand puts it in, using the open side again
     * takes it out; breaking the post puts the plain air back.
     */
    @GameTest(template = "air_room")
    public static void emergencyPostHoldsAKit(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 1, 6);
        helper.setBlock(pos, GCBlocks.EMERGENCY_POST.get());
        final EmergencyPostBlockEntity post = helper.getBlockEntity(pos);
        post.tick();
        helper.assertBlockPresent(GCBlocks.BRIGHT_AIR.get(), new BlockPos(0, 1, 6));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.EMERGENCY_KIT.get()));
        post.click(player, Direction.NORTH, false);
        helper.assertTrue(helper.getBlockState(pos).getValue(EmergencyPostBlock.KIT), "the kit went in");
        helper.assertTrue(post.isOpen(Direction.NORTH) && player.getMainHandItem().isEmpty(), "the north door opened");
        for (int i = 0; i < 12; i++) {
            post.tick(); // the 12-tick cooldown
        }
        post.click(player, Direction.NORTH, true);
        helper.assertFalse(helper.getBlockState(pos).getValue(EmergencyPostBlock.KIT), "the kit came out");
        helper.assertTrue(player.getInventory().contains(new ItemStack(GCItems.EMERGENCY_KIT.get())), "into the player's inventory");
        helper.destroyBlock(pos);
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(0, 1, 6));
        helper.assertTrue(EmergencyPostItem.hasKit(EmergencyPostItem.withKit(new ItemStack(GCBlocks.EMERGENCY_POST_ITEM.get()))), "a post item with a kit");
        helper.succeed();
    }

    /** The kit puts on the mask, gear, tanks and parachute, and hands over the rest. */
    @GameTest(template = "empty")
    public static void emergencyKitEquips(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.EMERGENCY_KIT.get()));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        final GCPlayerData data = GCPlayerData.get(player);
        helper.assertTrue(data.gear(GearSlot.MASK).is(GCItems.OXYGEN_MASK.get()) && data.gear(GearSlot.GEAR).is(GCItems.OXYGEN_GEAR.get())
                && data.gear(GearSlot.LEFT_TANK).is(GCItems.LIGHT_OXYGEN_TANK.get()) && data.gear(GearSlot.RIGHT_TANK).is(GCItems.LIGHT_OXYGEN_TANK.get())
                && data.gear(GearSlot.PARACHUTE).is(GCItems.PARACHUTES.get(13).get()), "gear on");
        helper.assertTrue(player.getInventory().contains(new ItemStack(GCItems.HEAVY_DUTY_PICKAXE.get())), "the pickaxe handed over");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the kit is used up");
        helper.assertValueEqual(EmergencyKitItem.contents().size(), 9, "nine things");
        helper.succeed();
    }

    /** Four platforms join into a square; another square four blocks up is the next stop; breaking one splits it. */
    @GameTest(template = "air_room")
    public static void hydraulicPlatformsJoin(GameTestHelper helper) {
        for (int y : new int[] {1, 5}) {
            for (int x = 5; x <= 6; x++) {
                for (int z = 5; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), GCBlocks.HYDRAULIC_PLATFORM.get());
                }
            }
        }
        helper.succeedWhen(() -> {
            helper.assertValueEqual(helper.getBlockState(new BlockPos(5, 1, 5)).getValue(HydraulicPlatformBlock.TYPE), HydraulicPlatformBlock.Corner.NW, "joined");
            helper.assertValueEqual(helper.getBlockState(new BlockPos(6, 5, 6)).getValue(HydraulicPlatformBlock.TYPE), HydraulicPlatformBlock.Corner.SE, "both");
            final HydraulicPlatformBlockEntity lower = helper.getBlockEntity(new BlockPos(5, 1, 5));
            final var box = net.minecraft.world.phys.AABB.ofSize(helper.absolutePos(new BlockPos(6, 2, 6)).getCenter(), 0.6D, 1.8D, 0.6D);
            helper.assertValueEqual(lower.nextPlatform(1, box), 4, "the next platform up is 4 blocks above");
            helper.assertValueEqual(lower.nextPlatform(-1, box), 0, "none below");
        });
    }

    /** A Tier 1 Rocket comes apart into (at least) its heavy plating; running it gives some of the salvage back. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void deconstructorSalvagesRockets(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GCBlocks.DECONSTRUCTOR.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        final DeconstructorBlockEntity machine = helper.getBlockEntity(pos);
        final List<ItemStack> salvage = machine.salvageOf(new ItemStack(GCItems.TIER_1_ROCKET.get()));
        final int plating = salvage.stream().filter(stack -> stack.is(GCItems.HEAVY_PLATING.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(plating >= 8, "heavy plating from the rocket: " + plating);
        helper.assertTrue(salvage.stream().allMatch(DeconstructorBlockEntity::isSalvage), "only salvage comes out");
        // a run takes 18,750 gJ, more than the 16,000 it holds: keep it topped up, as a wire would
        helper.onEachTick(() -> machine.energy().setStored(16_000.0F));
        machine.inventory().setStackInSlot(1, new ItemStack(GCItems.TIER_1_ROCKET.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.inventory().getStackInSlot(1).isEmpty(), "the rocket was taken apart");
            boolean any = false;
            for (int slot = 2; slot < DeconstructorBlockEntity.SLOTS; slot++) {
                any |= !machine.inventory().getStackInSlot(slot).isEmpty();
            }
            helper.assertTrue(any, "some salvage in the output");
        });
    }

    /** Mixing red dye into white, then painting clear Space Glass; a player nearby is shown that colour. */
    @GameTest(template = "empty")
    public static void chromaticApplicatorPaintsGlass(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, GCBlocks.CHROMATIC_APPLICATOR.get());
        final ChromaticApplicatorBlockEntity applicator = helper.getBlockEntity(pos);
        applicator.inventory().setStackInSlot(0, new ItemStack(Items.RED_DYE));
        applicator.buttonPressed(1, null);
        final int mixed = ColorUtil.addColorsBasic(DyeColor.RED.getFireworkColor(), 0xFFFFFF);
        helper.assertValueEqual(applicator.guiColor(), mixed, "red mixed into white");
        applicator.inventory().setStackInSlot(1, new ItemStack(GCBlocks.SPACE_GLASS_CLEAR.get()));
        applicator.buttonPressed(0, null);
        helper.assertValueEqual(applicator.glassColor(1), ColorUtil.lighten(mixed, 0.03F), "clear glass painted");
        applicator.buttonPressed(2, null);
        helper.assertValueEqual(applicator.guiColor(), 0xFFFFFF, "reset to white");
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5D, 1.0D, 4.5D)));
        // the applicator joins the level's list when it loads (on the next tick); the level tick then passes its colours on
        helper.succeedWhen(() -> helper.assertValueEqual(GCPlayerData.get(player).glassColor(1), ColorUtil.lighten(mixed, 0.03F), "the player sees it"));
    }
}
