package io.github.reiko45.galacticraft.client.screen;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenStorageModuleBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import io.github.reiko45.galacticraft.menu.OxygenMachineMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUIs of the oxygen machines (1.12.2 GuiOxygenCollector, GuiOxygenCompressor, GuiOxygenDecompressor,
 * GuiOxygenDistributor, GuiOxygenSealer, GuiOxygenStorageModule), drawn from the 1.12.2 sheets at the same coordinates.
 * The first four share one layout: an oxygen bar and an energy bar on the right, the status and a rate line below.
 */
public class OxygenMachineScreen extends MachineScreen<OxygenMachineMenu> {
    private static final ResourceLocation WIDGETS = GalacticraftCore.id("textures/gui/gui.png");
    private static final int LABEL = 0x404040;
    private final OxygenMachineMenu.Kind kind;
    private final int panelY;
    private Button sealButton;

    public OxygenMachineScreen(OxygenMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, texture(menu.kind()), 176, height(menu.kind()));
        this.kind = menu.kind();
        // 1.12.2 drew most oxygen GUIs 5 pixels below the container origin.
        this.panelY = this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE ? 0 : 5;
        if (this.kind != OxygenMachineMenu.Kind.STORAGE_MODULE) {
            energyTooltipRegion(112, this.kind == OxygenMachineMenu.Kind.SEALER ? 36 : 37, 56, 9);
        }
    }

    private static String texture(OxygenMachineMenu.Kind kind) {
        return switch (kind) {
            case COLLECTOR -> "oxygen";
            case COMPRESSOR -> "compressor";
            case DECOMPRESSOR -> "decompressor";
            case DISTRIBUTOR -> "oxygen_distributor";
            case SEALER -> "oxygen_sealer";
            case STORAGE_MODULE -> "oxygen_storage_module";
        };
    }

    private static int height(OxygenMachineMenu.Kind kind) {
        return switch (kind) {
            case SEALER -> 200;
            case STORAGE_MODULE -> 166;
            default -> 180;
        };
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 10;
        this.inventoryLabelY = switch (this.kind) {
            case COLLECTOR -> 92;
            case SEALER -> this.imageHeight - 87;
            case STORAGE_MODULE -> this.imageHeight - 94;
            case DISTRIBUTOR -> 93;
            default -> 93;
        };
        if (this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE) {
            this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
            this.titleLabelY = 6;
        }
        if (this.kind == OxygenMachineMenu.Kind.SEALER) {
            this.sealButton = addRenderableWidget(Button.builder(sealLabel(), button -> press(OxygenMachineMenu.BUTTON_DISABLE))
                    .bounds(this.leftPos + 50, this.topPos + 91, 76, 20).build());
        }
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private Component sealLabel() {
        return Component.translatable(this.menu.isDisabled() ? "gui.galacticraftcore.button.enable_seal" : "gui.galacticraftcore.button.disable_seal");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.sealButton != null) {
            this.sealButton.setMessage(sealLabel());
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(this.texture, this.leftPos, this.topPos + this.panelY, 0, 0, this.imageWidth,
                this.kind == OxygenMachineMenu.Kind.SEALER || this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE ? this.imageHeight : 181);
        renderOverlays(graphics);
    }

    private float oxygenFraction() {
        return this.menu.oxygenCapacity() <= 0 ? 0.0F : (float) this.menu.oxygen() / this.menu.oxygenCapacity();
    }

    @Override
    protected void renderOverlays(GuiGraphics graphics) {
        if (this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE) {
            blitHorizontal(graphics, 52, 52, 176, 0, 72, 3, oxygenFraction());
            return;
        }
        final int dy = this.kind == OxygenMachineMenu.Kind.SEALER ? -1 : 0;
        blitHorizontal(graphics, 113, 25 + dy, 197, 7, 54, 7, oxygenFraction());
        blitHorizontal(graphics, 113, 38 + dy, 197, 0, 54, 7, energyFraction());
        if (this.menu.energy() > 0) {
            graphics.blit(this.texture, this.leftPos + 99, this.topPos + 37 + dy, 176, 0, 11, 10);
        }
        if (this.menu.oxygen() > 0) {
            graphics.blit(this.texture, this.leftPos + 100, this.topPos + 24 + dy, 187, 0, 10, 10);
        }
        if (this.kind == OxygenMachineMenu.Kind.SEALER) {
            // Seal check timer
            final int scale = this.menu.value1();
            graphics.blit(this.texture, this.leftPos + 148, this.topPos + 60, 176, 14, 10, 27);
            if (scale != 0) {
                graphics.blit(this.texture, this.leftPos + 149, this.topPos + 61 + scale, 186, 14, 8, 25 - scale);
            }
        }
        if (this.kind == OxygenMachineMenu.Kind.DISTRIBUTOR) {
            // 1.12.2 GuiElementCheckbox: 13x13 at (20, 24) of gui.png, the ticked box 13 to the right
            graphics.blit(WIDGETS, this.leftPos + 85, this.topPos + 87, this.menu.value1() != 0 ? 33 : 20, 24, 13, 13);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE) {
            centeredAt(graphics, Component.literal(this.menu.oxygen() + " ").append(Component.translatable("gui.galacticraftcore.of")), 87, 30);
            centeredAt(graphics, Component.literal(String.valueOf(this.menu.oxygenCapacity())), 87, 40);
            centeredAt(graphics, Component.translatable("gui.galacticraftcore.max_output")
                    .append(": " + OxygenStorageModuleBlockEntity.OUTPUT_PER_TICK * 20).append(Component.translatable("gui.galacticraftcore.per_second")), 87, 60);
            return;
        }
        final int dy = this.kind == OxygenMachineMenu.Kind.SEALER ? 0 : 0;
        if (this.kind == OxygenMachineMenu.Kind.COLLECTOR) {
            rightAligned(graphics, Component.translatable("gui.galacticraftcore.out").append(":"), 99, 25);
            rightAligned(graphics, Component.translatable("gui.galacticraftcore.in").append(":"), 99, 37);
        } else {
            rightAligned(graphics, Component.translatable("gui.galacticraftcore.in").append(":"), 99, 26 + dy);
            rightAligned(graphics, Component.translatable("gui.galacticraftcore.in").append(":"), 99, 38 + dy);
        }
        final MachineStatus status = this.menu.status();
        Component statusText = status.text();
        if (status == MachineStatus.CHECK_STARTING) {
            statusText = Component.translatable("gui.galacticraftcore.status.check_starting")
                    .append(".".repeat((int) ((this.minecraft.level.getGameTime() / 5) % 4))).withStyle(ChatFormatting.GOLD);
        } else if (status == MachineStatus.CHECKING_SEAL) {
            statusText = status.text().copy().append("...");
        }
        centered(graphics, Component.translatable("gui.galacticraftcore.status").append(": ").append(statusText), 50);
        centered(graphics, secondLine(), 60);
        if (this.kind == OxygenMachineMenu.Kind.SEALER) {
            final Component thermal = switch (this.menu.value2()) {
                case 1 -> Component.translatable("gui.galacticraftcore.status.on").withStyle(ChatFormatting.DARK_GREEN);
                case 2 -> Component.translatable("gui.galacticraftcore.status.not_available").withStyle(ChatFormatting.DARK_RED);
                default -> Component.translatable("gui.galacticraftcore.status.off").withStyle(ChatFormatting.DARK_RED);
            };
            centered(graphics, Component.translatable("gui.galacticraftcore.thermal_status").append(": ").append(thermal), 70);
        }
        if (this.kind == OxygenMachineMenu.Kind.DISTRIBUTOR) {
            graphics.drawString(this.font, Component.translatable("gui.galacticraftcore.bubble_visible"), 85 + 13 + 3, 87 + 3, LABEL, false);
        }
    }

    private Component secondLine() {
        final Component perSecond = Component.translatable("gui.galacticraftcore.per_second");
        return switch (this.kind) {
            case COLLECTOR -> Component.translatable("gui.galacticraftcore.status.collecting")
                    .append(": " + Math.min(this.menu.value1() * 2, 100 * 20)).append(perSecond);
            case COMPRESSOR -> Component.translatable("gui.galacticraftcore.oxygen_use")
                    .append(": " + OxygenCompressorBlockEntity.TANK_TRANSFER_SPEED * 20).append(perSecond);
            case DECOMPRESSOR -> Component.translatable("gui.galacticraftcore.max_output").append(": " + 100 * 20).append(perSecond);
            case DISTRIBUTOR -> Component.translatable("gui.galacticraftcore.oxygen_use").append(": " + 8 * 20).append(perSecond);
            case SEALER -> {
                final MachineStatus status = this.menu.status();
                final boolean running = !this.menu.isDisabled() && status != MachineStatus.MISSING_POWER && status != MachineStatus.LOW_MISSING_POWER;
                yield Component.translatable("gui.galacticraftcore.oxygen_use")
                        .append(": " + (running ? (status == MachineStatus.SEALED ? 2 : 12) * 20 : 0)).append(perSecond);
            }
            case STORAGE_MODULE -> Component.empty();
        };
    }

    private void centered(GuiGraphics graphics, Component text, int y) {
        graphics.drawString(this.font, text, (this.imageWidth - this.font.width(text)) / 2, y, LABEL, false);
    }

    private void centeredAt(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(this.font, text, x - this.font.width(text) / 2, y, LABEL, false);
    }

    private void rightAligned(GuiGraphics graphics, Component text, int right, int y) {
        graphics.drawString(this.font, text, right - this.font.width(text), y, LABEL, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.kind == OxygenMachineMenu.Kind.DISTRIBUTOR) {
            final double x = mouseX - this.leftPos;
            final double y = mouseY - this.topPos;
            if (x >= 85 && x < 98 && y >= 87 && y < 100) {
                press(OxygenMachineMenu.BUTTON_BUBBLE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        final int x = mouseX - this.leftPos;
        final int y = mouseY - this.topPos;
        final boolean overOxygen = this.kind == OxygenMachineMenu.Kind.STORAGE_MODULE
                ? x >= 52 && x < 124 && y >= 50 && y < 57
                : x >= 112 && x < 168 && y >= (this.kind == OxygenMachineMenu.Kind.SEALER ? 23 : 24)
                        && y < (this.kind == OxygenMachineMenu.Kind.SEALER ? 32 : 33);
        if (overOxygen) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.galacticraftcore.oxygen_storage"),
                    Component.translatable("gui.galacticraftcore.oxygen").append(": " + this.menu.oxygen() + " / " + this.menu.oxygenCapacity())
                            .withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }
}
