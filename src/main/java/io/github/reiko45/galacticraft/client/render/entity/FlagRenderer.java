/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.entities.RenderFlag and
 * client.model.ModelFlag). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.race.ClientSpaceRaces;
import io.github.reiko45.galacticraft.entity.FlagEntity;
import io.github.reiko45.galacticraft.race.FlagData;
import io.github.reiko45.galacticraft.race.RaceNetwork;
import io.github.reiko45.galacticraft.world.Atmosphere;
import java.util.UUID;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 RenderFlag + ModelFlag: a 40 pixel pole on a 3 x 3 base (textures/model/flag.png), with the owner's team flag
 * hanging from the top, 48 x 32 cells of plain colour, each half a 24th of a block. The cloth ripples with the world's
 * wind: hard on the Overworld, gently on Mars, not at all on the Moon.
 */
public class FlagRenderer extends EntityRenderer<FlagEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("flag"), "main");
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/flag.png");
    /** 1.12.2 drew the cloth with texturing off: plain vertex colours. */
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final FlagData DEFAULT = new FlagData();

    private final ModelPart model;

    public FlagRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(LAYER);
        this.shadowRadius = 1.0F;
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(4, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 23.0F, 0.0F));
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -40.0F, -0.5F, 1.0F, 40.0F, 1.0F),
                PartPose.offset(0.0F, 23.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** 1.12.2 ClientUtil#updateFlagData: the owner's team flag, white if they have no team. */
    public static FlagData flagOf(FlagEntity flag) {
        final UUID owner = flag.owner();
        RaceNetwork.RaceData race = owner == null ? null : ClientSpaceRaces.of(owner);
        if (race == null) {
            for (RaceNetwork.RaceData each : ClientSpaceRaces.all()) {
                if (each.members().contains(flag.ownerName())) {
                    race = each;
                    break;
                }
            }
        }
        return race == null ? DEFAULT : race.flag();
    }

    @Override
    public void render(FlagEntity flag, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        // 1.12.2's tiny per-entity jitter, so two flags in one place don't z-fight
        long seed = flag.getId() * 493286711L;
        seed = seed * seed * 4392167121L + seed * 98761L;
        final float seedX = (((seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        final float seedY = (((seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        final float seedZ = (((seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(seedX, seedY + 1.5F, seedZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - flag.facingAngle()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        renderCloth(flagOf(flag), flag.tickCount + partialTick, Atmosphere.windLevel(flag.level().dimension()), poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light);
        poseStack.popPose();
        super.render(flag, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** 1.12.2 ModelFlag#renderFlag: each cell a quad, its far edge pushed out by the wave of the next column. */
    private static void renderCloth(FlagData data, float ticks, float wind, PoseStack poseStack, VertexConsumer consumer, int light) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -2.1F, 0.0F);
        final PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < data.width(); i++) {
            float offset = 0.0F;
            float ahead = 0.0F;
            if (wind > 0.0F) {
                offset = (float) (Math.sin(ticks / 2.0F + i * 50 + 3) / 25.0F) * i / 30.0F * wind;
                ahead = (float) (Math.sin(ticks / 2.0F + (i + 1) * 50 + 3) / 25.0F) * (i + 1) / 30.0F * wind;
            }
            final float x0 = i / 24.0F;
            final float x1 = (i + 1) / 24.0F;
            for (int j = 0; j < data.height(); j++) {
                final int rgb = data.colorAt(i, j);
                final float y0 = j / 24.0F;
                final float y1 = (j + 1) / 24.0F;
                vertex(consumer, pose, x0, y0 + offset, offset, 0.0F, 0.0F, rgb, light);
                vertex(consumer, pose, x0, y1 + offset, offset, 0.0F, 1.0F, rgb, light);
                vertex(consumer, pose, x1, y1 + ahead, ahead, 1.0F, 1.0F, rgb, light);
                vertex(consumer, pose, x1, y0 + ahead, ahead, 1.0F, 0.0F, rgb, light);
            }
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int rgb, int light) {
        consumer.addVertex(pose, x, y, z).setColor(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(FlagEntity flag) {
        return TEXTURE;
    }
}
