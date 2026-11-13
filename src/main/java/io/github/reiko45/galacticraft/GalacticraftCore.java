package io.github.reiko45.galacticraft;

import io.github.reiko45.galacticraft.registry.GCArmorMaterials;
import io.github.reiko45.galacticraft.registry.GCAttachments;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCCapabilities;
import io.github.reiko45.galacticraft.registry.GCDataComponents;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCFeatures;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCMenus;
import io.github.reiko45.galacticraft.registry.GCRecipes;
import io.github.reiko45.galacticraft.registry.GCSounds;
import io.github.reiko45.galacticraft.registry.GCStructures;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCChunkGenerators;
import io.github.reiko45.galacticraft.registry.GCCreativeTabs;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
        // GCFluids also adds the oil/fuel liquid blocks and buckets to GCBlocks.BLOCKS and GCItems.ITEMS.
        GCFluids.FLUID_TYPES.register(modBus);
        GCFluids.FLUIDS.register(modBus);
        GCFeatures.FEATURES.register(modBus);
        GCChunkGenerators.BIOME_SOURCES.register(modBus);
        GCArmorMaterials.ARMOR_MATERIALS.register(modBus);
        GCCreativeTabs.TABS.register(modBus);
        GCChunkGenerators.CHUNK_GENERATORS.register(modBus);
        GCDataComponents.COMPONENTS.register(modBus);
        GCBlockEntities.BLOCK_ENTITIES.register(modBus);
        GCAttachments.ATTACHMENT_TYPES.register(modBus);
        GCMenus.MENUS.register(modBus);
        GCEntities.ENTITY_TYPES.register(modBus);
        GCSounds.SOUNDS.register(modBus);
        io.github.reiko45.galacticraft.registry.GCParticles.PARTICLES.register(modBus);
        GCStructures.STRUCTURE_TYPES.register(modBus);
        GCStructures.STRUCTURE_PIECES.register(modBus);
        GCRecipes.TYPES.register(modBus);
        GCRecipes.SERIALIZERS.register(modBus);
        modBus.addListener(GCCapabilities::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(io.github.reiko45.galacticraft.block.entity.ChromaticApplicatorBlockEntity::onLevelTick);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                // 1.12.2 GCFluids: dispensers throw meteor chunks
                net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(io.github.reiko45.galacticraft.registry.GCItems.METEOR_CHUNK.get())));
        modBus.addListener(io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity::registerTickets);
        modBus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) ->
                event.register(io.github.reiko45.galacticraft.entity.AstroMinerEntity.TICKETS));
        container.registerConfig(ModConfig.Type.CLIENT, GCConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, GCConfig.SERVER_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
