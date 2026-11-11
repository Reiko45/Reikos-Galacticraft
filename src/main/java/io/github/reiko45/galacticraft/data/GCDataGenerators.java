package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * {@code ./gradlew runData} entry point; output goes to {@code src/generated/resources}.
 * Textures, translations and crafting recipes ported from 1.12.2 are produced by
 * {@code tools/port_legacy_assets.py} instead and live in {@code src/main/resources}.
 */
@EventBusSubscriber(modid = GalacticraftCore.MOD_ID)
public final class GCDataGenerators {
    private GCDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        final DataGenerator generator = event.getGenerator();
        final PackOutput output = generator.getPackOutput();
        final ExistingFileHelper existingFiles = event.getExistingFileHelper();
        final CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new GCBlockStateProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new GCItemModelProvider(output, existingFiles));

        final GCWorldgenProvider worldgen = generator.addProvider(event.includeServer(), new GCWorldgenProvider(output, lookup));
        final CompletableFuture<HolderLookup.Provider> fullLookup = worldgen.getRegistryProvider();
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(GCBlockLootProvider::new, LootContextParamSets.BLOCK)), fullLookup));
        generator.addProvider(event.includeServer(), new GCRecipeProvider(output, fullLookup));
        final GCBlockTagsProvider blockTags = generator.addProvider(event.includeServer(),
                new GCBlockTagsProvider(output, fullLookup, existingFiles));
        generator.addProvider(event.includeServer(), new GCItemTagsProvider(output, fullLookup, blockTags.contentsGetter(), existingFiles));
    }
}
