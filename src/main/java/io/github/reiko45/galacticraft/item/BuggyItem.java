package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.entity.BuggyEntity;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Moon Buggy (1.12.2 ItemBuggy). Used on a block (or liquid surface) within 5 blocks it sets a buggy down on top. The
 * storage component replaces the 1.12.2 buggy types 0-3 (0, 18, 36 or 54 storage slots); fuel stays with the item.
 */
public class BuggyItem extends Item implements HoldableItem {
    public BuggyItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static ItemStack create(int storage, int fuel) {
        final ItemStack stack = new ItemStack(GCItems.BUGGY.get());
        stack.set(GCDataComponents.ROCKET_STORAGE.get(), storage);
        if (fuel > 0) {
            stack.set(GCDataComponents.ROCKET_FUEL.get(), fuel);
        }
        return stack;
    }

    public static int storage(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.ROCKET_STORAGE.get(), 0);
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.ROCKET_FUEL.get(), 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        final BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        int y = hit.getBlockPos().getY();
        if (level.getBlockState(hit.getBlockPos()).is(Blocks.SNOW)) {
            y--;
        }
        final BuggyEntity buggy = BuggyEntity.create(level, hit.getBlockPos().getX() + 0.5D, y + 1.0D, hit.getBlockPos().getZ() + 0.5D,
                storage(stack), fuel(stack));
        buggy.setYRot(player.getYRot());
        if (!level.noCollision(buggy, buggy.getBoundingBox().deflate(0.1D))) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            level.addFreshEntity(buggy);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        final int storage = storage(stack);
        if (storage > 0) {
            tooltip.add(Component.translatable("gui.galacticraftcore.buggy.storage_space").append(": " + storage * 18));
        }
        if (stack.has(GCDataComponents.ROCKET_FUEL.get())) {
            tooltip.add(Component.translatable("gui.galacticraftcore.message.fuel").append(": " + fuel(stack) + " / " + BuggyEntity.FUEL_CAPACITY));
        }
    }
}
