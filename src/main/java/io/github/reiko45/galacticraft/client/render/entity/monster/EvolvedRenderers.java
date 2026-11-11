package io.github.reiko45.galacticraft.client.render.entity.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.reiko45.galacticraft.GalacticraftCore;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Renderers of the evolved mobs (1.12.2 RenderEvolved*): the vanilla renderers with the evolved meshes and the 1.12.2
 * textures. Zombies, skeletons, spiders and creepers are drawn 1.2x as large, as in 1.12.2.
 */
public final class EvolvedRenderers {
    private static final float SCALE = 1.2F;

    private EvolvedRenderers() {
    }

    private static ResourceLocation texture(String name) {
        return GalacticraftCore.id("textures/model/" + name + ".png");
    }

    public static class Zombies extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = texture("zombie");

        public Zombies(EntityRendererProvider.Context context) {
            super(context, EvolvedMobModels.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
        }

        @Override
        public ResourceLocation getTextureLocation(Zombie entity) {
            return TEXTURE;
        }

        @Override
        protected void scale(Zombie entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }

    public static class Skeletons extends SkeletonRenderer<AbstractSkeleton> {
        private static final ResourceLocation TEXTURE = texture("skeleton");

        public Skeletons(EntityRendererProvider.Context context) {
            super(context, EvolvedMobModels.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
            return TEXTURE;
        }

        @Override
        protected void scale(AbstractSkeleton entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }

    public static class Spiders extends SpiderRenderer<Spider> {
        private static final ResourceLocation TEXTURE = texture("spider");

        public Spiders(EntityRendererProvider.Context context) {
            super(context, EvolvedMobModels.SPIDER);
            this.layers.clear();
            // The 1.12.2 eye texture is white under its transparent pixels (1.12.2 alpha-tested them away); the additive
            // "eyes" type would light up the whole spider, so use a full-bright type that blends by alpha.
            this.addLayer(new Eyes<>(this, RenderType.entityTranslucentEmissive(texture("spider_eyes"))));
        }

        @Override
        public ResourceLocation getTextureLocation(Spider entity) {
            return TEXTURE;
        }

        @Override
        protected void scale(Spider entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }

    public static class Creepers extends CreeperRenderer {
        private static final ResourceLocation TEXTURE = texture("creeper");

        public Creepers(EntityRendererProvider.Context context) {
            super(context);
            this.model = new CreeperModel<>(context.bakeLayer(EvolvedMobModels.CREEPER));
            this.layers.clear();
            this.addLayer(new CreeperPowerLayer(this, context.getModelSet()));
        }

        @Override
        public ResourceLocation getTextureLocation(Creeper entity) {
            return TEXTURE;
        }

        @Override
        protected void scale(Creeper entity, PoseStack poseStack, float partialTick) {
            super.scale(entity, poseStack, partialTick);
            // 1.12.2 EntityEvolvedCreeper#setChildSize: babies (hatched from Creeper Eggs) are half size.
            final float scale = SCALE * entity.getAgeScale();
            poseStack.scale(scale, scale, scale);
        }
    }

    public static class Endermen extends EndermanRenderer {
        private static final ResourceLocation TEXTURE = texture("evolved_enderman");

        public Endermen(EntityRendererProvider.Context context) {
            super(context);
            this.model = new EndermanModel<>(context.bakeLayer(EvolvedMobModels.ENDERMAN));
            this.layers.clear();
            this.addLayer(new Eyes<>(this, RenderType.eyes(texture("evolved_enderman_eyes"))));
            this.addLayer(new CarriedBlockLayer(this, context.getBlockRenderDispatcher()));
        }

        @Override
        public ResourceLocation getTextureLocation(EnderMan entity) {
            return TEXTURE;
        }
    }

    /** Glowing eyes with the 1.12.2 eye textures, laid out for the evolved meshes. */
    private static final class Eyes<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
        private final RenderType renderType;

        Eyes(RenderLayerParent<T, M> parent, RenderType renderType) {
            super(parent);
            this.renderType = renderType;
        }

        @Override
        public RenderType renderType() {
            return this.renderType;
        }
    }
}
