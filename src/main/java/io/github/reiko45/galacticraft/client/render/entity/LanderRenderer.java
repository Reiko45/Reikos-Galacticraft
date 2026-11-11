package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
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
import net.minecraft.util.Mth;
import io.github.reiko45.galacticraft.entity.LanderEntity;

/** 1.12.2 RenderLander + ModelLander (converted by tools/convert_java_model.py, 512x512 textures/model/lander.png). */
public class LanderRenderer extends EntityRenderer<LanderEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("lander"), "main");
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/lander.png");

    private final ModelPart model;

    public LanderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(LAYER);
        this.shadowRadius = 1.5F;
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bodymain1", CubeListBuilder.create().texOffs(125, 128).mirror().addBox(-24.0F, 0.0F, -24.0F, 48.0F, 12.0F, 48.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3926991F, 0.0F));
        root.addOrReplaceChild("bodymain2", CubeListBuilder.create().texOffs(125, 128).mirror().addBox(-24.0F, -0.1F, -24.0F, 48.0F, 12.0F, 48.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.3926991F, 0.0F));
        root.addOrReplaceChild("bodyaux1", CubeListBuilder.create().texOffs(125, 192).mirror().addBox(-13.0F, -5.0F, -34.0F, 26.0F, 15.0F, 34.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("bodyaux2", CubeListBuilder.create().texOffs(125, 192).mirror().addBox(-13.0F, -5.0F, -34.0F, 26.0F, 15.0F, 34.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("bodyaux3", CubeListBuilder.create().texOffs(125, 192).mirror().addBox(-13.0F, -5.0F, -34.0F, 26.0F, 15.0F, 34.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, -2.356194F, 0.0F));
        root.addOrReplaceChild("bodyaux4", CubeListBuilder.create().texOffs(125, 192).mirror().addBox(-13.0F, -5.0F, -34.0F, 26.0F, 15.0F, 34.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 2.356194F, 0.0F));
        root.addOrReplaceChild("bodysupportmain1", CubeListBuilder.create().texOffs(125, 243).mirror().addBox(-19.0F, -7.0F, -19.0F, 38.0F, 10.0F, 38.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bodysupportmain2", CubeListBuilder.create().texOffs(125, 293).mirror().addBox(-12.0F, -15.0F, -12.0F, 24.0F, 15.0F, 24.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("legstructureaux1e", CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-26.0F, 28.0F, -2.0F, 3.0F, 24.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.7330383F));
        root.addOrReplaceChild("footbaseg", CubeListBuilder.create().texOffs(125, 81).mirror().addBox(-64.0F, 18.9F, -2.0F, 11.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.0F));
        root.addOrReplaceChild("legstructure1i", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-27.0F, 33.0F, -3.0F, 5.0F, 14.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.7330383F));
        root.addOrReplaceChild("legbase1j", CubeListBuilder.create().texOffs(16, 81).mirror().addBox(-29.0F, 20.0F, -5.0F, 9.0F, 10.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.7330383F));
        root.addOrReplaceChild("footjointk", CubeListBuilder.create().texOffs(64, 104).mirror().addBox(-58.0F, 20.0F, -6.5F, 7.0F, 5.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.0F));
        root.addOrReplaceChild("footbasel", CubeListBuilder.create().texOffs(64, 81).mirror().addBox(-62.0F, 23.0F, -8.5F, 10.0F, 2.0F, 15.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.356194F, 0.0F));
        root.addOrReplaceChild("legbase1n", CubeListBuilder.create().texOffs(16, 81).mirror().addBox(-29.0F, 20.0F, -5.0F, 9.0F, 10.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.7330383F));
        root.addOrReplaceChild("legstructureaux1s", CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-26.0F, 28.0F, -2.0F, 3.0F, 24.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.7330383F));
        root.addOrReplaceChild("legstructure1u", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-27.0F, 33.0F, -3.0F, 5.0F, 14.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.7330383F));
        root.addOrReplaceChild("footjointv", CubeListBuilder.create().texOffs(64, 104).mirror().addBox(-58.0F, 20.0F, -6.5F, 7.0F, 5.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("footbasew", CubeListBuilder.create().texOffs(64, 81).mirror().addBox(-62.0F, 23.0F, -8.5F, 10.0F, 2.0F, 15.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("footbasex", CubeListBuilder.create().texOffs(125, 81).mirror().addBox(-64.0F, 18.9F, -2.0F, 11.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("legbase1z", CubeListBuilder.create().texOffs(16, 81).mirror().addBox(-29.0F, 20.0F, -5.0F, 9.0F, 10.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.7330383F));
        root.addOrReplaceChild("legstructureaux1f", CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-26.0F, 28.0F, -2.0F, 3.0F, 24.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.7330383F));
        root.addOrReplaceChild("legstructure1g", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-27.0F, 33.0F, -3.0F, 5.0F, 14.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.7330383F));
        root.addOrReplaceChild("footjointh", CubeListBuilder.create().texOffs(64, 104).mirror().addBox(-58.0F, 20.0F, -6.5F, 7.0F, 5.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("footbasei", CubeListBuilder.create().texOffs(64, 81).mirror().addBox(-62.0F, 23.0F, -8.5F, 10.0F, 2.0F, 15.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("footbasej", CubeListBuilder.create().texOffs(125, 81).mirror().addBox(-64.0F, 18.9F, -2.0F, 11.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("legbase1l", CubeListBuilder.create().texOffs(16, 81).mirror().addBox(-29.0F, 20.0F, -5.0F, 9.0F, 10.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.7330383F));
        root.addOrReplaceChild("legstructureaux1q", CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-26.0F, 28.0F, -2.0F, 3.0F, 24.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.7330383F));
        root.addOrReplaceChild("legstructure1s", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-27.0F, 33.0F, -3.0F, 5.0F, 14.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.7330383F));
        root.addOrReplaceChild("footjointt", CubeListBuilder.create().texOffs(64, 104).mirror().addBox(-58.0F, 20.0F, -6.5F, 7.0F, 5.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.0F));
        root.addOrReplaceChild("footbaseu", CubeListBuilder.create().texOffs(64, 81).mirror().addBox(-62.0F, 23.0F, -8.5F, 10.0F, 2.0F, 15.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.0F));
        root.addOrReplaceChild("footbasev", CubeListBuilder.create().texOffs(125, 81).mirror().addBox(-64.0F, 18.9F, -2.0F, 11.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.356194F, 0.0F));
        root.addOrReplaceChild("modulemain1", CubeListBuilder.create().texOffs(289, 166).mirror().addBox(-6.0F, -44.0F, -16.0F, 32.0F, 32.0F, 32.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulemain2", CubeListBuilder.create().texOffs(249, 234).mirror().addBox(-16.0F, -30.0F, -13.0F, 32.0F, 16.0F, 26.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulemaindeco", CubeListBuilder.create().texOffs(70, 300).mirror().addBox(16.0F, -46.0F, -20.9F, 5.0F, 30.0F, 37.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulecore1", CubeListBuilder.create().texOffs(197, 293).mirror().addBox(-20.0F, -48.0F, -25.0F, 5.0F, 36.0F, 42.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulecore2", CubeListBuilder.create().texOffs(197, 293).mirror().addBox(-30.0F, -48.0F, -25.0F, 5.0F, 36.0F, 42.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulecore3", CubeListBuilder.create().texOffs(295, 300).mirror().addBox(-25.0F, -44.0F, -21.0F, 5.0F, 28.0F, 34.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux1", CubeListBuilder.create().texOffs(85, 238).mirror().addBox(26.0F, -34.0F, -13.0F, 4.0F, 16.0F, 26.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux2", CubeListBuilder.create().texOffs(0, 110).mirror().addBox(26.1F, -60.0F, 9.0F, 1.0F, 28.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux3", CubeListBuilder.create().texOffs(17, 110).mirror().addBox(27.0F, -57.0F, -12.0F, 1.0F, 23.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux4", CubeListBuilder.create().texOffs(5, 110).mirror().addBox(28.0F, -48.0F, -8.0F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux5", CubeListBuilder.create().texOffs(5, 110).mirror().addBox(28.0F, -48.0F, -6.0F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux6", CubeListBuilder.create().texOffs(5, 110).mirror().addBox(28.0F, -48.0F, -4.0F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux7", CubeListBuilder.create().texOffs(11, 110).mirror().addBox(27.0F, -53.0F, 2.0F, 1.0F, 19.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("moduleaux8", CubeListBuilder.create().texOffs(0, 110).mirror().addBox(28.9F, -60.0F, 9.0F, 1.0F, 28.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulethrusterbase1", CubeListBuilder.create().texOffs(21, 19).mirror().addBox(30.0F, -28.0F, -5.0F, 3.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("modulethrusterbase2", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(5.0F, -39.0F, -5.0F, 3.0F, 5.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        root.addOrReplaceChild("modulethrusterbase3", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(-43.0F, -4.0F, -5.0F, 3.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.356194F));
        root.addOrReplaceChild("modulethrusterbase4", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(24.0F, -28.0F, 13.7F, 3.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterbase5", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(24.0F, -28.0F, -19.5F, 3.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconetop1", CubeListBuilder.create().texOffs(40, 9).mirror().addBox(-44.0F, -3.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.356194F));
        root.addOrReplaceChild("modulethrusterconetop2", CubeListBuilder.create().texOffs(30, 9).mirror().addBox(-45.0F, -3.5F, -1.5F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.356194F));
        root.addOrReplaceChild("modulethrusterconetop3", CubeListBuilder.create().texOffs(29, 0).mirror().addBox(-46.0F, -4.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.356194F));
        root.addOrReplaceChild("modulethrusterconeback1", CubeListBuilder.create().texOffs(40, 9).mirror().addBox(27.0F, -26.0F, -18.5F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconeback2", CubeListBuilder.create().texOffs(30, 9).mirror().addBox(28.0F, -26.5F, -19.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconeback3", CubeListBuilder.create().texOffs(29, 0).mirror().addBox(29.0F, -27.0F, -19.5F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconebot1", CubeListBuilder.create().texOffs(40, 9).mirror().addBox(8.0F, -38.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        root.addOrReplaceChild("modulethrusterconebot2", CubeListBuilder.create().texOffs(30, 9).mirror().addBox(9.0F, -38.5F, -1.5F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        root.addOrReplaceChild("modulethrusterconebot3", CubeListBuilder.create().texOffs(29, 0).mirror().addBox(10.0F, -39.0F, -2.0F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        root.addOrReplaceChild("modulethrusterconefront1", CubeListBuilder.create().texOffs(40, 9).mirror().addBox(27.0F, -26.0F, 16.5F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconefront2", CubeListBuilder.create().texOffs(30, 9).mirror().addBox(28.0F, -26.5F, 16.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        root.addOrReplaceChild("modulethrusterconefront3", CubeListBuilder.create().texOffs(29, 0).mirror().addBox(29.0F, -27.0F, 15.5F, 1.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F, 0.0F));
        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void render(LanderEntity lander, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        final float pitch = Mth.lerp(partialTick, lander.xRotO, lander.getXRot());
        poseStack.translate(0.0F, 1.55F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(lander, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LanderEntity lander) {
        return TEXTURE;
    }
}
