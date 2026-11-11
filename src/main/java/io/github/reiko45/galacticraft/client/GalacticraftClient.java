package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/** Client-only entry point. */
@Mod(value = GalacticraftCore.MOD_ID, dist = Dist.CLIENT)
public final class GalacticraftClient {
    public GalacticraftClient(IEventBus modBus) {
        modBus.addListener(GalacticraftClient::registerDimensionEffects);
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
