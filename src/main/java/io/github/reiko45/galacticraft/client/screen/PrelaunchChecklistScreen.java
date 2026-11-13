/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.screen.GuiPreLaunchChecklist,
 * client.gui.element.GuiElementCheckboxPreLaunch and util.WorldUtil#getAllChecklistKeys). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.ChecklistNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.12.2 GuiPreLaunchChecklist: a 192 x 192 book, top centred, listing for each world you can fly to (by the rocket tier
 * it needs, as 1.12.2 sorted them) its name and what to do first, each with a box to tick. Ticking a world ticks all its
 * items; entries that don't fit go on to the next page.
 */
public class PrelaunchChecklistScreen extends Screen {
    private static final ResourceLocation TEXTURE = GalacticraftCore.id("textures/gui/checklist_book.png");
    private static final int BOOK = 192;

    /**
     * 1.12.2 addChecklistKeys for each body, in getAllChecklistKeys' order: tier 1 the Overworld, the Moon and space
     * stations; tier 2 Mars; tier 3 the asteroids and Venus.
     */
    private static final String[][] CHECKLIST = {
            {"planet.galacticraftcore.overworld", "equip_parachute"},
            {"moon.galacticraftcore.moon", "equip_oxygen_suit"},
            {"satellite.galacticraftcore.overworld_station", "equip_oxygen_suit", "create_grapple"},
            {"planet.galacticraftcore.mars", "equip_oxygen_suit", "thermal_padding"},
            {"planet.galacticraftcore.asteroids", "equip_oxygen_suit", "craft_grapple_hook", "thermal_padding"},
            {"planet.galacticraftcore.venus", "equip_oxygen_suit", "equip_shield_controller", "thermal_padding_t2"},
    };

    /** A box on the page: x, y, its key in the data, its text, and whether it is a world (ticking it ticks the rest). */
    private record Box(int x, int y, String key, Component text, boolean title) {
    }

    private final InteractionHand hand;
    private final CompoundTag data;
    private final List<Box> boxes = new ArrayList<>();
    private int page;
    private int pages;

    public PrelaunchChecklistScreen(InteractionHand hand, CompoundTag data) {
        super(Component.translatable("gui.galacticraftcore.message.prelaunch"));
        this.hand = hand;
        this.data = data;
    }

    private int lines(Component text) {
        return this.font.split(text, 100).size() * this.font.lineHeight;
    }

    @Override
    protected void init() {
        this.boxes.clear();
        int y = 25;
        int page = 0;
        for (String[] entry : CHECKLIST) {
            for (int i = 0; i < entry.length; i++) {
                final boolean title = i == 0;
                final Component text = Component.translatable(title ? entry[0] : "checklist.galacticraftcore." + entry[i]);
                final String key = title ? entry[0] : entry[0] + "." + entry[i];
                int size = lines(text);
                if (size > 152 - y) {
                    page++;
                    y = 25;
                }
                if (page == this.page) {
                    this.boxes.add(new Box(this.width / 2 - 73 + (title ? 11 : 16), y, key, text, title));
                }
                y += size + this.font.lineHeight / 2;
            }
        }
        this.pages = page + 1;
    }

    private boolean ticked(Box box) {
        return this.data.getBoolean(box.key());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int left = (this.width - BOOK) / 2;
        // the page turners (1.12.2 NextPageButton, 23 x 13)
        if (this.page < this.pages - 1 && inside(mouseX, mouseY, left + 120, 2 + 154, 23, 13)) {
            this.page++;
            init();
            return true;
        }
        if (this.page > 0 && inside(mouseX, mouseY, left + 38, 2 + 154, 23, 13)) {
            this.page--;
            init();
            return true;
        }
        for (int i = 0; i < this.boxes.size(); i++) {
            final Box box = this.boxes.get(i);
            if (inside(mouseX, mouseY, box.x(), box.y(), 9, 9)) {
                final boolean ticked = !ticked(box);
                this.data.putBoolean(box.key(), ticked);
                // 1.12.2 onSelectionChanged: the indented boxes after it follow
                for (int j = i + 1; j < this.boxes.size() && this.boxes.get(j).x() > box.x(); j++) {
                    this.data.putBoolean(this.boxes.get(j).key(), ticked);
                }
                PacketDistributor.sendToServer(new ChecklistNetwork.UpdateChecklist(this.hand, this.data.copy()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int left = (this.width - BOOK) / 2;
        graphics.blit(TEXTURE, left, 2, 0, 0, BOOK, BOOK);
        if (this.page < this.pages - 1) {
            graphics.blit(TEXTURE, left + 120, 2 + 154, inside(mouseX, mouseY, left + 120, 156, 23, 13) ? 23 : 0, 192, 23, 13);
        }
        if (this.page > 0) {
            graphics.blit(TEXTURE, left + 38, 2 + 154, inside(mouseX, mouseY, left + 38, 156, 23, 13) ? 23 : 0, 205, 23, 13);
        }
        for (Box box : this.boxes) {
            final boolean ticked = ticked(box);
            final boolean hovered = inside(mouseX, mouseY, box.x(), box.y(), 9, 9);
            // 1.12.2: an empty 9 x 9 box, or a 12 x 16 tick reaching up out of it
            if (ticked) {
                graphics.blit(TEXTURE, box.x(), box.y() - 7, hovered ? 206 : 194, 9, 12, 16);
            } else {
                graphics.blit(TEXTURE, box.x(), box.y(), hovered ? 206 : 194, 0, 9, 9);
            }
            int y = box.y() + 1;
            for (var line : this.font.split(box.text(), 100)) {
                graphics.drawString(this.font, line, box.x() + 9 + 3, y, 0xFF050505, false);
                y += this.font.lineHeight;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
