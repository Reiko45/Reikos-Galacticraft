package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.client.ClientPayloads;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import io.github.reiko45.galacticraft.player.GearData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.resources.ResourceLocation;

/**
 * Oxygen gear on players (1.12.2 LayerOxygenMask, LayerOxygenTanks, LayerOxygenGear, LayerOxygenParachute): a glass
 * helmet over the head, two tanks on the back (green light, orange medium, red heavy/infinite), the tubes running up to
 * the mask, an open parachute in the worn parachute's colour, and Thermal Padding (1.12.2 LayerThermalPadding).
 * Positions are the 1.12.2 values after its GL translations, in model units relative to the body.
 */
public class GearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation MASK = new ModelLayerLocation(GalacticraftCore.id("oxygen_mask"), "main");
    public static final ModelLayerLocation GEAR = new ModelLayerLocation(GalacticraftCore.id("oxygen_gear"), "main");
    public static final ModelLayerLocation PARACHUTE = new ModelLayerLocation(GalacticraftCore.id("parachute"), "main");
    private static final ResourceLocation MASK_TEXTURE = GalacticraftCore.id("textures/model/oxygen.png");
    private static final ResourceLocation GEAR_TEXTURE = GalacticraftCore.id("textures/model/player.png");

    private final ModelPart mask;
    private final ModelPart[] tanks;
    private final ModelPart tube;
    private final ModelPart parachute;
    /** 1.12.2 LayerThermalPadding#initArmor: ModelPlayerGC(0.55) for the head, ModelPlayerGC(0.05) for the rest. */
    public static final ModelLayerLocation THERMAL_HEAD = new ModelLayerLocation(GalacticraftCore.id("thermal_padding"), "head");
    public static final ModelLayerLocation THERMAL_BODY = new ModelLayerLocation(GalacticraftCore.id("thermal_padding"), "body");
    private static final ResourceLocation THERMAL_TEXTURE = GalacticraftCore.id("textures/misc/thermal_padding_1.png");
    /** 1.12.2 RenderPlayerGC.thermalPaddingTexture1_T2: Isothermal Padding. */
    private static final ResourceLocation THERMAL_TEXTURE_T2 = GalacticraftCore.id("textures/misc/thermal_padding_t2_1.png");
    private static final ResourceLocation THERMAL_GLOW = GalacticraftCore.id("textures/misc/thermal_padding_0.png");
    private final PlayerModel<AbstractClientPlayer> thermalHead;
    private final PlayerModel<AbstractClientPlayer> thermalBody;

    public GearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.mask = models.bakeLayer(MASK).getChild("mask");
        final ModelPart gear = models.bakeLayer(GEAR);
        this.tanks = new ModelPart[]{gear.getChild("tank_light"), gear.getChild("tank_medium"), gear.getChild("tank_heavy")};
        this.tube = gear.getChild("tube");
        this.parachute = models.bakeLayer(PARACHUTE);
        this.thermalHead = new PlayerModel<>(models.bakeLayer(THERMAL_HEAD), false);
        this.thermalBody = new PlayerModel<>(models.bakeLayer(THERMAL_BODY), false);
    }

    public static LayerDefinition createThermalHead() {
        return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.55F), false), 64, 64);
    }

    public static LayerDefinition createThermalBody() {
        return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.05F), false), 64, 64);
    }

    /**
     * 1.12.2 LayerThermalPadding: each piece over its part of the body, then the glow texture over it pulsing every
     * ~6 seconds: green on worlds with no temperature, red on cold ones, blue on hot ones.
     */
    private void renderThermal(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, int pieces) {
        final float pulse = (float) Math.sin(player.tickCount / 10.0F) * 0.5F + 0.5F;
        float r = 0.2F * pulse;
        float g = 1.0F * pulse;
        float b = 0.2F * pulse;
        final float modifier = io.github.reiko45.galacticraft.world.Atmosphere.thermalLevel(player.level().dimension());
        if (modifier > 0.0F) {
            b = g;
            g = r;
        } else if (modifier < 0.0F) {
            r = g;
            g = b;
        }
        final int glow = net.minecraft.util.FastColor.ARGB32.colorFromFloat(0.4F * pulse, r, g, b);
        for (int piece = 0; piece < 4; piece++) {
            if ((pieces & 1 << piece) == 0) {
                continue;
            }
            final PlayerModel<AbstractClientPlayer> model = piece == 0 ? this.thermalHead : this.thermalBody;
            getParentModel().copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (piece) {
                case 0 -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case 1 -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                default -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
            }
            final ResourceLocation texture = (pieces & 1 << (piece + 4)) != 0 ? THERMAL_TEXTURE_T2 : THERMAL_TEXTURE;
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(THERMAL_GLOW)), light, OverlayTexture.NO_OVERLAY, glow);
        }
    }

    /** LayerOxygenParachute: three canopy panels and four strings, 256x256 parachute texture. */
    public static LayerDefinition createParachute() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final float deg = (float) (Math.PI / 180.0D);
        root.addOrReplaceChild("canopy_0", CubeListBuilder.create().texOffs(0, 0).addBox(-20.0F, -45.0F, -20.0F, 10, 2, 40),
                PartPose.offsetAndRotation(15.0F, 4.0F, 0.0F, 0.0F, 0.0F, 30.0F * deg));
        root.addOrReplaceChild("canopy_1", CubeListBuilder.create().texOffs(0, 42).addBox(-20.0F, -45.0F, -20.0F, 40, 2, 40), PartPose.ZERO);
        root.addOrReplaceChild("canopy_2", CubeListBuilder.create().texOffs(0, 0).addBox(-20.0F, -45.0F, -20.0F, 10, 2, 40),
                PartPose.offsetAndRotation(11.0F, -11.0F, 0.0F, 0.0F, 0.0F, -30.0F * deg));
        final float[][] strings = {{-9.0F, 23.0F, 155.0F}, {-9.0F, -23.0F, 155.0F}, {9.0F, 23.0F, -155.0F}, {9.0F, -23.0F, -155.0F}};
        for (int i = 0; i < strings.length; i++) {
            root.addOrReplaceChild("string_" + i, CubeListBuilder.create().texOffs(100, 0).addBox(-0.5F, 0.0F, -0.5F, 1, 40, 1),
                    PartPose.offsetAndRotation(strings[i][0], -7.0F, 2.0F, strings[i][1] * deg, 0.0F, strings[i][2] * deg));
        }
        return LayerDefinition.create(mesh, 256, 256);
    }

    public static LayerDefinition createMask() {
        final MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("mask", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition createGear() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tank_light", CubeListBuilder.create().texOffs(4, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tank_medium", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tank_heavy", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(-1.5F, 0.0F, -1.5F, 3, 7, 3), PartPose.ZERO);
        root.addOrReplaceChild("tube", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        final GearData gear = ClientPayloads.gear(player.getId());
        if (gear.equals(GearData.NONE)) {
            return;
        }
        final PlayerModel<AbstractClientPlayer> model = getParentModel();
        if (gear.thermalPadding() != 0) {
            renderThermal(poseStack, buffers, light, player, gear.thermalPadding());
        }
        if (gear.parachuteOpen() && gear.parachute() >= 0) {
            final ResourceLocation texture = GalacticraftCore.id("textures/model/parachute/"
                    + ParachuteItem.LEGACY_COLORS[Math.min(gear.parachute(), ParachuteItem.LEGACY_COLORS.length - 1)] + ".png");
            this.parachute.render(poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, OverlayTexture.NO_OVERLAY);
        }
        if (gear.mask()) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            poseStack.scale(0.525F, 0.525F, 0.525F);
            this.mask.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(MASK_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(GEAR_TEXTURE));
        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        drawTank(poseStack, consumer, light, gear.leftTank(), 2.8F);
        drawTank(poseStack, consumer, light, gear.rightTank(), -2.8F);
        if (gear.gear() && !model.crouching) {
            for (int i = 0; i < 7; i++) {
                final float step = i * 2 - 1;
                final float y = -0.52F * step + 3.2F;
                final float z = -0.026F * step * step + 3.2F;
                for (int side = 0; side < 2; side++) {
                    poseStack.pushPose();
                    poseStack.translate((side == 0 ? -2.8F : 2.8F) / 16.0F, y / 16.0F, z / 16.0F);
                    this.tube.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }
        }
        poseStack.popPose();
    }

    private void drawTank(PoseStack poseStack, VertexConsumer consumer, int light, int tier, float x) {
        if (tier <= 0) {
            return;
        }
        poseStack.pushPose();
        // 1.12.2: rotation point (0, 0.5, 0.5) + translate (0.175, 0.2, 0.2); lower and closer when sneaking
        poseStack.translate(x / 16.0F, 3.7F / 16.0F, 3.7F / 16.0F);
        this.tanks[Math.min(tier, 3) - 1].render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
