package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.RocketItemRenderer;
import io.github.reiko45.galacticraft.client.render.entity.LanderRenderer;
import io.github.reiko45.galacticraft.client.render.entity.ParachestRenderer;
import io.github.reiko45.galacticraft.client.render.entity.Tier1RocketRenderer;
import io.github.reiko45.galacticraft.client.screen.CargoScreen;
import io.github.reiko45.galacticraft.client.screen.CircuitFabricatorScreen;
import io.github.reiko45.galacticraft.client.screen.FuelLoaderScreen;
import io.github.reiko45.galacticraft.client.screen.NasaWorkbenchScreen;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import io.github.reiko45.galacticraft.client.screen.CoalGeneratorScreen;
import io.github.reiko45.galacticraft.client.screen.CompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricCompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricFurnaceScreen;
import io.github.reiko45.galacticraft.client.hud.OxygenHud;
import io.github.reiko45.galacticraft.client.hud.VehicleHud;
import io.github.reiko45.galacticraft.client.render.BubbleRenderer;
import io.github.reiko45.galacticraft.client.render.GearLayer;
import io.github.reiko45.galacticraft.client.screen.EnergyStorageScreen;
import io.github.reiko45.galacticraft.client.screen.ExtendedInventoryScreen;
import io.github.reiko45.galacticraft.client.screen.OxygenMachineScreen;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
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
        modBus.addListener(GalacticraftClient::registerGuiLayers);
        modBus.addListener(GalacticraftClient::addPlayerLayers);
        modBus.addListener(VehicleControls::registerKeys);
        NeoForge.EVENT_BUS.addListener(VehicleControls::onClientTick);
        NeoForge.EVENT_BUS.addListener(InventoryTabs::onScreenInit);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientPayloads.reset());
        ScreenshotTour.registerIfEnabled();
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SolarPanelRenderer.LAYER, SolarPanelRenderer::createLayer);
        event.registerLayerDefinition(GearLayer.MASK, GearLayer::createMask);
        event.registerLayerDefinition(GearLayer.GEAR, GearLayer::createGear);
        event.registerLayerDefinition(GearLayer.PARACHUTE, GearLayer::createParachute);
        event.registerLayerDefinition(Tier1RocketRenderer.LAYER, Tier1RocketRenderer::createLayer);
        event.registerLayerDefinition(LanderRenderer.LAYER, LanderRenderer::createLayer);
        event.registerLayerDefinition(ParachestRenderer.CHEST, ParachestRenderer::createChest);
        event.registerLayerDefinition(ParachestRenderer.CHUTE, ParachestRenderer::createChute);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GCBlockEntities.BASIC_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.ADVANCED_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_PIPE.get(), FluidPipeRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.OXYGEN_DISTRIBUTOR.get(), BubbleRenderer::new);
        event.registerEntityRenderer(GCEntities.TIER_1_ROCKET.get(), Tier1RocketRenderer::new);
        event.registerEntityRenderer(GCEntities.LANDER.get(), LanderRenderer::new);
        event.registerEntityRenderer(GCEntities.PARACHEST.get(), ParachestRenderer::new);
    }

    private static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new GearLayer(renderer, event.getEntityModels()));
            }
        }
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, OxygenHud.TANKS_LAYER, OxygenHud::renderTanks);
        event.registerAbove(OxygenHud.TANKS_LAYER, OxygenHud.WARNING_LAYER, OxygenHud::renderWarning);
        event.registerAbove(OxygenHud.WARNING_LAYER, VehicleHud.LAYER, VehicleHud::render);
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
            ItemBlockRenderTypes.setRenderLayer(GCBlocks.UNLIT_TORCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GCBlocks.UNLIT_WALL_TORCH.get(), RenderType.cutout());
            for (Fluid fluid : new Fluid[]{GCFluids.OIL.get(), GCFluids.FLOWING_OIL.get(), GCFluids.FUEL.get(), GCFluids.FLOWING_FUEL.get()}) {
                ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent());
            }
        });
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private RocketItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RocketItemRenderer();
                }
                return this.renderer;
            }
        }, GCItems.TIER_1_ROCKET.get());
        event.registerFluidType(liquid("oil"), GCFluids.OIL_TYPE.get());
        event.registerFluidType(liquid("fuel"), GCFluids.FUEL_TYPE.get());
        final ResourceLocation oxygen = GalacticraftCore.id("block/fluids/oxygen_gas");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return oxygen;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return oxygen;
            }
        }, GCFluids.OXYGEN_TYPE.get());
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
        event.register(GCMenus.OXYGEN_COLLECTOR.get(), OxygenMachineScreen::new);
        event.register(GCMenus.OXYGEN_COMPRESSOR.get(), OxygenMachineScreen::new);
        event.register(GCMenus.OXYGEN_DECOMPRESSOR.get(), OxygenMachineScreen::new);
        event.register(GCMenus.OXYGEN_DISTRIBUTOR.get(), OxygenMachineScreen::new);
        event.register(GCMenus.OXYGEN_SEALER.get(), OxygenMachineScreen::new);
        event.register(GCMenus.OXYGEN_STORAGE_MODULE.get(), OxygenMachineScreen::new);
        event.register(GCMenus.EXTENDED_INVENTORY.get(), ExtendedInventoryScreen::new);
        event.register(GCMenus.FUEL_LOADER.get(), FuelLoaderScreen::new);
        event.register(GCMenus.CARGO.get(), CargoScreen::new);
        event.register(GCMenus.NASA_WORKBENCH.get(), NasaWorkbenchScreen::new);
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
