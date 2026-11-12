package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.fluid.GasFluid;
import io.github.reiko45.galacticraft.fluid.GCLiquidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
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

    /** 1.12.2 MarsModule "bacterialsludge": density 800, viscosity 1500, glowing (light 15), 3 quanta per block. */
    public static final DeferredHolder<FluidType, FluidType> BACTERIAL_SLUDGE_TYPE = FLUID_TYPES.register("bacterial_sludge", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.galacticraftcore.bacterial_sludge").density(800).viscosity(1500).lightLevel(15)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    /** 1.12.2 VenusModule "sulphuricacid": density 6229, viscosity 1400, a faint glow (light 1), 5 quanta per block. */
    public static final DeferredHolder<FluidType, FluidType> SULPHURIC_ACID_TYPE = FLUID_TYPES.register("sulphuric_acid", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.galacticraftcore.sulphuric_acid").density(6229).viscosity(1400).lightLevel(1)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    /**
     * 1.12.2 fluidOxygenGas: a gas with no block or bucket. Machines, tanks and pipes carry it in mB. NeoForge marks
     * gases by a density of 0 or less (1.12.2 had a separate gaseous flag and density 1).
     */
    public static final DeferredHolder<FluidType, FluidType> OXYGEN_TYPE = FLUID_TYPES.register("oxygen", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid.galacticraftcore.oxygen").density(0).viscosity(1)
                    .canSwim(false).canDrown(false).canPushEntity(false).canExtinguish(false)));

    // 1.12.2 GCFluids#registerFluids (hydrogen) and AsteroidsModule#registerFluid (the rest): gases and liquids that
    // only exist in tanks, pipes, machines and canisters, never as blocks or in buckets. Liquid methane is left out: the
    // Gas Liquefier turns methane into rocket fuel, and nothing else made or used liquid methane.
    public static final DeferredHolder<FluidType, FluidType> HYDROGEN_TYPE = tankOnly("hydrogen", 0, 1, 295);
    public static final DeferredHolder<FluidType, FluidType> METHANE_TYPE = tankOnly("methane", 0, 11, 295);
    public static final DeferredHolder<FluidType, FluidType> ATMOSPHERIC_GASES_TYPE = tankOnly("atmospheric_gases", 0, 13, 295);
    public static final DeferredHolder<FluidType, FluidType> CARBON_DIOXIDE_TYPE = tankOnly("carbon_dioxide", 0, 20, 295);
    public static final DeferredHolder<FluidType, FluidType> NITROGEN_TYPE = tankOnly("nitrogen", 0, 12, 295);
    public static final DeferredHolder<FluidType, FluidType> ARGON_TYPE = tankOnly("argon", 0, 4, 295);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_OXYGEN_TYPE = tankOnly("liquid_oxygen", 1141, 140, 90);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_NITROGEN_TYPE = tankOnly("liquid_nitrogen", 808, 130, 90);
    public static final DeferredHolder<FluidType, FluidType> LIQUID_ARGON_TYPE = tankOnly("liquid_argon", 900, 100, 87);

    private static DeferredHolder<FluidType, FluidType> tankOnly(String name, int density, int viscosity, int temperature) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid.galacticraftcore." + name)
                .density(density).viscosity(viscosity).temperature(temperature).canSwim(false).canDrown(false).canPushEntity(false)
                .canExtinguish(false)));
    }

    private static final BaseFlowingFluid.Properties OIL_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.OIL_TYPE, () -> GCFluids.OIL.get(), () -> GCFluids.FLOWING_OIL.get())
            .bucket(() -> GCFluids.OIL_BUCKET.get()).block(() -> GCFluids.OIL_BLOCK.get())
            .levelDecreasePerBlock(3).slopeFindDistance(2).tickRate(7).explosionResistance(100.0F);
    private static final BaseFlowingFluid.Properties FUEL_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.FUEL_TYPE, () -> GCFluids.FUEL.get(), () -> GCFluids.FLOWING_FUEL.get())
            .bucket(() -> GCFluids.FUEL_BUCKET.get()).block(() -> GCFluids.FUEL_BLOCK.get())
            .levelDecreasePerBlock(3).slopeFindDistance(2).tickRate(4).explosionResistance(100.0F);

    private static final BaseFlowingFluid.Properties BACTERIAL_SLUDGE_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.BACTERIAL_SLUDGE_TYPE, () -> GCFluids.BACTERIAL_SLUDGE.get(), () -> GCFluids.FLOWING_BACTERIAL_SLUDGE.get())
            .bucket(() -> GCFluids.BACTERIAL_SLUDGE_BUCKET.get()).block(() -> GCFluids.BACTERIAL_SLUDGE_BLOCK.get())
            .levelDecreasePerBlock(3).slopeFindDistance(2).tickRate(7).explosionResistance(100.0F);

    private static final BaseFlowingFluid.Properties SULPHURIC_ACID_PROPERTIES = new BaseFlowingFluid.Properties(
            GCFluids.SULPHURIC_ACID_TYPE, () -> GCFluids.SULPHURIC_ACID.get(), () -> GCFluids.FLOWING_SULPHURIC_ACID.get())
            .bucket(() -> GCFluids.SULPHURIC_ACID_BUCKET.get()).block(() -> GCFluids.SULPHURIC_ACID_BLOCK.get())
            .levelDecreasePerBlock(2).slopeFindDistance(2).tickRate(7).explosionResistance(100.0F);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SULPHURIC_ACID = FLUIDS.register("sulphuric_acid",
            () -> new BaseFlowingFluid.Source(SULPHURIC_ACID_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SULPHURIC_ACID = FLUIDS.register("flowing_sulphuric_acid",
            () -> new BaseFlowingFluid.Flowing(SULPHURIC_ACID_PROPERTIES));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL = FLUIDS.register("oil", () -> new BaseFlowingFluid.Source(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_OIL = FLUIDS.register("flowing_oil", () -> new BaseFlowingFluid.Flowing(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL = FLUIDS.register("fuel", () -> new BaseFlowingFluid.Source(FUEL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FUEL = FLUIDS.register("flowing_fuel", () -> new BaseFlowingFluid.Flowing(FUEL_PROPERTIES));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BACTERIAL_SLUDGE = FLUIDS.register("bacterial_sludge",
            () -> new BaseFlowingFluid.Source(BACTERIAL_SLUDGE_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_BACTERIAL_SLUDGE = FLUIDS.register("flowing_bacterial_sludge",
            () -> new BaseFlowingFluid.Flowing(BACTERIAL_SLUDGE_PROPERTIES));

    public static final DeferredHolder<Fluid, GasFluid> OXYGEN = FLUIDS.register("oxygen", () -> new GasFluid(GCFluids.OXYGEN_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> HYDROGEN = FLUIDS.register("hydrogen", () -> new GasFluid(GCFluids.HYDROGEN_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> METHANE = FLUIDS.register("methane", () -> new GasFluid(GCFluids.METHANE_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> ATMOSPHERIC_GASES = FLUIDS.register("atmospheric_gases",
            () -> new GasFluid(GCFluids.ATMOSPHERIC_GASES_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> CARBON_DIOXIDE = FLUIDS.register("carbon_dioxide", () -> new GasFluid(GCFluids.CARBON_DIOXIDE_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> NITROGEN = FLUIDS.register("nitrogen", () -> new GasFluid(GCFluids.NITROGEN_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> ARGON = FLUIDS.register("argon", () -> new GasFluid(GCFluids.ARGON_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> LIQUID_OXYGEN = FLUIDS.register("liquid_oxygen", () -> new GasFluid(GCFluids.LIQUID_OXYGEN_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> LIQUID_NITROGEN = FLUIDS.register("liquid_nitrogen", () -> new GasFluid(GCFluids.LIQUID_NITROGEN_TYPE));
    public static final DeferredHolder<Fluid, GasFluid> LIQUID_ARGON = FLUIDS.register("liquid_argon", () -> new GasFluid(GCFluids.LIQUID_ARGON_TYPE));

    public static final DeferredBlock<GCLiquidBlock> OIL_BLOCK = GCBlocks.BLOCKS.register("oil",
            () -> new GCLiquidBlock(GCFluids.OIL.get(), liquid(MapColor.COLOR_BROWN), false));
    public static final DeferredBlock<GCLiquidBlock> FUEL_BLOCK = GCBlocks.BLOCKS.register("fuel",
            () -> new GCLiquidBlock(GCFluids.FUEL.get(), liquid(MapColor.COLOR_YELLOW), true));

    /** Mars caverns pool it on their floors. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.fluid.BacterialSludgeBlock> BACTERIAL_SLUDGE_BLOCK = GCBlocks.BLOCKS.register("bacterial_sludge",
            () -> new io.github.reiko45.galacticraft.fluid.BacterialSludgeBlock(GCFluids.BACTERIAL_SLUDGE.get(), liquid(MapColor.PLANT).lightLevel(state -> 15)));

    /** Venus valleys and vapor pools hold it; it eats at whatever is in it. */
    public static final DeferredBlock<io.github.reiko45.galacticraft.fluid.SulphuricAcidBlock> SULPHURIC_ACID_BLOCK = GCBlocks.BLOCKS.register("sulphuric_acid",
            () -> new io.github.reiko45.galacticraft.fluid.SulphuricAcidBlock(GCFluids.SULPHURIC_ACID.get(), liquid(MapColor.EMERALD).lightLevel(state -> 1)));
    public static final DeferredItem<BucketItem> SULPHURIC_ACID_BUCKET = GCItems.ITEMS.register("sulphuric_acid_bucket",
            () -> new BucketItem(GCFluids.SULPHURIC_ACID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> OIL_BUCKET = GCItems.ITEMS.register("oil_bucket",
            () -> new BucketItem(GCFluids.OIL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<BucketItem> FUEL_BUCKET = GCItems.ITEMS.register("fuel_bucket",
            () -> new BucketItem(GCFluids.FUEL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BucketItem> BACTERIAL_SLUDGE_BUCKET = GCItems.ITEMS.register("bacterial_sludge_bucket",
            () -> new BucketItem(GCFluids.BACTERIAL_SLUDGE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private GCFluids() {
    }

    private static BlockBehaviour.Properties liquid(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollission().strength(100.0F)
                .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY);
    }
}
