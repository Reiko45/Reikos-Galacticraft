package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.TreasureChestBlock;
import io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.12.2 TileEntityTreasureChestRenderer + ModelTreasureChest: the 1.12.2 chest model (lid, knob, body) with a lock
 * plate, and the inserted key while it is unlocked. The lid opens to 45 degrees.
 */
public class TreasureChestRenderer implements BlockEntityRenderer<TreasureChestBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GalacticraftCore.id("treasure_chest"), "main");
    public static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/treasure.png");
    /** The Tier 2 (Mars) chest's own texture (planets textures/model/treasure.png). */
    public static final ResourceLocation TEXTURE_TIER_2 = GalacticraftCore.id("textures/model/treasure_tier_2.png");

    /** The Tier 3 (Venus) chest's (planets textures/model/treasure_venus.png). */
    public static final ResourceLocation TEXTURE_TIER_3 = GalacticraftCore.id("textures/model/treasure_tier_3.png");

    public static ResourceLocation texture(int tier) {
        return tier >= 3 ? TEXTURE_TIER_3 : tier >= 2 ? TEXTURE_TIER_2 : TEXTURE;
    }
    private static final String[] KEY_PARTS = {"key0", "key1", "key2", "key3", "key4"};
    private final ModelPart model;

    public TreasureChestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = context.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        // 1.12.2 ModelChest
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(1.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 0.0F, 14.0F, 10.0F, 14.0F), PartPose.offset(1.0F, 6.0F, 1.0F));
        // ModelTreasureChest keyParts[5] (the lock) and keyParts[0..4] (the key)
        root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.05F, -15.1F, 4.0F, 4.0F, 1.0F), PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("key0", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-4.5F, -1.0F, -0.5F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("key1", CubeListBuilder.create().texOffs(9, 43).mirror().addBox(-1.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("key2", CubeListBuilder.create().texOffs(14, 43).mirror().addBox(-0.5F, 0.0F, -0.5F, 11.0F, 1.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("key3", CubeListBuilder.create().texOffs(39, 43).mirror().addBox(6.0F, 1.0F, -0.5F, 4.0F, 1.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("key4", CubeListBuilder.create().texOffs(50, 43).mirror().addBox(7.0F, 2.0F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(TreasureChestBlockEntity chest, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final Direction facing = chest.getBlockState().hasProperty(TreasureChestBlock.FACING) ? chest.getBlockState().getValue(TreasureChestBlock.FACING) : Direction.SOUTH;
        draw(this.model, poseStack, buffers, light, overlay, facing, chest.getOpenNess(partialTick), !chest.isLocked(), chest.tier());
    }

    /** 1.12.2 render(): flip into model space, turn to face the front, open the lid, draw. */
    static void draw(ModelPart model, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, Direction facing, float openness,
                     boolean withKey, int tier) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 1.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        final float angle = switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float open = 1.0F - openness;
        open = 1.0F - open * open * open;
        final float lidAngle = -(open * Mth.PI / 4.0F);
        final ModelPart lid = model.getChild("lid");
        lid.xRot = lidAngle;
        model.getChild("knob").xRot = lidAngle;

        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(texture(tier)));
        for (String key : KEY_PARTS) {
            final ModelPart part = model.getChild(key);
            part.visible = withKey;
            part.setPos(8.0F, 7.0F, -2.0F);
            part.yRot = 3.0F * Mth.HALF_PI;
            part.xRot = -lidAngle;
        }
        model.render(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    /** The item: a closed, locked chest facing the viewer like the vanilla chest item. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        private ModelPart model;

        public Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
            this.model = null;
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            if (this.model == null) {
                this.model = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
            }
            final int tier = stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                    && block.getBlock() instanceof io.github.reiko45.galacticraft.block.TreasureChestBlock chest ? chest.tier() : 1;
            draw(this.model, poseStack, buffers, light, overlay, Direction.NORTH, 0.0F, false, tier);
        }
    }
}
