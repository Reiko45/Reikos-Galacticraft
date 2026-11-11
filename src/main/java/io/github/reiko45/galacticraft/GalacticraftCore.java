package io.github.reiko45.galacticraft;

import io.github.reiko45.galacticraft.registry.GCArmorMaterials;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCChunkGenerators;
import io.github.reiko45.galacticraft.registry.GCCreativeTabs;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the NeoForge 1.21.1 port of Galacticraft Legacy (1.12.2).
 *
 * <p>The 1.12.2 mod initialised everything from FML lifecycle events through a proxy. On NeoForge,
 * content goes into {@code DeferredRegister}s attached to the mod event bus here, game events are
 * handled by {@code @EventBusSubscriber} classes, and client-only setup lives in {@code GalacticraftClient}.
 */
@Mod(GalacticraftCore.MOD_ID)
public final class GalacticraftCore {
    public static final String MOD_ID = "galacticraftcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("Galacticraft");

    public GalacticraftCore(IEventBus modBus, ModContainer container) {
        // GCBlocks first: it also registers the block items into GCItems.ITEMS.
        GCBlocks.BLOCKS.register(modBus);
        GCItems.ITEMS.register(modBus);
        GCArmorMaterials.ARMOR_MATERIALS.register(modBus);
        GCCreativeTabs.TABS.register(modBus);
        GCChunkGenerators.CHUNK_GENERATORS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
