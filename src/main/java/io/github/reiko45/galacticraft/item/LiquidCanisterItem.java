package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Oil / Fuel Canister (1.12.2 ItemOilCanister, ItemFuelCanister): 1,000 mB. An empty canister is an (empty) oil
 * canister; filling it with fuel turns it into a fuel canister, and a drained fuel canister turns back, as in 1.12.2.
 * Filled and emptied by machines, tanks and other fluid handlers (no world pickup, like 1.12.2).
 */
public class LiquidCanisterItem extends Item {
    public static final int CAPACITY = 1000;
    private final boolean fuel;

    public LiquidCanisterItem(Properties properties, boolean fuel) {
        super(properties.stacksTo(1));
        this.fuel = fuel;
    }

    public static FluidStack contents(ItemStack stack) {
        return stack.getOrDefault(GCDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy();
    }

    public static ItemStack filled(Item item, FluidStack fluid) {
        final ItemStack stack = new ItemStack(item);
        if (!fluid.isEmpty()) {
            stack.set(GCDataComponents.FLUID.get(), SimpleFluidContent.copyOf(fluid));
        }
        return stack;
    }

    /** Fill stage for the 7 textures: 1.12.2 used 6 - 6 * damage / 1001 with damage = 1001 - mB. */
    public static int fillStage(ItemStack stack) {
        final int amount = contents(stack).getAmount();
        return amount <= 0 ? 0 : 6 - 6 * (1001 - amount) / 1001;
    }

    @Override
    public Component getName(ItemStack stack) {
        final int amount = contents(stack).getAmount();
        final String base = this.fuel ? "item.galacticraftcore.fuel_canister" : "item.galacticraftcore.oil_canister";
        if (amount <= 0) {
            return Component.translatable("item.galacticraftcore.empty_liquid_canister");
        }
        return Component.translatable(amount >= CAPACITY ? base : base + "_partial");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        final int amount = contents(stack).getAmount();
        if (amount > 0) {
            tooltip.add(Component.translatable(this.fuel ? "gui.galacticraftcore.fuel" : "gui.galacticraftcore.oil")
                    .append(": " + amount).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Fluid handler that also swaps the item between oil and fuel canister. */
    public static final class Handler implements IFluidHandlerItem {
        private ItemStack container;

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
        public FluidStack getFluidInTank(int tank) {
            return contents(this.container);
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(GCFluidTags.OIL) || stack.is(GCFluidTags.FUEL);
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource) || this.container.getCount() != 1) {
                return 0;
            }
            final FluidStack current = contents(this.container);
            if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, resource)) {
                return 0;
            }
            final int amount = Math.min(CAPACITY - current.getAmount(), resource.getAmount());
            if (amount > 0 && action.execute()) {
                set(resource.copyWithAmount(current.getAmount() + amount));
            }
            return amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            final FluidStack current = contents(this.container);
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            final FluidStack current = contents(this.container);
            if (current.isEmpty() || maxDrain <= 0 || this.container.getCount() != 1) {
                return FluidStack.EMPTY;
            }
            final int amount = Math.min(maxDrain, current.getAmount());
            if (action.execute()) {
                set(current.copyWithAmount(current.getAmount() - amount));
            }
            return current.copyWithAmount(amount);
        }

        private void set(FluidStack fluid) {
            final Item target = !fluid.isEmpty() && fluid.is(GCFluidTags.FUEL) ? GCItems.FUEL_CANISTER.get() : GCItems.OIL_CANISTER.get();
            this.container = filled(target, fluid);
        }
    }

    /** Creative tab / test helper: a full canister of oil or fuel. */
    public static ItemStack full(boolean fuel) {
        return filled(fuel ? GCItems.FUEL_CANISTER.get() : GCItems.OIL_CANISTER.get(),
                new FluidStack(fuel ? GCFluids.FUEL.get() : GCFluids.OIL.get(), CAPACITY));
    }
}
