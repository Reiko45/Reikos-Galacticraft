/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.render.tile.TileEntityEmergencyBoxRenderer).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.EmergencyPostBlock;
import io.github.reiko45.galacticraft.block.entity.EmergencyPostBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 TileEntityEmergencyBoxRenderer: two flaps on each side of the post (upper and lower, swinging apart as the door
 * opens), a plinth rising one pixel as it opens, and, with a kit inside, a red parachute pack, an oxygen mask and two
 * tanks rising into view.
 */
public class EmergencyPostRenderer implements BlockEntityRenderer<EmergencyPostBlockEntity> {
    public static final ModelLayerLocation FLAP = new ModelLayerLocation(GalacticraftCore.id("emergency_post"), "flap");
    public static final ModelLayerLocation PLINTH = new ModelLayerLocation(GalacticraftCore.id("emergency_post"), "plinth");
    public static final ModelLayerLocation MASK = new ModelLayerLocation(GalacticraftCore.id("emergency_post"), "mask");
    public static final ModelLayerLocation TANK = new ModelLayerLocation(GalacticraftCore.id("emergency_post"), "tank");
    public static final ModelLayerLocation PACK = new ModelLayerLocation(GalacticraftCore.id("emergency_post"), "pack");
    private static final ResourceLocation BOX = GalacticraftCore.id("textures/block/emergency_post.png");
    private static final ResourceLocation FLAP_TEXTURE = GalacticraftCore.id("textures/block/emergency_post_flap.png");
    private static final ResourceLocation PACK_TEXTURE = GalacticraftCore.id("textures/model/parachute/red.png");
    private static final ResourceLocation MASK_TEXTURE = GalacticraftCore.id("textures/model/oxygen.png");
    private static final ResourceLocation TANK_TEXTURE = GalacticraftCore.id("textures/model/player.png");
    private static final float MASK_SCALE = 3.0F;

    private final ModelPart flap;
    private final ModelPart plinth;
    private final ModelPart mask;
    private final ModelPart tank;
    private final ModelPart pack;

    public EmergencyPostRenderer(BlockEntityRendererProvider.Context context) {
        this.flap = context.bakeLayer(FLAP).getChild("flap");
        this.plinth = context.bakeLayer(PLINTH).getChild("plinth");
        this.mask = context.bakeLayer(MASK).getChild("mask");
        this.tank = context.bakeLayer(TANK).getChild("tank");
        this.pack = context.bakeLayer(PACK).getChild("pack");
    }

    private static LayerDefinition one(String name, CubeListBuilder cubes, PartPose pose, int width, int height) {
        final MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(name, cubes, pose);
        return LayerDefinition.create(mesh, width, height);
    }

    public static LayerDefinition createFlap() {
        return one("flap", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6, -6, 0, 12, 6, 1), PartPose.offset(0, 6, -7), 32, 32);
    }

    public static LayerDefinition createPlinth() {
        return one("plinth", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6, -7, -6, 12, 1, 12), PartPose.ZERO, 16, 16);
    }

    public static LayerDefinition createMask() {
        return one("mask", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8, -4, -8, 16, 16, 16, new CubeDeformation(1.0F)), PartPose.ZERO, 128, 64);
    }

    public static LayerDefinition createTank() {
        return one("tank", CubeListBuilder.create().texOffs(4, 0).mirror().addBox(-1.5F, 0, -1.5F, 3, 7, 3, new CubeDeformation(1.0F)), PartPose.ZERO, 128, 64);
    }

    public static LayerDefinition createPack() {
        return one("pack", CubeListBuilder.create().texOffs(50, 50).mirror().addBox(-6, -11, -10, 12, 1, 20, new CubeDeformation(1.0F)), PartPose.ZERO, 256, 256);
    }

    /** 1.12.2 ModelRenderer#render(scale) with a scale other than 1/16: shrink the pose, and with it the offset. */
    private static void render(ModelPart part, float scale, float offsetY, PoseStack poseStack, MultiBufferSource buffers, ResourceLocation texture,
                               int light, int overlay) {
        poseStack.pushPose();
        poseStack.scale(scale * 16.0F, scale * 16.0F, scale * 16.0F);
        part.setPos(0.0F, offsetY, 0.0F);
        part.render(poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, overlay);
        poseStack.popPose();
    }

    @Override
    public void render(EmergencyPostBlockEntity post, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!(post.getBlockState().getBlock() instanceof EmergencyPostBlock)) {
            return;
        }
        final float north = post.angle(Direction.NORTH, partialTick);
        final float west = post.angle(Direction.WEST, partialTick);
        final float south = post.angle(Direction.SOUTH, partialTick);
        final float east = post.angle(Direction.EAST, partialTick);
        final float height = Math.max(Math.max(north, west), Math.max(south, east)) / 90.0F;
        overlay = OverlayTexture.NO_OVERLAY;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (height > 0.0F && post.getBlockState().getValue(EmergencyPostBlock.KIT)) {
            poseStack.pushPose();
            render(this.pack, 1.0F / 32.0F, height * 2.0F, poseStack, buffers, PACK_TEXTURE, light, overlay);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, 0.0F, -0.07F);
            render(this.mask, 1.0F / 16.0F / MASK_SCALE, -height * MASK_SCALE, poseStack, buffers, MASK_TEXTURE, light, overlay);
            poseStack.translate(0.1F, 0.11F, 0.3F);
            render(this.tank, 1.0F / 16.0F / MASK_SCALE, -height * MASK_SCALE, poseStack, buffers, TANK_TEXTURE, light, overlay);
            poseStack.translate(-0.2F, 0.0F, 0.0F);
            render(this.tank, 1.0F / 16.0F / MASK_SCALE, -height * MASK_SCALE, poseStack, buffers, TANK_TEXTURE, light, overlay);
            poseStack.popPose();
        }
        render(this.plinth, 1.0F / 16.0F, height, poseStack, buffers, BOX, light, overlay);
        // the upper flaps, north, west, south, east; then turned over, the lower ones
        final float[] upper = {north, west, south, east};
        final float[] lower = {west, north, east, south};
        for (float angle : upper) {
            flap(angle, poseStack, buffers, light, overlay);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        for (float angle : lower) {
            flap(angle, poseStack, buffers, light, overlay);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.popPose();
    }

    private void flap(float angle, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        this.flap.setPos(0.0F, 6.0F, -7.0F);
        this.flap.xRot = angle * net.minecraft.util.Mth.DEG_TO_RAD;
        this.flap.render(poseStack, buffers.getBuffer(RenderType.entityCutout(FLAP_TEXTURE)), light, overlay);
    }
}
