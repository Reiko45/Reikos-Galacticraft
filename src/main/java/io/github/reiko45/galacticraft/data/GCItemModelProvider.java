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
            if (item instanceof net.minecraft.world.item.SpawnEggItem) {
                withExistingParent(holder.getId().getPath(), mcLoc("item/template_spawn_egg"));
            } else if (item instanceof io.github.reiko45.galacticraft.item.BuggyItem) {
                buggy(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.AstroMinerItem) {
                astroMiner(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.GrappleItem) {
                grapple(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.FlagItem) {
                flag(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.RocketItem) {
                rocket(holder.getId().getPath());
            } else if (item instanceof LiquidCanisterItem) {
                canister(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.DungeonLocatorItem) {
                dungeonLocator(holder.getId().getPath());
            } else if (item instanceof io.github.reiko45.galacticraft.item.InfiniteBatteryItem) {
                // 1.12.2 drew it with the battery's texture (and the enchantment glint)
                withExistingParent(holder.getId().getPath(), mcLoc("item/generated")).texture("layer0", modLoc("item/battery"));
            } else if (item instanceof io.github.reiko45.galacticraft.item.MeteorChunkItem) {
                // the porter's 1.12.2 lump models, cold and hot
                withExistingParent(holder.getId().getPath(), modLoc("item/meteor_chunk/cold")).override()
                        .predicate(io.github.reiko45.galacticraft.GalacticraftCore.id("hot"), 1.0F)
                        .model(getExistingFile(modLoc("item/meteor_chunk/hot"))).end();
            } else if (item instanceof TieredItem || item instanceof WrenchItem) {
                handheldItem(item);
            } else {
                basicItem(item);
            }
        }
    }

    /**
     * 1.12.2 TextureDungeonFinder's 32 needle frames, one model each under {@code item/dungeon_locator/}, picked by the
     * {@code galacticraftcore:angle} property as the compass picks its frames.
     */
    private void dungeonLocator(String name) {
        final int frames = 32;
        for (int i = 0; i < frames; i++) {
            final String frame = String.format(java.util.Locale.ROOT, "%02d", i);
            withExistingParent("item/" + name + "/" + frame, mcLoc("item/generated")).texture("layer0", modLoc("item/" + name + "/" + frame));
        }
        final var model = withExistingParent(name, modLoc("item/" + name + "/00"));
        for (int i = 1; i < frames; i++) {
            model.override().predicate(io.github.reiko45.galacticraft.GalacticraftCore.id("angle"), i / (float) frames)
                    .model(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(
                            modLoc(String.format(java.util.Locale.ROOT, "item/%s/%02d", name, i)))).end();
        }
    }

    /**
     * 1.12.2 ItemModelAstroMiner: the astro_miner_inv OBJ with its 1.12.2 matrices, decomposed into translation, rotation
     * and scale (the GUI one without its slow spin). Its first-person matrix (four times the size, 6.8 blocks up) can't
     * be expressed in a model file, so first person is a plainer view.
     */
    private void astroMiner(String name) {
        getBuilder(name).customLoader(net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder::begin)
                .modelLocation(modLoc("models/item/astro_miner.obj")).flipV(true).end()
                .texture("particle", modLoc("model/astro_miner_off")).transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).translation(-3.462F, 1.802F, 5.038F).scale(0.255F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(1.6F, 3.2F, 1.6F).scale(0.2F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0, 85.94F, 0).translation(3.043F, 2.4F, -2.19F).scale(0.3F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(-131.43F, 63.81F, 25.78F)
                .translation(-1.685F, -6.558F, -12.607F).scale(0.8F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(-131.43F, 63.81F, 25.78F)
                .translation(-1.685F, -6.558F, -12.607F).scale(0.8F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(1.0F, 4.0F, 0.0F).scale(0.4F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 45, 0).translation(1.0F, 4.0F, 0.0F).scale(0.4F).end()
                .end();
    }

    /** 1.12.2 ItemModelGrapple: the grapple OBJ (the transforms approximate the 1.12.2 matrices). */
    private void grapple(String name) {
        getBuilder(name).customLoader(net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder::begin)
                .modelLocation(modLoc("models/item/grapple.obj")).flipV(true).end()
                .texture("particle", modLoc("model/grapple")).transforms()
                // centred: the OBJ's middle (-0.29, 0, -0.13) brought to the slot's
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 135, 0).translation(-1.29F, 10.44F, -6.89F).scale(0.7F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0.0F, 3.0F, 0.0F).scale(0.35F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0, 90, 0).translation(1.0F, 1.0F, 0.0F).scale(0.6F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(0, 90, 0).translation(0.0F, 3.0F, 1.0F).scale(0.45F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(0, 90, 0).translation(0.0F, 3.0F, 1.0F).scale(0.45F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 120, 0).translation(1.0F, 4.0F, 0.0F).scale(0.7F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 120, 0).translation(1.0F, 4.0F, 0.0F).scale(0.7F).end()
                .end();
    }

    /**
     * 1.12.2 ItemModelFlag: the flag OBJ (a 4.1 block pole with the default green flag, textures/model/flag0), turned to
     * three quarters in the inventory and carried upright with the cloth out to the side (the transforms approximate the 1.12.2
     * matrices).
     */
    private void flag(String name) {
        getBuilder(name).customLoader(net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder::begin)
                .modelLocation(modLoc("models/item/flag.obj")).flipV(true).end()
                .texture("particle", modLoc("model/flag0")).transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(20, -45, 0).translation(-2.34F, -5.18F, -1.73F).scale(0.22F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(-0.84F, -3.04F, 0.96F).scale(0.12F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0, 180, 0).translation(1.41F, -4.96F, -1.6F).scale(0.2F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(90, 0, 0).translation(4.0F, -3.2F, -5.6F).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(90, 0, 0).translation(4.0F, -3.2F, -5.6F).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 20, 0).translation(-0.28F, -4.16F, -3.05F).scale(0.3F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 20, 0).translation(-0.28F, -4.16F, -3.05F).scale(0.3F).end()
                .end();
    }

    /**
     * 1.12.2 ItemModelBuggy: the four-wheeled buggy_inv OBJ, with 0-3 storage boxes picked by the "storage" item property
     * (the transforms approximate the 1.12.2 matrices).
     */
    private void buggy(String name) {
        final String[] cargo = {"CargoLeft", "CargoMid", "CargoRight"};
        ItemModelBuilder base = null;
        for (int storage = 3; storage >= 0; storage--) {
            final ItemModelBuilder model = getBuilder(storage == 0 ? name : "item/" + name + "/storage_" + storage);
            final var loader = model.customLoader(net.neoforged.neoforge.client.model.generators.loaders.ObjModelBuilder::begin)
                    .modelLocation(modLoc("models/item/buggy.obj")).flipV(true);
            for (int i = 0; i < cargo.length; i++) {
                loader.visibility(cargo[i], i < storage);
            }
            loader.end();
            model.texture("particle", modLoc("model/buggy_main")).transforms()
                    // centred in the slot (the OBJ's middle brought to the slot's)
                    .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 208, 0).translation(-0.72F, -3.66F, -1.5F).scale(0.11F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.08F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0, 180, 0).translation(0, -1.5F, 0).scale(0.12F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.09F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.09F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 225, 0).translation(0, 1, 0).scale(0.15F).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).translation(0, 1, 0).scale(0.15F).end()
                    .end();
            if (storage == 0) {
                base = model;
            }
        }
        for (int storage = 1; storage <= 3; storage++) {
            base.override().predicate(GalacticraftCore.id("storage"), storage).model(getExistingFile(modLoc("item/" + name + "/storage_" + storage))).end();
        }
    }

    /** Drawn by RocketItemRenderer with the rocket's entity model (which lays it across the slot itself in the GUI). */
    private void rocket(String name) {
        getBuilder(name).parent(new ModelFile.UncheckedModelFile("builtin/entity")).guiLight(net.minecraft.client.renderer.block.model.BlockModel.GuiLight.FRONT)
                .transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 2, 0).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).translation(0, 2, 1).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).translation(0, 2, 1).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).scale(0.5F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 45, 0).scale(0.5F).end()
                .end();
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
