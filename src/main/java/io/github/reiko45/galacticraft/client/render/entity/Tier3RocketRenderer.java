/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.asteroids.client.render.entity.RenderTier3Rocket).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.client.render.BakedModels;
import io.github.reiko45.galacticraft.entity.Tier3RocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 1.12.2 RenderTier3Rocket: the tier3rocket OBJ at 0.8 scale in three parts (models/entity/tier_3_rocket/*.json): the
 * body and boosters, the nose cone (untextured, in the viewer's Space Race team colour), and a small light that blinks red and green every half second, unlit. It rocks when
 * hit, and harder as the launch countdown runs down.
 */
public class Tier3RocketRenderer extends EntityRenderer<Tier3RocketEntity> {
    public static final ModelResourceLocation BODY = BakedModels.location("entity/tier_3_rocket/body");
    public static final ModelResourceLocation NOSE_CONE = BakedModels.location("entity/tier_3_rocket/nose_cone");
    public static final ModelResourceLocation LIGHT = BakedModels.location("entity/tier_3_rocket/light");
    private static final net.minecraft.resources.ResourceLocation WHITE = net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    /** 1.12.2 EntityTier3Rocket#getRenderOffsetY. */
    private static final float RENDER_OFFSET_Y = -1.0F;

    public Tier3RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
    }

    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(NOSE_CONE);
        event.register(LIGHT);
    }

    /** The model as 1.12.2 drew it after its own transforms: upside down, flipped back by scale(-1, -1, 1), at 0.8. */
    public static void renderModel(PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        BakedModels.draw(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), BODY, light);
        // 1.12.2: the nose cone untextured, in the viewer's Space Race team colour (white without a team)
        final int team = Minecraft.getInstance().player == null ? 0xFFFFFF
                : io.github.reiko45.galacticraft.client.race.ClientSpaceRaces.teamColor(Minecraft.getInstance().player.getUUID());
        BakedModels.drawTinted(poseStack, buffers.getBuffer(RenderType.entityCutout(WHITE)), NOSE_CONE, light,
                (team >> 16 & 255) / 255.0F, (team >> 8 & 255) / 255.0F, (team & 255) / 255.0F);
        // 1.12.2: drawn untextured and unlit, red or green by turns of 10 of the player's ticks
        final boolean red = Minecraft.getInstance().player == null || Minecraft.getInstance().player.tickCount / 10 % 2 < 1;
        BakedModels.drawTinted(poseStack, buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), LIGHT, LightTexture.FULL_BRIGHT,
                red ? 1.0F : 0.0F, red ? 0.0F : 1.0F, 0.0F);
        poseStack.popPose();
    }

    @Override
    public void render(Tier3RocketEntity rocket, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        final float pitch = Mth.lerp(partialTick, rocket.xRotO, rocket.getXRot()) + 180.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.translate(0.0F, RENDER_OFFSET_Y, 0.0F);
        final float roll = rocket.roll() / 3.0F - partialTick;
        if (roll > 0.0F) {
            // 1.12.2: (5 - floor(timeUntilLaunch / 85)) / 10 once launched, when the countdown is over
            final float strength = rocket.launched() ? 0.5F : 0.3F;
            final float angle = Mth.sin(roll) * roll * strength * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees(angle));
            poseStack.mulPose(new org.joml.Quaternionf().rotationAxis(angle * Mth.DEG_TO_RAD, 0.70710677F, 0.0F, 0.70710677F));
        }
        renderModel(poseStack, buffers, light);
        poseStack.popPose();
        super.render(rocket, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Tier3RocketEntity rocket) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
