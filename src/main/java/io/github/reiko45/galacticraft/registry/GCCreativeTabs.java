package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.ElectricItem;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
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
                        if (!(item instanceof LiquidCanisterItem canister) || canister.kind() == LiquidCanisterItem.Kind.OIL) {
                            output.accept(item); // every empty canister is an empty (oil) canister
                        }
                        if (item instanceof ElectricItem electric) {
                            output.accept(electric.charged());
                        }
                        if (item instanceof OxygenTankItem tank) {
                            output.accept(tank.full()); // 1.12.2 listed every tank empty and full
                        }
                        if (item instanceof io.github.reiko45.galacticraft.item.RocketItem rocket) {
                            // 1.12.2 rocket types: 18, 36, 54 slots, then the creative-only pre-fuelled rocket
                            for (int storage = 1; storage <= 3; storage++) {
                                output.accept(rocket.stack(storage, 0));
                            }
                            if (item != GCItems.CARGO_ROCKET.get()) {
                                output.accept(rocket.stack(0, rocket.fuelCapacity())); // 1.12.2 had no pre-fuelled cargo rocket
                            }
                        }
                        if (item == GCItems.BUGGY.get()) {
                            // 1.12.2 buggy types 1-3: 18, 36, 54 storage slots
                            for (int storage = 1; storage <= 3; storage++) {
                                output.accept(io.github.reiko45.galacticraft.item.BuggyItem.create(storage, 0));
                            }
                        }
                        if (item instanceof LiquidCanisterItem canister) {
                            output.accept(LiquidCanisterItem.full(canister.kind()));
                        }
                    }))
            .build());

    private GCCreativeTabs() {
    }
}
