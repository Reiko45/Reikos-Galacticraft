/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityGasLiquefier). Copyright
 * (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.block.entity;

import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.machine.RelativeSide;
import io.github.reiko45.galacticraft.menu.GasMachineMenu;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.world.Atmosphere;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * Gas Liquefier (1.12.2 TileEntityGasLiquefier), 60 gJ/t: methane becomes rocket fuel, oxygen liquid oxygen, nitrogen
 * liquid nitrogen and argon liquid argon, 2 mB of gas to 1 mB of liquid, up to 3 mB of liquid an operation. With an
 * Atmospheric Valve in its input slot it draws 4 mB of the world's air a tick from the open air above and liquefies
 * what the air is made of (on Mars argon and nitrogen; in the Overworld nitrogen and oxygen). It also takes gases from
 * pipes on its left face, methane / liquid oxygen / liquid nitrogen canisters (each mB of a liquid counts as 2 of gas)
 * and other containers of those gases. The liquid leaves through the right face, 1 mB a tick, or fills a canister.
 * Tanks: gas 4,000, liquid 2,000 mB; the gas tank holds one gas and the liquid tank one liquid at a time.
 */
public class GasLiquefierBlockEntity extends GasMachineBlockEntity {
    /** Liquefying, as last sent to clients. */
    private boolean misting;
    public static final int GAS_CAPACITY = 4000;
    public static final int LIQUID_CAPACITY = 2000;
    /** 1.12.2 OUTPUT_PER_SECOND (pushed every tick). */
    public static final int LIQUID_OUTPUT_PER_TICK = 1;

    /** 1.12.2 TankGases: a gas and what it liquefies into; AIR is the world's mix. */
    public enum Gas {
        METHANE(() -> GCFluids.METHANE.get(), () -> GCFluids.FUEL.get()),
        OXYGEN(() -> GCFluids.OXYGEN.get(), () -> GCFluids.LIQUID_OXYGEN.get()),
        NITROGEN(() -> GCFluids.NITROGEN.get(), () -> GCFluids.LIQUID_NITROGEN.get()),
        ARGON(() -> GCFluids.ARGON.get(), () -> GCFluids.LIQUID_ARGON.get()),
        AIR(() -> GCFluids.ATMOSPHERIC_GASES.get(), null);

        private final Supplier<Fluid> gas;
        @Nullable
        private final Supplier<Fluid> liquid;

        Gas(Supplier<Fluid> gas, @Nullable Supplier<Fluid> liquid) {
            this.gas = gas;
            this.liquid = liquid;
        }

        public Fluid gas() {
            return this.gas.get();
        }

        @Nullable
        public Fluid liquid() {
            return this.liquid == null ? null : this.liquid.get();
        }

        @Nullable
        public static Gas ofGas(FluidStack stack) {
            for (Gas gas : values()) {
                if (stack.is(gas.gas())) {
                    return gas;
                }
            }
            return null;
        }

        @Nullable
        public static Gas ofLiquid(FluidStack stack) {
            for (Gas gas : values()) {
                if (gas.liquid != null && (stack.is(gas.liquid()) || gas == METHANE && stack.is(GCFluidTags.FUEL))) {
                    return gas;
                }
            }
            return null;
        }
    }

    private final FluidTank gasTank = tank(GAS_CAPACITY, stack -> Gas.ofGas(stack) != null);
    private final FluidTank liquidTank = tank(LIQUID_CAPACITY, stack -> Gas.ofLiquid(stack) != null);
    private final FluidTank[] tanks = {this.gasTank, this.liquidTank};

    public GasLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntities.GAS_LIQUEFIER.get(), pos, state, 3, 60.0F);
    }

    @Override
    public FluidTank[] tanks() {
        return this.tanks;
    }

    @Override
    protected float energyPerTick() {
        return io.github.reiko45.galacticraft.GCConfig.hardMode() ? 90.0F : 60.0F;
    }

    /**
     * 1.12.2 getAirProducts: the first three gases of this world's air, as the liquefier knows them (null for one it
     * can't liquefy, such as Mars's carbon dioxide). Mars: CO2, argon, nitrogen; any non-Galacticraft world (1.12.2's
     * fixed 35): nitrogen, then oxygen.
     */
    public List<Gas> airMix() {
        final List<Gas> mix = new ArrayList<>();
        final List<Atmosphere.Gas> gases = this.level == null ? List.of() : Atmosphere.of(this.level.dimension());
        if (gases == null) {
            mix.add(Gas.NITROGEN);
            mix.add(Gas.OXYGEN);
            return mix;
        }
        for (Atmosphere.Gas gas : gases.subList(0, Math.min(3, gases.size()))) {
            mix.add(switch (gas) {
                case METHANE -> Gas.METHANE;
                case OXYGEN -> Gas.OXYGEN;
                case NITROGEN -> Gas.NITROGEN;
                case ARGON -> Gas.ARGON;
                default -> null;
            });
        }
        return mix;
    }

    /** What this world's air liquefies into. */
    public List<Gas> airProducts() {
        final List<Gas> products = new ArrayList<>();
        for (Gas gas : airMix()) {
            if (gas != null) {
                products.add(gas);
            }
        }
        return products;
    }

    @Nullable
    public Gas gasType() {
        return this.gasTank.isEmpty() ? null : Gas.ofGas(this.gasTank.getFluid());
    }

    @Nullable
    public Gas liquidType() {
        return this.liquidTank.isEmpty() ? null : Gas.ofLiquid(this.liquidTank.getFluid());
    }

    @Override
    protected void tick() {
        if (gasType() == Gas.AIR && airProducts().isEmpty()) {
            this.gasTank.setFluid(FluidStack.EMPTY);
        }
        takeInput();
        emptyIntoSlot(2, this.liquidTank);
        super.tick();
        push(this.liquidTank, RelativeSide.RIGHT, LIQUID_OUTPUT_PER_TICK);
        if (isWorking() != this.misting && this.level != null) {
            this.misting = isWorking();
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Whether it is liquefying (1.12.2 processTicks > 0), as the client knows it, for the mist. */
    public boolean misting() {
        return this.misting;
    }

    /**
     * 1.12.2 BlockMachineMarsT2#randomDisplayTick: while it works, cold mist spills over the top edge of all four sides
     * and sinks.
     */
    public void animateTick(net.minecraft.util.RandomSource random) {
        if (!this.misting || this.level == null) {
            return;
        }
        final float x = this.worldPosition.getX() + 0.5F;
        final float y = this.worldPosition.getY() + 0.8F + 0.05F * random.nextInt(3);
        final float z = this.worldPosition.getZ() + 0.5F;
        final net.minecraft.core.particles.SimpleParticleType mist = io.github.reiko45.galacticraft.registry.GCParticles.SMALL_SMOKE.get();
        for (float i = -0.41F + 0.16F * random.nextFloat(); i < 0.5F; i += 0.167F) {
            if (random.nextInt(3) == 0) {
                this.level.addParticle(mist, x + i, y, z - 0.41F, 0.0D, -0.015D, -0.0015D);
            }
            if (random.nextInt(3) == 0) {
                this.level.addParticle(mist, x + i, y, z + 0.537F, 0.0D, -0.015D, 0.0015D);
            }
            if (random.nextInt(3) == 0) {
                this.level.addParticle(mist, x - 0.41F, y, z + i, -0.0015D, -0.015D, 0.0D);
            }
            if (random.nextInt(3) == 0) {
                this.level.addParticle(mist, x + 0.537F, y, z + i, 0.0015D, -0.015D, 0.0D);
            }
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("Misting", this.misting);
        return tag;
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        this.misting = tag.getBoolean("Misting");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
                             net.minecraft.core.HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    /** 1.12.2 update: the input slot's valve (the world's air) or container. */
    private void takeInput() {
        final ItemStack stack = this.inventory.getStackInSlot(1);
        if (stack.isEmpty()) {
            return;
        }
        if (isValve(stack)) {
            if (!airProducts().isEmpty() && (this.gasTank.isEmpty() || gasType() == Gas.AIR && this.gasTank.getSpace() > 0) && openAirAbove()) {
                this.gasTank.fill(new FluidStack(GCFluids.ATMOSPHERIC_GASES.get(), VALVE_VENT), IFluidHandler.FluidAction.EXECUTE);
            }
            return;
        }
        final IFluidHandlerItem handler = stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null || stack.getCount() != 1) {
            return;
        }
        final FluidStack contained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (contained.isEmpty()) {
            return;
        }
        // Liquid oxygen and nitrogen count double as gas (1.12.2: amount * 2 for "liquid" fluids and LOX / LN2 canisters)
        final Gas gas;
        final int factor;
        if (contained.is(GCFluids.METHANE.get())) {
            gas = Gas.METHANE;
            factor = 1;
        } else if (contained.is(GCFluids.OXYGEN.get()) || contained.is(GCFluids.LIQUID_OXYGEN.get())) {
            gas = Gas.OXYGEN;
            factor = contained.is(GCFluids.LIQUID_OXYGEN.get()) ? 2 : 1;
        } else if (contained.is(GCFluids.NITROGEN.get()) || contained.is(GCFluids.LIQUID_NITROGEN.get())) {
            gas = Gas.NITROGEN;
            factor = contained.is(GCFluids.LIQUID_NITROGEN.get()) ? 2 : 1;
        } else {
            return;
        }
        if (!this.gasTank.isEmpty() && gasType() != gas) {
            return;
        }
        // 1.12.2 took canisters a bit at a time and other containers only whole
        final boolean canister = stack.getItem() instanceof LiquidCanisterItem;
        final int room = this.gasTank.getSpace() / factor;
        final int take = canister ? Math.min(room, contained.getAmount()) : contained.getAmount() <= room ? contained.getAmount() : 0;
        if (take <= 0) {
            return;
        }
        final FluidStack drained = handler.drain(contained.copyWithAmount(take), IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            this.gasTank.fill(new FluidStack(gas.gas(), drained.getAmount() * factor), IFluidHandler.FluidAction.EXECUTE);
            this.inventory.setStackInSlot(1, handler.getContainer());
        }
    }

    /** 1.12.2 canProcess: gas in, and room for its liquid (any of the air's liquids for air). */
    @Override
    protected boolean canProcess() {
        final Gas gas = gasType();
        if (this.disabled || gas == null) {
            return false;
        }
        final Gas liquid = liquidType();
        if (liquid == null) {
            return gas != Gas.AIR || !airProducts().isEmpty();
        }
        if (this.liquidTank.getSpace() <= 0) {
            return false;
        }
        return gas == Gas.AIR ? airProducts().contains(liquid) : gas == liquid;
    }

    /** 1.12.2 doLiquefaction: up to 3 mB of liquid from twice that of gas; air shares it between its products. */
    @Override
    protected void process() {
        final Gas gas = gasType();
        final int amount = this.gasTank.getFluidAmount();
        if (gas == null || amount == 0) {
            return;
        }
        if (gas == Gas.AIR) {
            // 1.12.2: 2 at most if the air has a product after its first gas, 3 otherwise; halved for each next gas
            final List<Gas> mix = airMix();
            boolean later = false;
            for (int i = 1; i < mix.size(); i++) {
                later |= mix.get(i) != null;
            }
            int toLiquefy = Math.max(1, Math.min(amount / 2, later ? 2 : 3));
            for (Gas product : mix) {
                if (product != null) {
                    this.gasTank.drain(liquefy(product, toLiquefy) * 2, IFluidHandler.FluidAction.EXECUTE);
                }
                toLiquefy = Math.max(1, toLiquefy >> 1);
            }
        } else if (amount == 1) {
            this.gasTank.drain(liquefy(gas, 1), IFluidHandler.FluidAction.EXECUTE);
        } else {
            this.gasTank.drain(liquefy(gas, Math.min(amount / 2, 3)) * 2, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 1.12.2 placeIntoFluidTanks: liquid of {@code product} if the liquid tank holds that (or nothing). */
    private int liquefy(Gas product, int amount) {
        final Gas liquid = liquidType();
        if (product.liquid() == null || liquid != null && liquid != product || this.liquidTank.getSpace() <= 0) {
            return 0;
        }
        return this.liquidTank.fill(new FluidStack(product.liquid(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    /** 1.12.2 GuiGasLiquefier status. */
    @Override
    public MachineStatus status() {
        final MachineStatus power = powerStatus();
        if (power != null) {
            return power;
        }
        if (isWorking() || canProcess()) {
            return MachineStatus.LIQUEFYING;
        }
        if (this.gasTank.isEmpty()) {
            return MachineStatus.NO_GAS;
        }
        if (this.disabled) {
            return MachineStatus.READY;
        }
        return this.liquidTank.getSpace() == 0 ? MachineStatus.TANKS_FULL : MachineStatus.UNKNOWN;
    }

    public static boolean isGasInput(ItemStack stack) {
        return contains(stack, fluid -> fluid.is(GCFluids.METHANE.get()) || fluid.is(GCFluids.OXYGEN.get()) || fluid.is(GCFluids.LIQUID_OXYGEN.get())
                || fluid.is(GCFluids.NITROGEN.get()) || fluid.is(GCFluids.LIQUID_NITROGEN.get()));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem;
            case 1 -> isValve(stack) || isGasInput(stack) || LiquidCanisterItem.isEmpty(stack);
            case 2 -> isValve(stack) || canFill(stack, new FluidStack(GCFluids.FUEL.get(), 1))
                    || canFill(stack, new FluidStack(GCFluids.LIQUID_OXYGEN.get(), 1)) || canFill(stack, new FluidStack(GCFluids.LIQUID_NITROGEN.get(), 1));
            default -> false;
        };
    }

    @Override
    protected int[] automationSlots(RelativeSide side) {
        return new int[]{0, 1, 2};
    }

    @Override
    protected boolean canAutomationInsert(int slot, ItemStack stack, RelativeSide side) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem electric && electric.stored(stack) > 0.0F;
            case 1 -> isGasInput(stack);
            case 2 -> !this.liquidTank.isEmpty() && canFill(stack, this.liquidTank.getFluid());
            default -> false;
        };
    }

    /** 1.12.2: take out empty batteries, emptied gas containers and filled liquid containers. */
    @Override
    protected boolean canAutomationExtract(int slot, RelativeSide side) {
        final ItemStack stack = this.inventory.getStackInSlot(slot);
        return switch (slot) {
            case 0 -> stack.getItem() instanceof ElectricItem electric && electric.stored(stack) <= 0.0F;
            case 1 -> !isValve(stack) && !isGasInput(stack);
            case 2 -> !isValve(stack) && (this.liquidTank.isEmpty() || !canFill(stack, this.liquidTank.getFluid()));
            default -> false;
        };
    }

    /** Gases in through the left face (one kind at a time); the liquid out through the right. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        final RelativeSide relative = side == null ? null : RelativeSide.of(facing(), side);
        if (relative == RelativeSide.LEFT) {
            return new DirectionalTank(this.gasTank, true, false);
        }
        if (relative == RelativeSide.RIGHT) {
            return new DirectionalTank(this.liquidTank, false, true);
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new GasMachineMenu(GasMachineMenu.Kind.GAS_LIQUEFIER, id, playerInventory, this);
    }
}
