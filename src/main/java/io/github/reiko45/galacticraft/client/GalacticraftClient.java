package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.FootprintRenderer;
import io.github.reiko45.galacticraft.client.render.MoonSkyRenderer;
import io.github.reiko45.galacticraft.client.render.RocketItemRenderer;
import io.github.reiko45.galacticraft.block.FallenMeteorBlock;
import io.github.reiko45.galacticraft.client.render.entity.LanderRenderer;
import io.github.reiko45.galacticraft.client.render.entity.MeteorRenderer;
import io.github.reiko45.galacticraft.client.render.entity.monster.EvolvedMobModels;
import io.github.reiko45.galacticraft.client.render.entity.monster.EvolvedRenderers;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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
        modBus.addListener(GalacticraftClient::registerBlockColors);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) -> event.registerSpriteSet(
                io.github.reiko45.galacticraft.registry.GCParticles.BACTERIAL_DRIP.get(),
                io.github.reiko45.galacticraft.client.particle.BacterialDripParticle.Provider::new));
        NeoForge.EVENT_BUS.addListener(VehicleControls::onClientTick);
        NeoForge.EVENT_BUS.addListener(GalacticraftClient::orbitTick);
        NeoForge.EVENT_BUS.addListener(FootprintRenderer::onClientTick);
        NeoForge.EVENT_BUS.addListener(FootprintRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(InventoryTabs::onScreenInit);
        NeoForge.EVENT_BUS.addListener(io.github.reiko45.galacticraft.client.render.CryogenicChamberRender::pre);
        NeoForge.EVENT_BUS.addListener(io.github.reiko45.galacticraft.client.render.CryogenicChamberRender::post);
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
        event.registerLayerDefinition(MeteorRenderer.LAYER, MeteorRenderer::createLayer);
        event.registerLayerDefinition(EvolvedMobModels.ZOMBIE, EvolvedMobModels::zombie);
        event.registerLayerDefinition(EvolvedMobModels.SKELETON, EvolvedMobModels::skeleton);
        event.registerLayerDefinition(EvolvedMobModels.CREEPER, EvolvedMobModels::creeper);
        event.registerLayerDefinition(EvolvedMobModels.SPIDER, EvolvedMobModels::spider);
        event.registerLayerDefinition(EvolvedMobModels.ENDERMAN, EvolvedMobModels::enderman);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.entity.SkeletonBossModel.LAYER,
                io.github.reiko45.galacticraft.client.render.entity.SkeletonBossModel::createLayer);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.entity.CreeperBossModel.LAYER,
                io.github.reiko45.galacticraft.client.render.entity.CreeperBossModel::createLayer);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.entity.SludgelingModel.LAYER,
                io.github.reiko45.galacticraft.client.render.entity.SludgelingModel::createLayer);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.entity.SlimelingModel.LAYER,
                io.github.reiko45.galacticraft.client.render.entity.SlimelingModel::createLayer);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.TreasureChestRenderer.LAYER,
                io.github.reiko45.galacticraft.client.render.TreasureChestRenderer::createLayer);
        event.registerLayerDefinition(ParachestRenderer.CHEST, ParachestRenderer::createChest);
        event.registerLayerDefinition(ParachestRenderer.CHUTE, ParachestRenderer::createChute);
        event.registerLayerDefinition(io.github.reiko45.galacticraft.client.render.entity.LandingBalloonsRenderer.CHUTE,
                io.github.reiko45.galacticraft.client.render.entity.LandingBalloonsRenderer::createChute);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GCBlockEntities.BASIC_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.ADVANCED_SOLAR_PANEL.get(), SolarPanelRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.FLUID_PIPE.get(), FluidPipeRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.OXYGEN_DISTRIBUTOR.get(), BubbleRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.TERRAFORMER.get(), BubbleRenderer::new);
        event.registerEntityRenderer(GCEntities.TIER_1_ROCKET.get(), Tier1RocketRenderer::new);
        event.registerEntityRenderer(GCEntities.TIER_2_ROCKET.get(), io.github.reiko45.galacticraft.client.render.entity.Tier2RocketRenderer::new);
        event.registerEntityRenderer(GCEntities.TIER_3_ROCKET.get(), io.github.reiko45.galacticraft.client.render.entity.Tier3RocketRenderer::new);
        event.registerEntityRenderer(GCEntities.ENTRY_POD.get(), io.github.reiko45.galacticraft.client.render.entity.EntryPodRenderer::new);
        event.registerEntityRenderer(GCEntities.CARGO_ROCKET.get(), io.github.reiko45.galacticraft.client.render.entity.CargoRocketRenderer::new);
        event.registerEntityRenderer(GCEntities.LANDING_BALLOONS.get(), io.github.reiko45.galacticraft.client.render.entity.LandingBalloonsRenderer::new);
        event.registerEntityRenderer(GCEntities.LANDER.get(), LanderRenderer::new);
        event.registerEntityRenderer(GCEntities.PARACHEST.get(), ParachestRenderer::new);
        event.registerEntityRenderer(GCEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(GCEntities.EVOLVED_ZOMBIE.get(), EvolvedRenderers.Zombies::new);
        event.registerEntityRenderer(GCEntities.EVOLVED_SKELETON.get(), EvolvedRenderers.Skeletons::new);
        event.registerEntityRenderer(GCEntities.EVOLVED_SPIDER.get(), EvolvedRenderers.Spiders::new);
        event.registerEntityRenderer(GCEntities.EVOLVED_CREEPER.get(), EvolvedRenderers.Creepers::new);
        event.registerEntityRenderer(GCEntities.EVOLVED_ENDERMAN.get(), EvolvedRenderers.Endermen::new);
        event.registerEntityRenderer(GCEntities.BUGGY.get(), io.github.reiko45.galacticraft.client.render.entity.BuggyRenderer::new);
        event.registerEntityRenderer(GCEntities.SKELETON_BOSS.get(), io.github.reiko45.galacticraft.client.render.entity.SkeletonBossRenderer::new);
        event.registerEntityRenderer(GCEntities.CREEPER_BOSS.get(), io.github.reiko45.galacticraft.client.render.entity.CreeperBossRenderer::new);
        event.registerEntityRenderer(GCEntities.TNT_PROJECTILE.get(), io.github.reiko45.galacticraft.client.render.entity.TntProjectileRenderer::new);
        event.registerEntityRenderer(GCEntities.SLUDGELING.get(), io.github.reiko45.galacticraft.client.render.entity.SludgelingRenderer::new);
        event.registerEntityRenderer(GCEntities.SLIMELING.get(), io.github.reiko45.galacticraft.client.render.entity.SlimelingRenderer::new);
        event.registerBlockEntityRenderer(GCBlockEntities.TREASURE_CHEST.get(), io.github.reiko45.galacticraft.client.render.TreasureChestRenderer::new);
    }

    private static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new GearLayer(renderer, event.getEntityModels()));
            }
        }
    }

    /** 1.12.2 ClientProxyCore block colours: the Fallen Meteor glows orange while hot. */
    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> FallenMeteorBlock.tint(level, pos), GCBlocks.FALLEN_METEOR.get());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, OxygenHud.TANKS_LAYER, OxygenHud::renderTanks);
        event.registerAbove(OxygenHud.TANKS_LAYER, OxygenHud.WARNING_LAYER, OxygenHud::renderWarning);
        event.registerAbove(OxygenHud.WARNING_LAYER, VehicleHud.LAYER, VehicleHud::render);
    }

    private static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FluidPipeRenderer.PULL_MODEL);
        io.github.reiko45.galacticraft.client.render.entity.BuggyRenderer.registerModels(event);
        io.github.reiko45.galacticraft.client.render.entity.Tier2RocketRenderer.registerModels(event);
        io.github.reiko45.galacticraft.client.render.entity.CargoRocketRenderer.registerModels(event);
        io.github.reiko45.galacticraft.client.render.entity.Tier3RocketRenderer.registerModels(event);
        io.github.reiko45.galacticraft.client.render.entity.EntryPodRenderer.registerModels(event);
        io.github.reiko45.galacticraft.client.render.entity.LandingBalloonsRenderer.registerModels(event);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Canister textures <canister>_partial_0..6 are picked by the "fill" stage.
            for (Item canister : LiquidCanisterItem.items()) {
                ItemProperties.register(canister, GalacticraftCore.id("fill"),
                        (stack, level, entity, seed) -> LiquidCanisterItem.fillStage(stack));
            }
            // Buggy item: 1.12.2 buggy types 1-3 showed 1-3 storage boxes.
            ItemProperties.register(GCItems.BUGGY.get(), GalacticraftCore.id("storage"),
                    (stack, level, entity, seed) -> io.github.reiko45.galacticraft.item.BuggyItem.storage(stack));
            ItemBlockRenderTypes.setRenderLayer(GCBlocks.UNLIT_TORCH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(GCBlocks.UNLIT_WALL_TORCH.get(), RenderType.cutout());
            for (Fluid fluid : new Fluid[]{GCFluids.OIL.get(), GCFluids.FLOWING_OIL.get(), GCFluids.FUEL.get(), GCFluids.FLOWING_FUEL.get(),
                    GCFluids.BACTERIAL_SLUDGE.get(), GCFluids.FLOWING_BACTERIAL_SLUDGE.get()}) {
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
                    this.renderer = new RocketItemRenderer(1);
                }
                return this.renderer;
            }
        }, GCItems.TIER_1_ROCKET.get());
        event.registerItem(new IClientItemExtensions() {
            private RocketItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RocketItemRenderer(2);
                }
                return this.renderer;
            }
        }, GCItems.TIER_2_ROCKET.get());
        event.registerItem(new IClientItemExtensions() {
            private RocketItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RocketItemRenderer(3);
                }
                return this.renderer;
            }
        }, GCItems.TIER_3_ROCKET.get());
        event.registerItem(new IClientItemExtensions() {
            private RocketItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new RocketItemRenderer(0);
                }
                return this.renderer;
            }
        }, GCItems.CARGO_ROCKET.get());
        event.registerItem(new IClientItemExtensions() {
            private io.github.reiko45.galacticraft.client.render.TreasureChestRenderer.Item renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new io.github.reiko45.galacticraft.client.render.TreasureChestRenderer.Item();
                }
                return this.renderer;
            }
        }, GCBlocks.TREASURE_CHEST_TIER_1.asItem(), GCBlocks.TREASURE_CHEST_TIER_2.asItem());
        event.registerFluidType(liquid("oil"), GCFluids.OIL_TYPE.get());
        event.registerFluidType(liquid("fuel"), GCFluids.FUEL_TYPE.get());
        event.registerFluidType(liquid("bacterial_sludge"), GCFluids.BACTERIAL_SLUDGE_TYPE.get());
        event.registerFluidType(tankOnly("oxygen_gas"), GCFluids.OXYGEN_TYPE.get());
        event.registerFluidType(tankOnly("hydrogen_gas"), GCFluids.HYDROGEN_TYPE.get());
        event.registerFluidType(tankOnly("methane"), GCFluids.METHANE_TYPE.get());
        event.registerFluidType(tankOnly("atmospheric_gases"), GCFluids.ATMOSPHERIC_GASES_TYPE.get());
        event.registerFluidType(tankOnly("carbon_dioxide"), GCFluids.CARBON_DIOXIDE_TYPE.get());
        event.registerFluidType(tankOnly("nitrogen"), GCFluids.NITROGEN_TYPE.get());
        event.registerFluidType(tankOnly("argon"), GCFluids.ARGON_TYPE.get());
        event.registerFluidType(tankOnly("liquid_oxygen"), GCFluids.LIQUID_OXYGEN_TYPE.get());
        event.registerFluidType(tankOnly("liquid_nitrogen"), GCFluids.LIQUID_NITROGEN_TYPE.get());
        event.registerFluidType(tankOnly("liquid_argon"), GCFluids.LIQUID_ARGON_TYPE.get());
    }

    /** A gas or liquid that only shows inside tanks and pipes: one (animated) texture for still and flowing. */
    private static IClientFluidTypeExtensions tankOnly(String texture) {
        final ResourceLocation location = GalacticraftCore.id("block/fluids/" + texture);
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return location;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return location;
            }
        };
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
        event.register(GCMenus.CARGO_LOADER.get(), io.github.reiko45.galacticraft.client.screen.CargoLoaderScreen::new);
        event.register(GCMenus.CARGO_UNLOADER.get(), io.github.reiko45.galacticraft.client.screen.CargoLoaderScreen::new);
        event.register(GCMenus.CARGO.get(), CargoScreen::new);
        event.register(GCMenus.GAS_LIQUEFIER.get(), io.github.reiko45.galacticraft.client.screen.GasMachineScreen::new);
        event.register(GCMenus.METHANE_SYNTHESIZER.get(), io.github.reiko45.galacticraft.client.screen.GasMachineScreen::new);
        event.register(GCMenus.ELECTROLYZER.get(), io.github.reiko45.galacticraft.client.screen.GasMachineScreen::new);
        event.register(GCMenus.TERRAFORMER.get(), io.github.reiko45.galacticraft.client.screen.TerraformerScreen::new);
        event.register(GCMenus.LAUNCH_CONTROLLER.get(), io.github.reiko45.galacticraft.client.screen.LaunchControllerScreen::new);
        event.register(GCMenus.SLIMELING.get(), io.github.reiko45.galacticraft.client.screen.SlimelingInventoryScreen::new);
        event.register(GCMenus.NASA_WORKBENCH.get(), NasaWorkbenchScreen::new);
        event.register(GCMenus.BUGGY_BENCH.get(), io.github.reiko45.galacticraft.client.screen.BuggyBenchScreen::new);
        event.register(GCMenus.TIER_2_ROCKET_BENCH.get(), io.github.reiko45.galacticraft.client.screen.Tier2RocketBenchScreen::new);
        event.register(GCMenus.TIER_3_ROCKET_BENCH.get(), io.github.reiko45.galacticraft.client.screen.Tier3RocketBenchScreen::new);
        event.register(GCMenus.CARGO_ROCKET_BENCH.get(), io.github.reiko45.galacticraft.client.screen.CargoRocketBenchScreen::new);
        event.register(GCMenus.SCHEMATIC_INPUT.get(), io.github.reiko45.galacticraft.client.screen.SchematicInputScreen::new);
    }

    /** In orbit: turn the sky with the station's spin, and carry a floating player round with it. */
    private static void orbitTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || minecraft.isPaused()
                || !io.github.reiko45.galacticraft.world.station.StationSpin.Client.inOrbit(minecraft.player.level())) {
            io.github.reiko45.galacticraft.client.render.OrbitSkyRenderer.spinDegreesPerTick = 0.0F;
            return;
        }
        final var spin = io.github.reiko45.galacticraft.world.station.StationSpin.Client.at(minecraft.player.blockPosition());
        io.github.reiko45.galacticraft.client.render.OrbitSkyRenderer.spinDegreesPerTick = spin == null ? 0.0F
                : spin.angularVelocity() * net.minecraft.util.Mth.RAD_TO_DEG;
        io.github.reiko45.galacticraft.world.station.Freefall.spin(minecraft.player);
    }

    private static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(GCDimensions.MOON_EFFECTS, new MoonEffects());
        event.register(GCDimensions.ORBIT_EFFECTS, new OrbitEffects());
        event.register(GCDimensions.MARS_EFFECTS, new MarsEffects());
        event.register(GCDimensions.ASTEROIDS_EFFECTS, new AsteroidsEffects());
    }

    /** The asteroid belt: black sky and fog, no clouds or sunrise glow, the 1.12.2 asteroid sky. */
    static final class AsteroidsEffects extends DimensionSpecialEffects {
        private final io.github.reiko45.galacticraft.client.render.AsteroidsSkyRenderer sky = new io.github.reiko45.galacticraft.client.render.AsteroidsSkyRenderer();

        AsteroidsEffects() {
            super(Float.NaN, false, SkyType.NORMAL, false, false);
        }

        @Override
        public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, org.joml.Matrix4f modelViewMatrix,
                                 net.minecraft.client.Camera camera, org.joml.Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            return this.sky.render(level, partialTick, modelViewMatrix, camera, projectionMatrix, setupFog);
        }

        @Override
        public boolean renderClouds(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    double camX, double camY, double camZ, org.joml.Matrix4f modelViewMatrix, org.joml.Matrix4f projectionMatrix) {
            return true;
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
            return null;
        }
    }

    /** Mars: no clouds or sunrise glow, the 1.12.2 Mars sky, and orange fog that dims to a quarter at night. */
    static final class MarsEffects extends DimensionSpecialEffects {
        private final io.github.reiko45.galacticraft.client.render.MarsSkyRenderer sky = new io.github.reiko45.galacticraft.client.render.MarsSkyRenderer();

        MarsEffects() {
            super(Float.NaN, true, SkyType.NORMAL, false, false);
        }

        @Override
        public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, org.joml.Matrix4f modelViewMatrix,
                                 net.minecraft.client.Camera camera, org.joml.Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            return this.sky.render(level, partialTick, modelViewMatrix, camera, projectionMatrix, setupFog);
        }

        @Override
        public boolean renderClouds(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    double camX, double camY, double camZ, org.joml.Matrix4f modelViewMatrix, org.joml.Matrix4f projectionMatrix) {
            return true;
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            final net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
            return level == null ? fogColor : io.github.reiko45.galacticraft.client.render.MarsSkyRenderer.fogColor(level.getTimeOfDay(1.0F));
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }

        @Override
        public float[] getSunriseColor(float timeOfDay, float partialTicks) {
            return null; // 1.12.2 WorldProviderMars#hasSunset
        }
    }

    /** Earth orbit: no clouds or fog, the 1.12.2 orbit sky with the Earth below. */
    static final class OrbitEffects extends DimensionSpecialEffects {
        private final io.github.reiko45.galacticraft.client.render.OrbitSkyRenderer sky = new io.github.reiko45.galacticraft.client.render.OrbitSkyRenderer();

        OrbitEffects() {
            super(Float.NaN, false, SkyType.NORMAL, false, false);
        }

        @Override
        public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, org.joml.Matrix4f modelViewMatrix,
                                 net.minecraft.client.Camera camera, org.joml.Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            return this.sky.render(level, partialTick, modelViewMatrix, camera, projectionMatrix, setupFog);
        }

        @Override
        public boolean renderClouds(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    double camX, double camY, double camZ, org.joml.Matrix4f modelViewMatrix, org.joml.Matrix4f projectionMatrix) {
            return true;
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
            return null;
        }
    }

    /** Airless sky: no clouds, black fog, no sunrise glow, and the 1.12.2 Moon sky (stars by day, Earth overhead). */
    static final class MoonEffects extends DimensionSpecialEffects {
        private final MoonSkyRenderer sky = new MoonSkyRenderer();

        MoonEffects() {
            super(Float.NaN, true, SkyType.NORMAL, false, false);
        }

        @Override
        public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, org.joml.Matrix4f modelViewMatrix,
                                 net.minecraft.client.Camera camera, org.joml.Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            return this.sky.render(level, partialTick, modelViewMatrix, camera, projectionMatrix, setupFog);
        }

        @Override
        public boolean renderClouds(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    double camX, double camY, double camZ, org.joml.Matrix4f modelViewMatrix, org.joml.Matrix4f projectionMatrix) {
            return true;
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
