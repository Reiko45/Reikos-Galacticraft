/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.render.entity.RenderCargoRocket).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.CargoRocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 1.12.2 RenderCargoRocket: the cargo_rocket OBJ (models/entity/cargo_rocket.json) at 0.4 scale, tilted while it steers in to land. */
public class CargoRocketRenderer extends EntityRenderer<CargoRocketEntity> {
    public static final ModelResourceLocation MODEL = BakedModels.location("entity/cargo_rocket");
    /** 1.12.2 EntityCargoRocket#getRenderOffsetY. */
    private static final float RENDER_OFFSET_Y = -0.1F;

    public CargoRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    public static void renderModel(PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(0.4F, 0.4F, 0.4F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), MODEL, light);
        poseStack.popPose();
    }

    @Override
    public void render(CargoRocketEntity rocket, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        final float pitch = Mth.lerp(partialTick, rocket.xRotO, rocket.getXRot());
        poseStack.translate(0.0F, RENDER_OFFSET_Y, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        final float roll = rocket.roll() - partialTick;
        if (roll > 0.0F) {
            final float angle = Mth.sin(roll) * roll * 0.3F * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        }
        renderModel(poseStack, buffers, light);
        poseStack.popPose();
        super.render(rocket, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CargoRocketEntity rocket) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
