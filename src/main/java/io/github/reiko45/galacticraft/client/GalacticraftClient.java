package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.screen.CircuitFabricatorScreen;
import io.github.reiko45.galacticraft.client.screen.CoalGeneratorScreen;
import io.github.reiko45.galacticraft.client.screen.CompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricCompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricFurnaceScreen;
import io.github.reiko45.galacticraft.client.screen.EnergyStorageScreen;
import io.github.reiko45.galacticraft.client.screen.RefineryScreen;
import io.github.reiko45.galacticraft.client.screen.SolarPanelScreen;
import io.github.reiko45.galacticraft.client.render.FluidPipeRenderer;
import io.github.reiko45.galacticraft.client.render.FluidTankRenderer;
import io.github.reiko45.galacticraft.client.render.SolarPanelRenderer;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.registry.GCBlockEntities;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.github.reiko45.galacticraft.registry.GCMenus;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-only entry point. */
@Mod(value = GalacticraftCore.MOD_ID, dist = Dist.CLIENT)
public final class GalacticraftClient {
    public GalacticraftClient(IEventBus modBus) {
        modBus.addListener(GalacticraftClient::registerDimensionEffects);
        modBus.addListener(GalacticraftClient::registerScreens);
        modBus.addListener(GalacticraftClient::registerLayers);
        modBus.addListener(GalacticraftClient::registerRenderers);
        modBus.addListener(GalacticraftClient::registerClientExtensions);
        modBus.addListener(GalacticraftClient::registerAdditionalModels);
        modBus.addListener(GalacticraftClient::clientSetup);
        ScreenshotTour.registerIfEnabled();
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SolarPanelRenderer.LAYER, SolarPanelRenderer::createLayer);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GCBlockEntities.BASIC_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.ADVANCED_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_PIPE.get(), FluidPipeRenderer::new);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FluidPipeRenderer.PULL_MODEL);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Canister textures oil/fuel_canister_partial_0..6 are picked by the "fill" stage.
            for (Item canister : new Item[]{GCItems.OIL_CANISTER.get(), GCItems.FUEL_CANISTER.get()}) {
                ItemProperties.register(canister, GalacticraftCore.id("fill"),
                        (stack, level, entity, seed) -> LiquidCanisterItem.fillStage(stack));
            }
            for (Fluid fluid : new Fluid[]{GCFluids.OIL.get(), GCFluids.FLOWING_OIL.get(), GCFluids.FUEL.get(), GCFluids.FLOWING_FUEL.get()}) {
                ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent());
            }
        });
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(liquid("oil"), GCFluids.OIL_TYPE.get());
        event.registerFluidType(liquid("fuel"), GCFluids.FUEL_TYPE.get());
    }

    /** Still/flowing textures and the 1.12.2 "under liquid" screen overlay. */
    private static IClientFluidTypeExtensions liquid(String name) {
        final ResourceLocation still = GalacticraftCore.id("block/fluids/" + name + "_still");
        final ResourceLocation flowing = GalacticraftCore.id("block/fluids/" + name + "_flow");
        final ResourceLocation overlay = GalacticraftCore.id("textures/misc/under" + name + ".png");
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
                return overlay;
            }
        };
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(GCMenus.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
        event.register(GCMenus.ENERGY_STORAGE.get(), EnergyStorageScreen::new);
        event.register(GCMenus.COMPRESSOR.get(), CompressorScreen::new);
        event.register(GCMenus.ELECTRIC_COMPRESSOR.get(), ElectricCompressorScreen::new);
        event.register(GCMenus.ADVANCED_COMPRESSOR.get(), ElectricCompressorScreen::new);
        event.register(GCMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(GCMenus.ELECTRIC_ARC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(GCMenus.CIRCUIT_FABRICATOR.get(), CircuitFabricatorScreen::new);
        event.register(GCMenus.SOLAR_PANEL.get(), SolarPanelScreen::new);
        event.register(GCMenus.REFINERY.get(), RefineryScreen::new);
    }

    private static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(GCDimensions.MOON_EFFECTS, new MoonEffects());
    }

    /**
     * Airless sky: no clouds, black fog. The 1.12.2 SkyProviderMoon (Earth in the sky, always-visible stars)
     * is a later porting phase; until then the vanilla sun/moon/stars render over a black sky colour.
     */
    static final class MoonEffects extends DimensionSpecialEffects {
        MoonEffects() {
            super(Float.NaN, true, SkyType.NORMAL, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return Vec3.ZERO;
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }

        @Override
        public float[] getSunriseColor(float timeOfDay, float partialTicks) {
            // No atmosphere, no sunrise glow.
            return null;
        }
    }
}
