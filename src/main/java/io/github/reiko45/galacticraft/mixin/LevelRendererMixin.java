package io.github.reiko45.galacticraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.RocketEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rockets and landers are drawn wherever they are. 1.21 skips an entity in a section of the world whose blocks haven't
 * been prepared for drawing yet, and a rocket climbing a block or more a tick outruns that, vanishing in the sky below
 * the world's build height (1.12.2 only asked that the chunk was loaded).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiled(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean galacticraftcore$drawFastVehicles(LevelRenderer renderer, BlockPos pos, Operation<Boolean> original, @Local Entity entity) {
        return entity instanceof RocketEntity || entity instanceof LanderEntity || original.call(renderer, pos);
    }
}
