package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.WrenchItem;
import io.github.reiko45.galacticraft.registry.GCItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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
            if (item instanceof LiquidCanisterItem) {
                canister(holder.getId().getPath());
            } else if (item instanceof TieredItem || item instanceof WrenchItem) {
                handheldItem(item);
            } else {
                basicItem(item);
            }
        }
    }

    /** 1.12.2 PartialCanister: 7 textures, _partial_0 empty to _partial_6 full, picked by the "fill" item property. */
    private void canister(String name) {
        final ItemModelBuilder base = withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("item/" + name + "_partial_0"));
        for (int stage = 1; stage <= 6; stage++) {
            final ModelFile model = withExistingParent("item/" + name + "/stage_" + stage, mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/" + name + "_partial_" + stage));
            base.override().predicate(GalacticraftCore.id("fill"), stage).model(model).end();
        }
    }
}
