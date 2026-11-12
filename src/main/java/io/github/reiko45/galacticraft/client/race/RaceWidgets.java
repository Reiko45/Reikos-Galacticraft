/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.gui.element.GuiElementGradientButton,
 * GuiElementSlider, GuiElementCheckbox and GuiElementGradientList). Copyright (c) 2023 Team Galacticraft. Licensed under
 * the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.race;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.race.FlagData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** The Space Race screens' widgets, drawn like their 1.12.2 counterparts. */
public final class RaceWidgets {
    static final ResourceLocation GUI = GalacticraftCore.id("textures/gui/gui.png");

    private RaceWidgets() {
    }

    static int argb(int a, int r, int g, int b) {
        return FastColor.ARGB32.color(a, r, g, b);
    }

    /** 1.12.2 GuiElementGradientButton: a dark translucent box with centred text. */
    static class GradientButton extends AbstractButton {
        private final Runnable onPress;

        GradientButton(int x, int y, int width, int height, Component label, Runnable onPress) {
            super(x, y, width, height, label);
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            this.onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final boolean hovered = isHovered();
            int color = argb(150, 10, 10, 10);
            if (!this.active) {
                color = hovered ? argb(150, 30, 30, 30) : argb(150, 32, 32, 32);
            } else if (hovered) {
                color = argb(150, 30, 30, 30);
            }
            graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, color);
            final int text = !this.active ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xE0E0E0;
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + this.width / 2, getY() + (this.height - 8) / 2, text);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /**
     * 1.12.2 GuiElementSlider: vertical colour sliders (black at the top to the channel's colour at the bottom, a white
     * line at the value) or small horizontal ones (grey, with their label).
     */
    static final class Slider extends AbstractWidget {
        private final boolean vertical;
        private final int firstColor;
        private final int lastColor;
        /** 0-1. */
        private float value;
        private final Consumer<Float> onChange;

        Slider(int x, int y, int width, int height, boolean vertical, int firstColor, int lastColor, float value, Component label, Consumer<Float> onChange) {
            super(x, y, width, height, label);
            this.vertical = vertical;
            this.firstColor = firstColor;
            this.lastColor = lastColor;
            this.value = value;
            this.onChange = onChange;
        }

        float value() {
            return this.value;
        }

        void setValue(float value) {
            this.value = Mth.clamp(value, 0.0F, 1.0F);
        }

        /** 1.12.2 getColorValueD: 0-255. */
        int colorValue() {
            return Math.round(this.value * 255.0F);
        }

        private void drag(double mouseX, double mouseY) {
            setValue(this.vertical ? (float) ((mouseY - getY()) / this.height) : (float) ((mouseX - getX()) / this.width));
            this.onChange.accept(this.value);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            drag(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            drag(mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final int x = getX();
            final int y = getY();
            graphics.fill(x, y, x + this.width, y + this.height, 0xFF000000);
            if (this.vertical) {
                graphics.fillGradient(x + 1, y + 1, x + this.width - 1, y + this.height - 1, 0xFF000000 | this.firstColor, 0xFF000000 | this.lastColor);
                final int pos = y + Math.round(this.value * this.height);
                graphics.fill(x, pos - 1, x + this.width, pos + 1, 0xFFFFFFFF);
            } else {
                graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, 0xFF000000 | this.firstColor);
                final int pos = x + Math.round(this.value * this.width);
                graphics.fill(pos - 1, y, pos + 1, y + this.height, 0xFFFFFFFF);
            }
            if (isHovered() && !getMessage().getString().isEmpty()) {
                graphics.renderTooltip(Minecraft.getInstance().font, getMessage(), mouseX, mouseY);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** 1.12.2 GuiElementCheckbox with a gui.png icon (26 x 26 drawn at 13 x 13; the second column when selected). */
    static final class ToolBox extends AbstractButton {
        private final int texY;
        private boolean selected;
        private final Consumer<ToolBox> onToggle;

        ToolBox(int x, int y, int texY, boolean selected, Component tooltip, Consumer<ToolBox> onToggle) {
            super(x, y, 13, 13, tooltip);
            this.texY = texY;
            this.selected = selected;
            this.onToggle = onToggle;
        }

        boolean selected() {
            return this.selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void onPress() {
            this.selected = !this.selected;
            this.onToggle.accept(this);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(GUI, getX(), getY(), 13, 13, this.selected ? 159.0F : 133.0F, this.texY, 26, 26, 256, 256);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** 1.12.2 GuiElementGradientList: a dark list of names with a scroll bar; one can be selected. */
    static final class NameList extends AbstractWidget {
        private static final int ROW = 14;
        private List<String> names = new ArrayList<>();
        private List<Integer> colors = new ArrayList<>();
        private int selected = -1;
        private int scroll;

        NameList(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        void setContents(List<String> names, List<Integer> colors) {
            final String keep = selectedName();
            this.names = names;
            this.colors = colors;
            this.selected = keep == null ? -1 : names.indexOf(keep);
            this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, names.size() - visibleRows()));
        }

        String selectedName() {
            return this.selected >= 0 && this.selected < this.names.size() ? this.names.get(this.selected) : null;
        }

        private int visibleRows() {
            return Math.max(1, (this.height - 2) / ROW);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (mouseX < getX() + this.width - 10) {
                final int row = (int) ((mouseY - getY() - 1) / ROW) + this.scroll;
                this.selected = row >= 0 && row < this.names.size() ? row : -1;
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, Math.max(0, this.names.size() - visibleRows()));
            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final int x = getX();
            final int y = getY();
            graphics.fill(x, y, x + this.width - 10, y + this.height, argb(255, 30, 30, 30));
            graphics.fill(x + this.width - 9, y, x + this.width, y + this.height, argb(255, 50, 50, 50));
            final boolean scrolls = this.names.size() > visibleRows();
            final int rows = visibleRows();
            final int sliderY = scrolls ? y + (this.height - 15) * this.scroll / Math.max(1, this.names.size() - rows) : y;
            graphics.fill(x + this.width - 9, sliderY, x + this.width, sliderY + 15, scrolls ? argb(255, 90, 90, 90) : argb(255, 40, 40, 40));
            graphics.renderOutline(x, y, this.width, this.height, 0xFF000000);
            graphics.fill(x + this.width - 10, y, x + this.width - 9, y + this.height, 0xFF000000);
            for (int i = 0; i < rows && i + this.scroll < this.names.size(); i++) {
                final int index = i + this.scroll;
                final int rowY = y + 1 + i * ROW;
                graphics.fill(x + 1, rowY, x + this.width - 10, rowY + ROW - 1, index == this.selected ? argb(255, 35, 35, 35) : argb(255, 25, 25, 25));
                graphics.drawString(Minecraft.getInstance().font, this.names.get(index), x + 4, rowY + 3, this.colors.get(index), false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }

    /** A flag drawn flat in a box, one rectangle per pixel (1.12.2 drew the 3D flag model in the GUI). */
    static void drawFlag(GuiGraphics graphics, FlagData flag, float x, float y, float width, float height) {
        final float cellW = width / flag.width();
        final float cellH = height / flag.height();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(cellW, cellH, 1.0F);
        for (int px = 0; px < flag.width(); px++) {
            for (int py = 0; py < flag.height(); py++) {
                graphics.fill(px, py, px + 1, py + 1, 0xFF000000 | flag.colorAt(px, py));
            }
        }
        graphics.pose().popPose();
    }
}
