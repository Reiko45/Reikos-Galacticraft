/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.blocks.BlockSpaceGlass#updateGlassColors and
 * BlockPanelLighting#setColor). Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see
 * LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.block.SpaceGlassBlock;
import io.github.reiko45.galacticraft.network.PaintNetwork;
import io.github.reiko45.galacticraft.util.ColorUtil;
import net.minecraft.client.Minecraft;

/**
 * The colours this client shows the paintable blocks in: the plain-framed Space Glass of each kind (the tin-framed glass
 * keeps its colour, as in 1.12.2) and the lit Lighting Panels. A change redraws every chunk, as 1.12.2 did.
 */
public final class PaintColors {
    private static final int[] GLASS = {-1, -1, -1};
    private static int panel = 0xF0F0E0;

    private PaintColors() {
    }

    public static void set(PaintNetwork.PaintColors colors) {
        final int panelColor = colors.panel() == 0xF0F0E0 ? 0xF0F0E0 : ColorUtil.lighten(ColorUtil.lightenFully(colors.panel(), 255), 0.1F);
        final boolean changed = GLASS[0] != colors.glass1() || GLASS[1] != colors.glass2() || GLASS[2] != colors.glass3() || panel != panelColor;
        GLASS[0] = colors.glass1();
        GLASS[1] = colors.glass2();
        GLASS[2] = colors.glass3();
        panel = panelColor;
        if (changed && Minecraft.getInstance().levelRenderer != null) {
            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    /** A glass block's frame colour. */
    public static int glass(SpaceGlassBlock glass) {
        final int painted = GLASS[glass.type().ordinal()];
        return painted >= 0 && glass.frame() == SpaceGlassBlock.GlassFrame.PLAIN ? painted : glass.frame().color();
    }

    /** The lit panels' colour (1.12.2 BlockPanelLighting.color). */
    public static int panel() {
        return panel;
    }
}
