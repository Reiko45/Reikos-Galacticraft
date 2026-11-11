package io.github.reiko45.galacticraft.registry;

import com.mojang.serialization.Codec;
import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components (1.12.2 stored these in item NBT). */
public final class GCDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GalacticraftCore.MOD_ID);

    /** gJ stored in an electric item (battery). */
    public static final Supplier<DataComponentType<Float>> ENERGY = COMPONENTS.registerComponentType("energy",
            builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    private GCDataComponents() {
    }
}
