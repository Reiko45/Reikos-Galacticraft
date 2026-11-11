package io.github.reiko45.galacticraft.client.render.entity;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SludgelingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.12.2 RenderSludgeling: shadow 0.3; it rolls right over when it dies. */
public class SludgelingRenderer extends MobRenderer<SludgelingEntity, SludgelingModel> {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/model/sludgeling.png");

    public SludgelingRenderer(EntityRendererProvider.Context context) {
        super(context, new SludgelingModel(context.bakeLayer(SludgelingModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(SludgelingEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(SludgelingEntity entity) {
        return 180.0F;
    }
}
