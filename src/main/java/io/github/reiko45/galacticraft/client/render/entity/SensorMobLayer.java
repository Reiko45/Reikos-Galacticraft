/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.overlay.OverlaySensorGlasses
 * preRenderMobs, and the RenderEvolved* "texSwitch" second pass). Copyright (c) 2023 Team Galacticraft. Licensed under
 * the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.SensorGlasses;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.12.2 Sensor Glasses mob view: while the local player wears them, the mob's model is drawn a second time over itself,
 * slightly larger (1.07 x 1.035 x 1.07, lowered 0.045) and full-bright, with the sensor_mobs texture.
 */
public class SensorMobLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/entity/sensor_mobs.png");

    public SensorMobLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!SensorGlasses.worn() || entity.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        // 1.12.2 applied these right after the renderer's own scale, before its 1.501 drop
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.translate(0.0F, 0.045F, 0.0F);
        poseStack.scale(1.07F, 1.035F, 1.07F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), LightTexture.FULL_BRIGHT,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
        poseStack.popPose();
    }
}
