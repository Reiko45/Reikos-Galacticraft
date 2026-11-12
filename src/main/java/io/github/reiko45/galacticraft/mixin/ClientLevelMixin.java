package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.client.render.PlanetSkyRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2 WorldProviderSpace#getSkyColor: on Mars the sky colour follows WorldProviderMars (it dims to a quarter at night
 * instead of going black). The sky dome and vanilla's fog-towards-the-sky blending both read it from here.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void galacticraft$planetSkyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        final ClientLevel level = (ClientLevel) (Object) this;
        final PlanetSkyRenderer.Style style = PlanetSkyRenderer.styleOf(level);
        if (style != null) {
            cir.setReturnValue(style.skyColor(level.getTimeOfDay(partialTick)));
        }
    }
}
