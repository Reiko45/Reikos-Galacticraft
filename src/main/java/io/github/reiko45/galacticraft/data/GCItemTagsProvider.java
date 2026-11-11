package io.github.reiko45.galacticraft.data;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item tags. The {@code c:} tags replace the 1.12.2 ore dictionary names used by recipes
 * (e.g. {@code compressedSteel} -> {@code c:plates/steel}, see tools/legacy_mapping.json).
 */
public class GCItemTagsProvider extends ItemTagsProvider {
    public GCItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookup, blockTags, GalacticraftCore.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(BlockTags.STAIRS, ItemTags.STAIRS);
        copy(BlockTags.SLABS, ItemTags.SLABS);
        copy(BlockTags.WALLS, ItemTags.WALLS);
        for (String ore : new String[]{"tin", "aluminum", "silicon", "sapphire", "cheese", "desh"}) {
            copy(GCTags.block("ores/" + ore), GCTags.item("ores/" + ore));
        }
        copy(Tags.Blocks.ORES_COPPER, Tags.Items.ORES_COPPER);
        copy(Tags.Blocks.ORES_IRON, Tags.Items.ORES_IRON);
        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        copy(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
        for (String metal : new String[]{"tin", "aluminum", "meteoric_iron", "silicon", "desh"}) {
            copy(GCTags.block("storage_blocks/" + metal), GCTags.item("storage_blocks/" + metal));
        }
        copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS);

        tag(GCTags.item("ingots/tin")).add(GCItems.TIN_INGOT.get());
        tag(GCTags.item("ingots/aluminum")).add(GCItems.ALUMINUM_INGOT.get());
        tag(GCTags.item("ingots/meteoric_iron")).add(GCItems.METEORIC_IRON_INGOT.get());
        tag(GCTags.item("ingots/desh")).add(GCItems.DESH_INGOT.get());
        tag(Tags.Items.INGOTS).addTags(GCTags.item("ingots/tin"), GCTags.item("ingots/aluminum"), GCTags.item("ingots/meteoric_iron"),
                GCTags.item("ingots/desh"));

        tag(GCTags.item("raw_materials/meteoric_iron")).add(GCItems.METEORIC_IRON_RAW.get());
        tag(GCTags.item("raw_materials/desh")).add(GCItems.RAW_DESH.get());
        tag(Tags.Items.RAW_MATERIALS).addTags(GCTags.item("raw_materials/meteoric_iron"), GCTags.item("raw_materials/desh"));
        tag(GCTags.item("rods/desh")).add(GCItems.DESH_STICK.get());
        tag(Tags.Items.RODS).addTag(GCTags.item("rods/desh"));
        tag(GCTags.item("gems/sapphire")).add(GCItems.LUNAR_SAPPHIRE.get());
        tag(Tags.Items.GEMS).addTag(GCTags.item("gems/sapphire"));
        tag(GCTags.item("silicon")).add(GCItems.RAW_SILICON.get());

        tag(GCTags.item("plates/copper")).add(GCItems.COMPRESSED_COPPER.get());
        tag(GCTags.item("plates/tin")).add(GCItems.COMPRESSED_TIN.get());
        tag(GCTags.item("plates/aluminum")).add(GCItems.COMPRESSED_ALUMINUM.get());
        tag(GCTags.item("plates/steel")).add(GCItems.COMPRESSED_STEEL.get());
        tag(GCTags.item("plates/bronze")).add(GCItems.COMPRESSED_BRONZE.get());
        tag(GCTags.item("plates/iron")).add(GCItems.COMPRESSED_IRON.get());
        tag(GCTags.item("plates/meteoric_iron")).add(GCItems.COMPRESSED_METEORIC_IRON.get());
        tag(GCTags.item("plates/desh")).add(GCItems.COMPRESSED_DESH.get());
        tag(GCTags.item("plates")).addTags(GCTags.item("plates/copper"), GCTags.item("plates/tin"), GCTags.item("plates/aluminum"),
                GCTags.item("plates/steel"), GCTags.item("plates/bronze"), GCTags.item("plates/iron"), GCTags.item("plates/meteoric_iron"),
                GCTags.item("plates/desh"));

        // Vanilla tool/armor tags drive enchantability since 1.21.
        tag(ItemTags.PICKAXES).add(GCItems.HEAVY_DUTY_PICKAXE.get(), GCItems.DESH_PICKAXE.get(), GCItems.STICKY_DESH_PICKAXE.get());
        tag(ItemTags.AXES).add(GCItems.HEAVY_DUTY_AXE.get(), GCItems.DESH_AXE.get());
        tag(ItemTags.SHOVELS).add(GCItems.HEAVY_DUTY_SHOVEL.get(), GCItems.DESH_SHOVEL.get());
        tag(ItemTags.HOES).add(GCItems.HEAVY_DUTY_HOE.get(), GCItems.DESH_HOE.get());
        tag(ItemTags.SWORDS).add(GCItems.HEAVY_DUTY_SWORD.get(), GCItems.DESH_SWORD.get());
        tag(ItemTags.HEAD_ARMOR).add(GCItems.HEAVY_DUTY_HELMET.get(), GCItems.DESH_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(GCItems.HEAVY_DUTY_CHESTPLATE.get(), GCItems.DESH_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(GCItems.HEAVY_DUTY_LEGGINGS.get(), GCItems.DESH_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(GCItems.HEAVY_DUTY_BOOTS.get(), GCItems.DESH_BOOTS.get());
    }
}
