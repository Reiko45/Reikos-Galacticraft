package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCDamageTypes;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Oxygen suffocation ignores armour, protection enchantments and Resistance (1.12.2 setDamageBypassesArmor + setDamageIsAbsolute). */
public class GCDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public GCDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, Registries.DAMAGE_TYPE, lookup, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.BYPASSES_ARMOR).add(GCDamageTypes.OXYGEN_SUFFOCATION, GCDamageTypes.SPACESHIP_CRASH, GCDamageTypes.THERMAL);
        // 1.12.2 DamageSourceGC.spaceshipCrash: setDamageBypassesArmor().setExplosion()
        tag(DamageTypeTags.IS_EXPLOSION).add(GCDamageTypes.SPACESHIP_CRASH);
        // setDamageIsAbsolute: no enchantment or Resistance protects
        tag(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(GCDamageTypes.OXYGEN_SUFFOCATION, GCDamageTypes.THERMAL);
        tag(DamageTypeTags.BYPASSES_RESISTANCE).add(GCDamageTypes.OXYGEN_SUFFOCATION, GCDamageTypes.THERMAL);
    }
}
