package io.github.reiko45.galacticraft.registry;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components (1.12.2 stored these in item NBT). */
public final class GCDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GalacticraftCore.MOD_ID);

    /** gJ stored in an electric item (battery). */
    public static final Supplier<DataComponentType<Float>> ENERGY = COMPONENTS.registerComponentType("energy",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    /** A hot meteor chunk's burn time left (1.12.2 "MeteorBurnTimeF"; it counts down half a tick a tick). */
    public static final Supplier<DataComponentType<Float>> METEOR_BURN_TIME = COMPONENTS.registerComponentType("meteor_burn_time",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** Liquid in a canister (1.12.2 stored the fill level as item damage). */
    public static final Supplier<DataComponentType<SimpleFluidContent>> FLUID = COMPONENTS.registerComponentType("fluid",
            builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** Oxygen in an oxygen tank, in mB (1.12.2 stored it as maxDamage - damage). */
    public static final Supplier<DataComponentType<Integer>> OXYGEN = COMPONENTS.registerComponentType("oxygen",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Chests built into a rocket, 0-3 (1.12.2 EnumRocketType as item damage). */
    public static final Supplier<DataComponentType<Integer>> ROCKET_STORAGE = COMPONENTS.registerComponentType("rocket_storage",
            builder -> builder.persistent(Codec.intRange(0, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Fuel left in a rocket item, in mB (1.12.2 "RocketFuel" tag). */
    public static final Supplier<DataComponentType<Integer>> ROCKET_FUEL = COMPONENTS.registerComponentType("rocket_fuel",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private GCDataComponents() {
    }
}
