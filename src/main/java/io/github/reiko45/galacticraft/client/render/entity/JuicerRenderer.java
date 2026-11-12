/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.venus.client.render.entity.RenderJuicer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.JuicerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.12.2 RenderJuicer: upside down while it hangs from the ceiling. */
public class JuicerRenderer extends MobRenderer<JuicerEntity, JuicerModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/juicer.png");

    public JuicerRenderer(EntityRendererProvider.Context context) {
        super(context, new JuicerModel(context.bakeLayer(JuicerModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(JuicerEntity juicer, PoseStack poseStack, float partialTick) {
        final boolean hanging = juicer.isHanging();
        if (hanging) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        }
        poseStack.translate(0.0F, hanging ? 1.8F : 1.3F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(JuicerEntity juicer) {
        return TEXTURE;
    }
}
