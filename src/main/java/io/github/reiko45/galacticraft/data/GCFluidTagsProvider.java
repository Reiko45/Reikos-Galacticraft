package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.fluid.GCFluidTags;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** c:oil and c:fuel, so other mods' oil works in the Refinery and ours in their machines. */
public class GCFluidTagsProvider extends FluidTagsProvider {
    public GCFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(GCFluidTags.OIL).add(GCFluids.OIL.get(), GCFluids.FLOWING_OIL.get());
        tag(GCFluidTags.FUEL).add(GCFluids.FUEL.get(), GCFluids.FLOWING_FUEL.get());
        tag(GCFluidTags.OXYGEN).add(GCFluids.OXYGEN.get());
    }
}
