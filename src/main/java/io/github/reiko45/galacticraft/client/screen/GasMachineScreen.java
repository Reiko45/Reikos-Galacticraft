/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiWaterElectrolyzer,
 * GuiMethaneSynthesizer and GuiGasLiquefier). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.menu.GasMachineMenu;
import io.github.reiko45.galacticraft.registry.GCFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Mars gas machines' GUIs: the 1.12.2 panels, their gauges drawn from the shared gas sheet (methane, oxygen,
 * hydrogen/nitrogen, liquid oxygen, water/liquid nitrogen and argon columns), the energy bar, the status and the
 * Process / Stop button.
 */
public class GasMachineScreen extends MachineScreen<GasMachineMenu> {
    private static final ResourceLocation GASES = GalacticraftCore.id("textures/gui/gases.png");

    /** A gauge: where it is, how tall it can be, and which column of the gas sheet it shows (-1: the panel's fuel). */
    private record Gauge(int tank, int x, int top, int height, int column, String label) {
    }

    private final List<Gauge> gauges = new ArrayList<>();
    private final int titleX;
    private final int energyX;
    private final int statusX;
    private final int buttonX;
    private Button toggle;

    public GasMachineScreen(GasMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, texture(menu.kind()), 176, 168);
        switch (menu.kind()) {
            case ELECTROLYZER -> {
                this.titleX = 30;
                this.energyX = 28;
                this.statusX = 56;
                this.buttonX = -49;
                this.gauges.add(new Gauge(0, 7, 28, 38, 4, "gui.galacticraftcore.water_tank"));
                this.gauges.add(new Gauge(1, 132, 28, 38, 2, "gui.galacticraftcore.gas_tank"));
                this.gauges.add(new Gauge(2, 153, 28, 38, 1, "gui.galacticraftcore.gas_tank"));
            }
            case METHANE_SYNTHESIZER -> {
                this.titleX = 47;
                this.energyX = 52;
                this.statusX = 72;
                this.buttonX = -28;
                this.gauges.add(new Gauge(0, 7, 28, 38, 1, "gui.galacticraftcore.gas_tank"));
                this.gauges.add(new Gauge(1, 28, 28, 20, 2, "gui.galacticraftcore.gas_tank"));
                this.gauges.add(new Gauge(2, 153, 28, 38, 2, "gui.galacticraftcore.gas_tank"));
            }
            default -> {
                this.titleX = 40;
                this.energyX = 28;
                this.statusX = 56;
                this.buttonX = -49;
                this.gauges.add(new Gauge(0, 7, 28, 38, 0, "gui.galacticraftcore.gas_tank"));
                this.gauges.add(new Gauge(1, 132, 28, 38, 0, "gui.galacticraftcore.gas_tank"));
            }
        }
        energyTooltipRegion(this.energyX + 14, 16, 56, 9);
    }

    private static String texture(GasMachineMenu.Kind kind) {
        return switch (kind) {
            case ELECTROLYZER -> "gas_liquefier";
            case METHANE_SYNTHESIZER -> "methane_synthesizer";
            case GAS_LIQUEFIER -> "gas_liquefier_m";
        };
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.titleX;
        this.titleLabelY = 5;
        this.toggle = addRenderableWidget(Button.builder(buttonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, GasMachineMenu.BUTTON_TOGGLE);
            }
        }).bounds(this.width / 2 + this.buttonX, this.height / 2 - 56, 76, 20).build());
    }

    private Component buttonLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.process" : "gui.galacticraftcore.button.stop_process");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.toggle.setMessage(buttonLabel());
    }

    /** 1.12.2 GuiGasLiquefier: the gas sheet column for what a liquefier tank holds (-1: fuel, from the panel). */
    private int liquefierColumn(Gauge gauge) {
        final Fluid fluid = this.menu.tankFluid(gauge.tank);
        if (gauge.tank == 0) {
            return fluid == GCFluids.METHANE.get() ? 0 : fluid == GCFluids.OXYGEN.get() ? 1 : 2;
        }
        if (fluid == GCFluids.LIQUID_OXYGEN.get()) {
            return 3;
        }
        if (fluid == GCFluids.LIQUID_NITROGEN.get()) {
            return 4;
        }
        return fluid == GCFluids.LIQUID_ARGON.get() ? 5 : -1;
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        for (Gauge gauge : this.gauges) {
            final int capacity = this.menu.tankCapacity(gauge.tank);
            final int level = capacity <= 0 ? 0 : this.menu.tankAmount(gauge.tank) * gauge.height / capacity;
            if (level <= 0) {
                continue;
            }
            final int column = this.menu.kind() == GasMachineMenu.Kind.GAS_LIQUEFIER ? liquefierColumn(gauge) : gauge.column;
            final int y = this.topPos + gauge.top + gauge.height - level;
            if (column < 0) {
                graphics.blit(this.texture, this.leftPos + gauge.x, y, 192, 38 - level, 16, level);
            } else {
                graphics.blit(GASES, this.leftPos + gauge.x, y, 1 + column * 17, 38 - level, 16, level);
            }
        }
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + this.energyX, this.topPos + 16, 208, 0, 11, 10);
        }
        blitHorizontal(graphics, this.energyX + 14, 17, 176, 38, 54, 7, energyFraction());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        for (Gauge gauge : this.gauges) {
            if (x >= gauge.x && x < gauge.x + 16 && y >= gauge.top && y < gauge.top + gauge.height) {
                final List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(gauge.label));
                final Fluid fluid = this.menu.tankFluid(gauge.tank);
                if (fluid != Fluids.EMPTY) {
                    lines.add(Component.literal("(").append(new FluidStack(fluid, 1).getHoverName()).append(")"));
                }
                lines.add(Component.literal(this.menu.tankAmount(gauge.tank) + " / " + this.menu.tankCapacity(gauge.tank)).withStyle(ChatFormatting.YELLOW));
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.status").append(":"), this.statusX, 50, 0x404040, false);
        graphics.drawString(this.font, this.menu.status().text(), this.statusX + (this.menu.kind() == GasMachineMenu.Kind.METHANE_SYNTHESIZER ? 3 : 6),
                60, 0x404040, false);
    }
}
