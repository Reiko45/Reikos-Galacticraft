package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.world.station.Freefall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.12.2 FreefallHandler#postVanillaMotion: in orbit, a player touching nothing floats instead of walking or falling. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected boolean jumping;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void galacticraftcore$freefall(Vec3 travelVector, CallbackInfo ci) {
        final LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && player.level().isClientSide && player.isControlledByLocalInstance() && Freefall.inFreefall(player)) {
            Freefall.travel(player, this.jumping);
            ci.cancel();
        }
    }
}
