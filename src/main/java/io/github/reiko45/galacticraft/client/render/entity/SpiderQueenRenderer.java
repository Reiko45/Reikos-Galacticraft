/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.render.entity.RenderSpiderQueen).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.SpiderQueenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderSpiderQueen: half as big again as her model, spun ever faster as she dies, and while hiding upside
 * down on the ceiling inside two layers of the web sphere.
 */
public class SpiderQueenRenderer extends MobRenderer<SpiderQueenEntity, SpiderQueenModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/spider_queen.png");
    public static final ModelResourceLocation COCOON = BakedModels.location("entity/web_cocoon");

    public SpiderQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderQueenModel(context.bakeLayer(SpiderQueenModel.LAYER)), 1.0F);
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(COCOON);
    }

    @Override
    protected void scale(SpiderQueenEntity queen, PoseStack poseStack, float partialTick) {
        if (queen.burrowedCount() >= 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, queen.getBbHeight(), 0.0F);
        }
        poseStack.scale(1.5F, 1.5F, 1.5F);
        final double ticks = queen.deathTime;
        final double spin = Math.pow(ticks, 2) / 5.0D + (Math.pow(ticks, 2) / 5.0D - Math.pow(ticks - 1, 2) / 5.0D) * partialTick;
        poseStack.mulPose(Axis.YP.rotationDegrees((float) spin));
    }

    /** Death is the spin and explosions, not vanilla's topple. */
    @Override
    protected void setupRotations(SpiderQueenEntity queen, PoseStack poseStack, float bob, float yaw, float partialTick, float scale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }

    @Override
    public void render(SpiderQueenEntity queen, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        super.render(queen, entityYaw, partialTick, poseStack, buffers, light);
        if (queen.burrowedCount() >= 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.8F, 0.0F);
            poseStack.scale(1.4F, 1.5F, 1.4F);
            final var consumer = buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
            BakedModels.draw(poseStack, consumer, COCOON, light);
            poseStack.scale(1.05F, 1.1F, 1.05F);
            poseStack.mulPose(Axis.YP.rotationDegrees(192.5F));
            BakedModels.draw(poseStack, consumer, COCOON, light);
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SpiderQueenEntity queen) {
        return TEXTURE;
    }
}
