package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.block.LandingPadBlock;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A rocket item (1.12.2 ItemTier1Rocket, ItemTier2Rocket). Used on a joined launch pad, it stands a rocket on the pad's
 * centre. The storage component replaces the 1.12.2 rocket types 0-3 (0, 18, 36 or 54 cargo slots); the creative-only
 * pre-fuelled rocket is a plain rocket with a full tank.
 */
public class RocketItem extends Item implements HoldableItem {
    /** Makes the rocket entity for this item. */
    @FunctionalInterface
    public interface Factory {
        RocketEntity create(Level level, double x, double y, double z, int storage, int fuel);
    }

    private final int fuelCapacity;
    private final double padOffset;
    private final Factory factory;

    /**
     * @param padOffset 1.12.2 getOnPadYOffset: how far above (or below) the usual 0.4 over the pad the rocket stands
     */
    public RocketItem(Properties properties, int fuelCapacity, double padOffset, Factory factory) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
        this.fuelCapacity = fuelCapacity;
        this.padOffset = padOffset;
        this.factory = factory;
    }

    public ItemStack stack(int storage, int fuel) {
        final ItemStack stack = new ItemStack(this);
        stack.set(GCDataComponents.ROCKET_STORAGE.get(), storage);
        if (fuel > 0) {
            stack.set(GCDataComponents.ROCKET_FUEL.get(), fuel);
        }
        return stack;
    }

    public int fuelCapacity() {
        return this.fuelCapacity;
    }

    public static int storage(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.ROCKET_STORAGE.get(), 0);
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.ROCKET_FUEL.get(), 0);
    }

    /** 1.12.2 onItemUse: the clicked block or one next to it must be the centre of a joined pad with no rocket on it. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Level level = context.getLevel();
        BlockPos centre = null;
        for (BlockPos pos : BlockPos.betweenClosed(context.getClickedPos().offset(-1, 0, -1), context.getClickedPos().offset(1, 0, 1))) {
            final BlockPos found = LandingPadBlock.findLaunchPadCentre(level, pos);
            if (found != null && found.equals(pos)) {
                centre = found.immutable();
                break;
            }
        }
        if (centre == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.getEntitiesOfClass(RocketEntity.class, new AABB(centre).inflate(0.5D, 0.0D, 0.5D).expandTowards(0.0D, 2.0D, 0.0D)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        final ItemStack stack = context.getItemInHand();
        final RocketEntity rocket = this.factory.create(level, centre.getX() + 0.5D, centre.getY() + 0.4D + this.padOffset, centre.getZ() + 0.5D,
                storage(stack), fuel(stack));
        if (context.getPlayer() != null) {
            rocket.setYRot(context.getPlayer().getYRot());
        }
        level.addFreshEntity(rocket);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        final int storage = storage(stack);
        if (storage > 0) {
            tooltip.add(Component.translatable("gui.galacticraftcore.rocket_type." + (storage - 1)));
        }
        final int fuel = fuel(stack);
        if (fuel >= this.fuelCapacity && storage == 0) {
            tooltip.add(Component.translatable("gui.galacticraftcore.rocket_type.3"));
            tooltip.add(Component.translatable("gui.galacticraftcore.creative_only.desc").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        } else if (stack.has(GCDataComponents.ROCKET_FUEL.get())) {
            tooltip.add(Component.translatable("gui.galacticraftcore.message.fuel").append(": " + fuel + " / " + this.fuelCapacity * RocketEntity.fuelFactor()));
        }
    }
}
