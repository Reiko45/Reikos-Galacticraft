package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Models for non-block items (block item models come from {@link GCBlockStateProvider}). */
public class GCItemModelProvider extends ItemModelProvider {
    public GCItemModelProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void registerModels() {
        for (DeferredHolder<Item, ? extends Item> holder : GCItems.ITEMS.getEntries()) {
            final Item item = holder.get();
            if (item instanceof BlockItem) {
                continue;
            }
            if (item instanceof TieredItem) {
                handheldItem(item);
            } else {
                basicItem(item);
            }
        }
    }
}
