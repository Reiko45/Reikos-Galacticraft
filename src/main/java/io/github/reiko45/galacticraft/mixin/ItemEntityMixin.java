package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.world.CelestialPhysics;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.12.2 TransformerHooks#getItemGravity: dropped items fall slowly on other worlds. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "getDefaultGravity", at = @At("RETURN"), cancellable = true)
    private void galacticraftcore$celestialGravity(CallbackInfoReturnable<Double> cir) {
        final ItemEntity self = (ItemEntity) (Object) this;
        final CelestialPhysics physics = CelestialPhysics.of(self.level().dimension());
        if (physics != null) {
            cir.setReturnValue(physics.itemGravity());
        }
    }
}
