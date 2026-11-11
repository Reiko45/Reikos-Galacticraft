package io.github.reiko45.galacticraft.item.gear;

import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Infinite Oxygen Supply (1.12.2 ItemCanisterOxygenInfinite, creative only): works as an oxygen tank that never runs
 * out, and as an endless oxygen source in a machine's oxygen slot.
 */
public class InfiniteOxygenTankItem extends GearItem {
    public InfiniteOxygenTankItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC), GearSlot.LEFT_TANK, GearSlot.RIGHT_TANK);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.galacticraftcore.infinite_item").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Drains any amount of oxygen and accepts nothing. */
    public static final class Handler implements IFluidHandlerItem {
        private final ItemStack container;

        public Handler(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return this.container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return new FluidStack(GCFluids.OXYGEN.get(), Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int index) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return stack.is(GCFluids.OXYGEN.get());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return isFluidValid(0, resource) ? resource.copy() : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return maxDrain > 0 ? new FluidStack(GCFluids.OXYGEN.get(), maxDrain) : FluidStack.EMPTY;
        }
    }
}
