package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.screen.CircuitFabricatorScreen;
import io.github.reiko45.galacticraft.client.screen.CoalGeneratorScreen;
import io.github.reiko45.galacticraft.client.screen.CompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricCompressorScreen;
import io.github.reiko45.galacticraft.client.screen.ElectricFurnaceScreen;
import io.github.reiko45.galacticraft.client.screen.EnergyStorageScreen;
import io.github.reiko45.galacticraft.registry.GCMenus;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only entry point. */
@Mod(value = GalacticraftCore.MOD_ID, dist = Dist.CLIENT)
public final class GalacticraftClient {
    public GalacticraftClient(IEventBus modBus) {
        modBus.addListener(GalacticraftClient::registerDimensionEffects);
        modBus.addListener(GalacticraftClient::registerScreens);
        ScreenshotTour.registerIfEnabled();
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
