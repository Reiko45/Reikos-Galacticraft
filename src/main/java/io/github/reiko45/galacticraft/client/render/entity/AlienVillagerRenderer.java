/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.entities.RenderAlienVillager).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.AlienVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.12.2 RenderAlienVillager: drawn at 15/16 size, children at half that. */
public class AlienVillagerRenderer extends MobRenderer<AlienVillagerEntity, AlienVillagerModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/villager.png");

    public AlienVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienVillagerModel(context.bakeLayer(AlienVillagerModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(AlienVillagerEntity villager, PoseStack poseStack, float partialTick) {
        float scale = 0.9375F;
        if (villager.isBaby()) {
            scale *= 0.5F;
            this.shadowRadius = 0.25F;
        } else {
            this.shadowRadius = 0.5F;
        }
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienVillagerEntity villager) {
        return TEXTURE;
    }
}
