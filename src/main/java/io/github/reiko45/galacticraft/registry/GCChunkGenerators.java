package io.github.reiko45.galacticraft.registry;

import com.mojang.serialization.MapCodec;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.MoonChunkGenerator;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GCChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, GalacticraftCore.MOD_ID);

    public static final Supplier<MapCodec<MoonChunkGenerator>> MOON = CHUNK_GENERATORS.register("moon", () -> MoonChunkGenerator.CODEC);

    public static final Supplier<MapCodec<io.github.reiko45.galacticraft.world.MarsChunkGenerator>> MARS = CHUNK_GENERATORS.register("mars",
            () -> io.github.reiko45.galacticraft.world.MarsChunkGenerator.CODEC);

    private GCChunkGenerators() {
    }
}
