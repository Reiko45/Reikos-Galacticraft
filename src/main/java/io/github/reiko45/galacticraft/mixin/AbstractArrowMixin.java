package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.12.2 TransformerHooks#getArrowGravity: arrows (and tridents) fly much flatter on other worlds. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "getDefaultGravity", at = @At("RETURN"), cancellable = true)
    private void galacticraftcore$celestialGravity(CallbackInfoReturnable<Double> cir) {
        final AbstractArrow self = (AbstractArrow) (Object) this;
        final CelestialPhysics physics = CelestialPhysics.of(self.level().dimension());
        if (physics != null) {
            cir.setReturnValue((double) physics.arrowGravity());
        }
    }
}
