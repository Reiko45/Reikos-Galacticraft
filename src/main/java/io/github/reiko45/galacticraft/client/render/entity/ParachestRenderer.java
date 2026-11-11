package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
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

/**
 * 1.12.2 RenderParaChest + ModelParaChest: the 1.12.2 chest model turned upside down (its parts rotated by 180 degrees
 * about X, with no entity flip) under three canopy panels and four strings, in the parachute's colour.
 */
public class ParachestRenderer extends EntityRenderer<ParachestEntity> {
    public static final ModelLayerLocation CHEST = new ModelLayerLocation(GalacticraftCore.id("parachest"), "chest");
    public static final ModelLayerLocation CHUTE = new ModelLayerLocation(GalacticraftCore.id("parachest"), "chute");
    private static final ResourceLocation CHEST_TEXTURE = GalacticraftCore.id("textures/model/parachest.png");
    private static final ResourceLocation[] CHUTE_TEXTURES = new ResourceLocation[ParachuteItem.LEGACY_COLORS.length];

    static {
        for (int i = 0; i < CHUTE_TEXTURES.length; i++) {
            CHUTE_TEXTURES[i] = GalacticraftCore.id("textures/model/parachute/" + ParachuteItem.LEGACY_COLORS[i] + ".png");
        }
    }

    private final ModelPart chest;
    private final ModelPart chute;

    public ParachestRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.chest = context.bakeLayer(CHEST);
        this.chute = context.bakeLayer(CHUTE);
    }

    /** 1.12.2 ModelChest boxes at ModelParaChest#renderAll's rotation points. */
    public static LayerDefinition createChest() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14, 5, 14),
                PartPose.offsetAndRotation(2.0F, 7.0F, -6.0F, Mth.PI, 0.0F, 0.0F));
        root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2, 4, 1),
                PartPose.offsetAndRotation(9.0F, 7.0F, -6.0F, Mth.PI, 0.0F, 0.0F));
        root.addOrReplaceChild("below", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 0.0F, 14, 10, 14),
                PartPose.offsetAndRotation(2.0F, 8.0F, 8.0F, Mth.PI, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** ModelParaChest parachute and strings (256x256 parachute texture). */
    public static LayerDefinition createChute() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final float deg = Mth.DEG_TO_RAD;
        root.addOrReplaceChild("canopy_0", CubeListBuilder.create().texOffs(0, 0).addBox(-20.0F, -45.0F, -20.0F, 10, 2, 40),
                PartPose.offsetAndRotation(-5.85F, -11.0F, 2.0F, 0.0F, 0.0F, 210.0F * deg));
        root.addOrReplaceChild("canopy_1", CubeListBuilder.create().texOffs(0, 42).addBox(-20.0F, -45.0F, -20.0F, 40, 2, 40),
                PartPose.offsetAndRotation(9.0F, -7.0F, 2.0F, 0.0F, 0.0F, 180.0F * deg));
        root.addOrReplaceChild("canopy_2", CubeListBuilder.create().texOffs(0, 0).addBox(-20.0F, -45.0F, -20.0F, 10, 2, 40),
                PartPose.offsetAndRotation(-2.15F, 4.0F, 2.0F, 0.0F, 0.0F, -210.0F * deg));
        final float[][] strings = {{23.0F, 335.0F}, {-23.0F, 335.0F}, {23.0F, -335.0F}, {-23.0F, -335.0F}};
        for (int i = 0; i < strings.length; i++) {
            root.addOrReplaceChild("string_" + i, CubeListBuilder.create().texOffs(100, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 40, 1),
                    PartPose.offsetAndRotation(9.0F, 3.0F, 2.0F, strings[i][0] * deg, 0.0F, strings[i][1] * deg));
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void render(ParachestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        final int color = Math.floorMod(entity.color(), CHUTE_TEXTURES.length);
        this.chute.render(poseStack, buffers.getBuffer(RenderType.entityCutout(CHUTE_TEXTURES[color])), light, OverlayTexture.NO_OVERLAY);
        this.chest.render(poseStack, buffers.getBuffer(RenderType.entityCutout(CHEST_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ParachestEntity entity) {
        return CHEST_TEXTURE;
    }
}
