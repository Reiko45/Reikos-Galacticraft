/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.render.entity.RenderEntryPodVenus).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.VenusEntryPodEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderEntryPodVenus: the pod_flame OBJ's PodBody like the asteroid Entry Pod; above y 382 two glowing,
 * spinning, pulsing shells of its Flame_Sphere (fully bright, added to what is behind, fading as the pod slows below
 * 0.6 blocks a tick); below y 242 the balloons' parachute over it.
 */
public class VenusEntryPodRenderer extends EntityRenderer<VenusEntryPodEntity> {
    public static final ModelResourceLocation BODY = BakedModels.location("entity/venus_entry_pod/body");
    public static final ModelResourceLocation FLAME = BakedModels.location("entity/venus_entry_pod/flame");
    private static final ResourceLocation CHUTE_TEXTURE = GalacticraftCore.id("textures/model/parachute/gray.png");

    private final ModelPart chute;

    public VenusEntryPodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.chute = context.bakeLayer(LandingBalloonsRenderer.CHUTE);
        this.shadowRadius = 1.0F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(FLAME);
    }

    @Override
    public void render(VenusEntryPodEntity pod, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        final float pitch = Mth.lerp(partialTick, pod.xRotO, pod.getXRot());
        final float yaw = Mth.lerp(partialTick, pod.yRotO, pod.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F - pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.65F, 0.6F, 0.65F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), BODY, light);
        final double y = Mth.lerp(partialTick, pod.yo, pod.getY());
        if (y > VenusEntryPodEntity.FLAME_HEIGHT) {
            final double speed = pod.getY() - pod.yo;
            final float glow = y >= 790.0D ? 1.0F : Mth.clamp((float) (-(speed + 0.6D) * 100.0D), 0.0F, 255.0F) / 255.0F;
            final float age = pod.tickCount + partialTick;
            final float pulse = Mth.sin(pod.tickCount) / 20.0F + 0.5F;
            final var flame = buffers.getBuffer(RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
            poseStack.pushPose();
            poseStack.scale(1.0F, 1.0F + pulse, 1.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 20.0F));
            BakedModels.drawTinted(poseStack, flame, FLAME, LightTexture.FULL_BRIGHT, glow, glow, glow);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.scale(1.0F, 1.0F + pulse / 6.0F, 1.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 5.0F));
            BakedModels.drawTinted(poseStack, flame, FLAME, LightTexture.FULL_BRIGHT, glow, glow, glow);
            poseStack.popPose();
        }
        if (pod.parachuteOpen()) {
            poseStack.pushPose();
            poseStack.translate(-1.4F, 1.5F, -0.3F);
            poseStack.scale(2.5F, 3.0F, 2.5F);
            this.chute.render(poseStack, buffers.getBuffer(RenderType.entityCutout(CHUTE_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(pod, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(VenusEntryPodEntity pod) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
