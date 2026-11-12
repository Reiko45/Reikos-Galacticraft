package io.github.reiko45.galacticraft.block.entity;

import net.minecraft.world.phys.AABB;

/** A block entity that shows a translucent bubble around itself (1.12.2 IBubbleProviderColored). */
public interface BubbleProvider {
    /** Radius in blocks, as last synced to the client. */
    float bubbleSize();

    boolean bubbleVisible();

    /** Red, green and blue, 0-1 (1.12.2 getColor). */
    float[] bubbleColor();

    AABB renderBounds();
}
