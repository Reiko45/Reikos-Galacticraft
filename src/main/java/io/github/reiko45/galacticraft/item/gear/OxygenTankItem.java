package io.github.reiko45.galacticraft.item.gear;

import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Light (900), Medium (1,800) and Heavy (2,700) Oxygen Tank (1.12.2 ItemOxygenTank, tier x 900 as max damage). A
 * player breathes 1 unit every 9 ticks. Crafted tanks are empty; fill them in an Oxygen Compressor.
 */
public class OxygenTankItem extends GearItem {
    private final int tier;

    public OxygenTankItem(Properties properties, int tier) {
        super(properties, GearSlot.LEFT_TANK, GearSlot.RIGHT_TANK);
        this.tier = tier;
    }

    public int tier() {
        return this.tier;
    }

    public int capacity() {
        return this.tier * 900;
    }

    public static int oxygen(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.OXYGEN.get(), 0);
    }

    public static void setOxygen(ItemStack stack, int oxygen) {
        if (stack.getItem() instanceof OxygenTankItem tank) {
            stack.set(GCDataComponents.OXYGEN.get(), Mth.clamp(oxygen, 0, tank.capacity()));
        }
    }

    public ItemStack full() {
        final ItemStack stack = new ItemStack(this);
        setOxygen(stack, capacity());
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return oxygen(stack) < capacity();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * oxygen(stack) / capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) oxygen(stack) / capacity()) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.galacticraftcore.oxygen_remaining").append(": " + oxygen(stack)).withStyle(ChatFormatting.GRAY));
    }

    /** Oxygen tank as a fluid container (mB of oxygen gas), so compressors, pipes and other mods can fill or empty it. */
    public static final class Handler implements IFluidHandlerItem {
        private final ItemStack container;
        private final OxygenTankItem tank;

        public Handler(ItemStack container) {
            this.container = container;
            this.tank = (OxygenTankItem) container.getItem();
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
            final int oxygen = oxygen(this.container);
            return oxygen > 0 ? new FluidStack(GCFluids.OXYGEN.get(), oxygen) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            return this.tank.capacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return stack.is(GCFluids.OXYGEN.get());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (this.container.getCount() != 1 || !isFluidValid(0, resource)) {
                return 0;
            }
            final int amount = Math.min(resource.getAmount(), this.tank.capacity() - oxygen(this.container));
            if (amount > 0 && action.execute()) {
                setOxygen(this.container, oxygen(this.container) + amount);
            }
            return amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return isFluidValid(0, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            if (this.container.getCount() != 1) {
                return FluidStack.EMPTY;
            }
            final int amount = Math.min(maxDrain, oxygen(this.container));
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                setOxygen(this.container, oxygen(this.container) - amount);
            }
            return new FluidStack(GCFluids.OXYGEN.get(), amount);
        }
    }
}
