/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.items.ItemBlockDesc and ItemDesc). Copyright (c)
 * 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.energy.EnergyDisplay;
import io.github.reiko45.galacticraft.item.ShiftDescribed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * 1.12.2's "Press Shift for more info" (ItemBlockDesc, ItemDesc): Galacticraft blocks and items with a
 * {@code <description id>.description} text (or a {@link ShiftDescribed} one) show, while the sneak key is held, that
 * text wrapped to 150 pixels; otherwise the prompt, after the power a machine draws when it runs.
 */
public final class ShiftDescriptions {
    /** One block entity of each machine, made once, to ask what it draws (1.12.2 made a new one each time). */
    private static final Map<Block, MachineBlockEntity> MACHINES = new HashMap<>();
    /** The screenshot tour's stand-in for holding the sneak key (a key held on the test display can't be faked). */
    public static boolean forceSneak;

    private ShiftDescriptions() {
    }

    @Nullable
    public static String key(ItemStack stack) {
        final Item item = stack.getItem();
        if (item instanceof ShiftDescribed described) {
            return described.shiftDescriptionKey(stack);
        }
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShiftDescribed described) {
            return described.shiftDescriptionKey(stack);
        }
        return stack.getDescriptionId() + ".description";
    }

    public static void onTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(GalacticraftCore.MOD_ID)) {
            return;
        }
        final String key = key(stack);
        if (key == null || !I18n.exists(key)) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final List<Component> lines = new ArrayList<>();
        if (sneakHeld(minecraft)) {
            for (FormattedText line : minecraft.font.getSplitter().splitLines(Component.translatable(key), 150, Style.EMPTY)) {
                lines.add(Component.literal(line.getString()));
            }
        } else {
            final float draw = powerDraw(stack);
            if (draw > 0.0F) {
                lines.add(Component.translatable("item.galacticraftcore.power_draw", EnergyDisplay.format(draw * 20.0F))
                        .withStyle(ChatFormatting.GREEN));
            }
            lines.add(Component.translatable("item.galacticraftcore.shift_description", minecraft.options.keyShift.getTranslatedKeyMessage()));
        }
        final List<Component> tooltip = event.getToolTip();
        tooltip.addAll(insertAt(tooltip, id, event.getFlags().isAdvanced()), lines);
    }

    /** Before the advanced lines (durability, registry name, components), where 1.12.2's addInformation lines went. */
    private static int insertAt(List<Component> tooltip, ResourceLocation id, boolean advanced) {
        if (advanced) {
            for (int i = tooltip.size() - 1; i > 0; i--) {
                if (tooltip.get(i).getString().equals(id.toString())) {
                    return i > 1 && tooltip.get(i - 1).getContents() instanceof TranslatableContents contents
                            && contents.getKey().equals("item.durability") ? i - 1 : i;
                }
            }
        }
        return tooltip.size();
    }

    private static boolean sneakHeld(Minecraft minecraft) {
        if (forceSneak) {
            return true;
        }
        final InputConstants.Key key = minecraft.options.keyShift.getKey();
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    /** gJ a tick the block's machine uses while it works (1.12.2 storage.getMaxExtract()), 0 for anything else. */
    public static float powerDraw(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof EntityBlock entityBlock)) {
            return 0.0F;
        }
        final Block block = blockItem.getBlock();
        MachineBlockEntity machine = MACHINES.get(block);
        if (machine == null && !MACHINES.containsKey(block)) {
            try {
                final BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                machine = blockEntity instanceof MachineBlockEntity found ? found : null;
            } catch (RuntimeException e) {
                machine = null;
            }
            MACHINES.put(block, machine);
        }
        return machine == null ? 0.0F : machine.powerDraw();
    }
}
