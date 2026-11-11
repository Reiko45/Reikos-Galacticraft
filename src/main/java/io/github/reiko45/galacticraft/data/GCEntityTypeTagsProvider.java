package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.registry.GCModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Entity type tags: the evolved mobs breathe on airless worlds (1.12.2 IEntityBreathable). */
public class GCEntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
    public GCEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, Registries.ENTITY_TYPE, lookup, type -> type.builtInRegistryHolder().key(), GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(GCModTags.BREATHES_IN_SPACE).add(GCEntities.SKELETON_BOSS.get(), GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_SPIDER.get(),
                GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_ENDERMAN.get());
        tag(EntityTypeTags.SKELETONS).add(GCEntities.EVOLVED_SKELETON.get());
        tag(EntityTypeTags.ZOMBIES).add(GCEntities.EVOLVED_ZOMBIE.get());
        tag(EntityTypeTags.UNDEAD).add(GCEntities.EVOLVED_SKELETON.get(), GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.SKELETON_BOSS.get());
        tag(EntityTypeTags.ARTHROPOD).add(GCEntities.EVOLVED_SPIDER.get());
    }
}
