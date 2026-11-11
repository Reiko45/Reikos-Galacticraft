package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Same split as 1.12.2: one tab for blocks, one for items. */
public final class GCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GalacticraftCore.MOD_ID);

    public static final Supplier<CreativeModeTab> BLOCKS = TABS.register("blocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.galacticraftcore.blocks"))
            .icon(() -> new ItemStack(GCBlocks.MOON_TURF.get()))
            .displayItems((params, output) -> GCItems.ITEMS.getEntries().stream()
                    .map(holder -> holder.get())
                    .filter(item -> item instanceof BlockItem && item != GCBlocks.UNLIT_TORCH_ITEM.get()) // 1.12.2 hid space torches
                    .forEach(output::accept))
            .build());

    public static final Supplier<CreativeModeTab> ITEMS = TABS.register("items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.galacticraftcore.items"))
            .icon(() -> new ItemStack(GCItems.CANNED_BEEF.get()))
            .withTabsBefore(GalacticraftCore.id("blocks"))
            .displayItems((params, output) -> GCItems.ITEMS.getEntries().stream()
                    .map(holder -> holder.get())
                    .filter(item -> !(item instanceof BlockItem))
                    .forEach(item -> {
                        if (item != GCItems.FUEL_CANISTER.get()) {
                            output.accept(item); // an empty fuel canister is an empty (oil) canister
                        }
                        if (item instanceof ElectricItem electric) {
                            output.accept(electric.charged());
                        }
                        if (item instanceof OxygenTankItem tank) {
                            output.accept(tank.full()); // 1.12.2 listed every tank empty and full
                        }
                        if (item == GCItems.TIER_1_ROCKET.get()) {
                            // 1.12.2 rocket types: 18, 36, 54 slots, then the creative-only pre-fuelled rocket
                            for (int storage = 1; storage <= 3; storage++) {
                                output.accept(Tier1RocketItem.create(storage, 0));
                            }
                            output.accept(Tier1RocketItem.create(0, Tier1RocketEntity.FUEL_CAPACITY));
                        }
                        if (item == GCItems.OIL_CANISTER.get()) {
                            output.accept(LiquidCanisterItem.full(false));
                        } else if (item == GCItems.FUEL_CANISTER.get()) {
                            output.accept(LiquidCanisterItem.full(true));
                        }
                    }))
            .build());

    private GCCreativeTabs() {
    }
}
