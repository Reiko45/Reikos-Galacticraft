package io.github.reiko45.galacticraft.item;

import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.List;
import java.util.function.Predicate;
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
 * Liquid canisters, 1,000 mB each: Oil and Fuel (1.12.2 ItemOilCanister, ItemFuelCanister), and the asteroids module's
 * Methane Gas, Liquid Oxygen and Liquid Nitrogen canisters (ItemCanisterMethane, ItemCanisterLiquidOxygen,
 * ItemCanisterLiquidNitrogen) that the Mars gas machines fill. An empty canister is an (empty) oil canister; filling
 * it turns it into the canister for what went in, and a drained canister turns back, as in 1.12.2. Filled and emptied
 * by machines, tanks and other fluid handlers (no world pickup, like 1.12.2).
 */
public class LiquidCanisterItem extends Item {
    public static final int CAPACITY = 1000;

    /** What a canister holds: its fluid, and its names (full, partial) and tooltip prefix. */
    public enum Kind {
        OIL(stack -> stack.is(GCFluidTags.OIL), "item.galacticraftcore.oil_canister", "gui.galacticraftcore.oil"),
        FUEL(stack -> stack.is(GCFluidTags.FUEL), "item.galacticraftcore.fuel_canister", "gui.galacticraftcore.fuel"),
        METHANE(stack -> stack.is(GCFluids.METHANE.get()), "item.galacticraftcore.methane_canister", "item.galacticraftcore.methane_canister.contents"),
        LIQUID_OXYGEN(stack -> stack.is(GCFluids.LIQUID_OXYGEN.get()), "item.galacticraftcore.liquid_oxygen_canister",
                "item.galacticraftcore.liquid_oxygen_canister.contents"),
        LIQUID_NITROGEN(stack -> stack.is(GCFluids.LIQUID_NITROGEN.get()), "item.galacticraftcore.liquid_nitrogen_canister",
                "item.galacticraftcore.liquid_nitrogen_canister.contents");

        private final Predicate<FluidStack> fluid;
        private final String name;
        private final String contents;

        Kind(Predicate<FluidStack> fluid, String name, String contents) {
            this.fluid = fluid;
            this.name = name;
            this.contents = contents;
        }

        public boolean holds(FluidStack stack) {
            return this.fluid.test(stack);
        }

        public Item item() {
            return switch (this) {
                case OIL -> GCItems.OIL_CANISTER.get();
                case FUEL -> GCItems.FUEL_CANISTER.get();
                case METHANE -> GCItems.METHANE_CANISTER.get();
                case LIQUID_OXYGEN -> GCItems.LIQUID_OXYGEN_CANISTER.get();
                case LIQUID_NITROGEN -> GCItems.LIQUID_NITROGEN_CANISTER.get();
            };
        }

        /** The canister for a fluid, or null if no canister holds it. */
        static Kind of(FluidStack stack) {
            for (Kind kind : values()) {
                if (kind.holds(stack)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private final Kind kind;

    public LiquidCanisterItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    /** Every canister item, for registering their fluid handler and fill-stage textures. */
    public static Item[] items() {
        final Kind[] kinds = Kind.values();
        final Item[] items = new Item[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            items[i] = kinds[i].item();
        }
        return items;
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

    /** A canister holding {@code fluid} (the right kind of canister for it). */
    public static ItemStack filled(FluidStack fluid) {
        final Kind kind = Kind.of(fluid);
        return filled(kind == null ? GCItems.OIL_CANISTER.get() : kind.item(), kind == null ? FluidStack.EMPTY : fluid);
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack.getItem() instanceof LiquidCanisterItem && contents(stack).isEmpty();
    }

    /** Fill stage for the 7 textures: 1.12.2 used 6 - 6 * damage / 1001 with damage = 1001 - mB. */
    public static int fillStage(ItemStack stack) {
        final int amount = contents(stack).getAmount();
        return amount <= 0 ? 0 : 6 - 6 * (1001 - amount) / 1001;
    }

    @Override
    public Component getName(ItemStack stack) {
        final int amount = contents(stack).getAmount();
        if (amount <= 0) {
            return Component.translatable("item.galacticraftcore.empty_liquid_canister");
        }
        return Component.translatable(amount >= CAPACITY ? this.kind.name : this.kind.name + "_partial");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        final int amount = contents(stack).getAmount();
        if (amount > 0) {
            tooltip.add(Component.translatable(this.kind.contents).append(": " + amount).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Fluid handler that also swaps the item to the canister for what it holds. */
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
            return Kind.of(stack) != null;
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
            this.container = filled(fluid);
        }
    }

    /** Creative tab / test helper: a full canister of oil or fuel. */
    public static ItemStack full(boolean fuel) {
        return full(fuel ? Kind.FUEL : Kind.OIL);
    }

    /** Creative tab / test helper: a full canister of its kind. */
    public static ItemStack full(Kind kind) {
        final FluidStack fluid = switch (kind) {
            case OIL -> new FluidStack(GCFluids.OIL.get(), CAPACITY);
            case FUEL -> new FluidStack(GCFluids.FUEL.get(), CAPACITY);
            case METHANE -> new FluidStack(GCFluids.METHANE.get(), CAPACITY);
            case LIQUID_OXYGEN -> new FluidStack(GCFluids.LIQUID_OXYGEN.get(), CAPACITY);
            case LIQUID_NITROGEN -> new FluidStack(GCFluids.LIQUID_NITROGEN.get(), CAPACITY);
        };
        return filled(kind.item(), fluid);
    }
}
