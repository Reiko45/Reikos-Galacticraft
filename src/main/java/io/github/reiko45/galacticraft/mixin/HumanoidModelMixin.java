package io.github.reiko45.galacticraft.mixin;

import io.github.reiko45.galacticraft.client.render.PlayerPoses;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.12.2 MicdoodleTransformer#transformModelBiped: Galacticraft's player poses after the vanilla ones. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void galacticraftcore$poses(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                        float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            PlayerPoses.setupAnim((HumanoidModel<?>) (Object) this, player, limbSwing, limbSwingAmount, ageInTicks);
        }
    }
}
