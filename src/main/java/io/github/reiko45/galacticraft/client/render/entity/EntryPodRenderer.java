/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity.RenderEntryPod).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.EntryPodEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 1.12.2 RenderEntryPod: the pod OBJ's PodBody (models/entity/entry_pod.json), scaled 0.65 x 0.6 x 0.65. */
public class EntryPodRenderer extends EntityRenderer<EntryPodEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("entity/entry_pod");

    public EntryPodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(EntryPodEntity pod, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        final float pitch = Mth.lerp(partialTick, pod.xRotO, pod.getXRot());
        final float yaw = Mth.lerp(partialTick, pod.yRotO, pod.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F - pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.65F, 0.6F, 0.65F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, light);
        poseStack.popPose();
        super.render(pod, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntryPodEntity pod) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
