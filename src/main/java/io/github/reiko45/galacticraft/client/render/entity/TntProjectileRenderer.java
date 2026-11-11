package io.github.reiko45.galacticraft.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.entity.TntProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.12.2 RenderProjectileTNT: a TNT block flashing white every other tick. (1.12.2 copied the primed TNT renderer's
 * offsets without its rotation, which drew the block one block off the entity; here it is centred on it.)
 */
public class TntProjectileRenderer extends EntityRenderer<TntProjectileEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public TntProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TntProjectileEntity tnt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        TntMinecartRenderer.renderWhiteSolidBlock(this.blockRenderer, Blocks.TNT.defaultBlockState(), poseStack, buffers, light, tnt.tickCount % 2 == 0);
        poseStack.popPose();
        super.render(tnt, entityYaw, partialTick, poseStack, buffers, light);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(TntProjectileEntity tnt) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
