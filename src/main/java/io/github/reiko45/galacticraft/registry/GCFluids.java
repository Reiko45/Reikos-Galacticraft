package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.fluid.GasFluid;
import io.github.reiko45.galacticraft.fluid.GCLiquidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Oil and fuel (1.12.2 GCFluids + BlockFluidGC). Density/viscosity are the 1.12.2 values; 1.12.2 used 3 quanta per
 * block, so the liquids only spread 2 blocks (level decrease 3), and Forge's tick rate of viscosity/200.
 */
public final class GCFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, GalacticraftCore.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, GalacticraftCore.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = FLUID_TYPES.register("oil", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.galacticraftcore.oil").density(800).viscosity(1500)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<FluidType, FluidType> FUEL_TYPE = FLUID_TYPES.register("fuel", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.galacticraftcore.fuel").density(400).viscosity(900)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    /**
     * 1.12.2 fluidOxygenGas: a gas with no block or bucket. Machines, tanks and pipes carry it in mB. NeoForge marks
     * gases by a density of 0 or less (1.12.2 had a separate gaseous flag and density 1).
     */
    public static final DeferredHolder<FluidType, FluidType> OXYGEN_TYPE = FLUID_TYPES.register("oxygen", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid.galacticraftcore.oxygen").density(0).viscosity(1)
                    .canSwim(false).canDrown(false).canPushEntity(false).canExtinguish(false)));

    private static final BaseFlowingFluid.Properties OIL_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.OIL_TYPE, () -> GCFluids.OIL.get(), () -> GCFluids.FLOWING_OIL.get())
            .bucket(() -> GCFluids.OIL_BUCKET.get()).block(() -> GCFluids.OIL_BLOCK.get())
            .levelDecreasePerBlock(3).slopeFindDistance(2).tickRate(7).explosionResistance(100.0F);
    private static final BaseFlowingFluid.Properties FUEL_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.FUEL_TYPE, () -> GCFluids.FUEL.get(), () -> GCFluids.FLOWING_FUEL.get())
            .bucket(() -> GCFluids.FUEL_BUCKET.get()).block(() -> GCFluids.FUEL_BLOCK.get())
            .levelDecreasePerBlock(3).slopeFindDistance(2).tickRate(4).explosionResistance(100.0F);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL = FLUIDS.register("oil", () -> new BaseFlowingFluid.Source(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_OIL = FLUIDS.register("flowing_oil", () -> new BaseFlowingFluid.Flowing(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL = FLUIDS.register("fuel", () -> new BaseFlowingFluid.Source(FUEL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FUEL = FLUIDS.register("flowing_fuel", () -> new BaseFlowingFluid.Flowing(FUEL_PROPERTIES));

    public static final DeferredHolder<Fluid, GasFluid> OXYGEN = FLUIDS.register("oxygen", () -> new GasFluid(GCFluids.OXYGEN_TYPE));

    public static final DeferredBlock<GCLiquidBlock> OIL_BLOCK = GCBlocks.BLOCKS.register("oil",
            () -> new GCLiquidBlock(GCFluids.OIL.get(), liquid(MapColor.COLOR_BROWN), false));
    public static final DeferredBlock<GCLiquidBlock> FUEL_BLOCK = GCBlocks.BLOCKS.register("fuel",
            () -> new GCLiquidBlock(GCFluids.FUEL.get(), liquid(MapColor.COLOR_YELLOW), true));

    public static final DeferredItem<BucketItem> OIL_BUCKET = GCItems.ITEMS.register("oil_bucket",
            () -> new BucketItem(GCFluids.OIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<BucketItem> FUEL_BUCKET = GCItems.ITEMS.register("fuel_bucket",
            () -> new BucketItem(GCFluids.FUEL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private GCFluids() {
    }

    private static BlockBehaviour.Properties liquid(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollission().strength(100.0F)
                .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY);
    }
}
