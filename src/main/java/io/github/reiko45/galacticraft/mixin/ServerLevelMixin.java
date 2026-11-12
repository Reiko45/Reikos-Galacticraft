package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.world.VenusWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Venus runs its own weather cycle instead of following the Overworld's ({@link VenusWeather}). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void galacticraft$venusWeather(CallbackInfo ci) {
        if (VenusWeather.tick((ServerLevel) (Object) this)) {
            ci.cancel();
        }
    }
}
