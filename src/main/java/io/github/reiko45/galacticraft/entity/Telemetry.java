/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.api.entity.ITelemetry). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

/**
 * 1.12.2 ITelemetry: an entity that sends its own five numbers to a Telemetry Unit (data[2], its speed, is already
 * filled in), and says how a Display Screen should word them and turn its picture.
 */
public interface Telemetry {
    /** Server. */
    void transmitData(int[] data);

    /** Client: the five lines under the name. */
    void receiveData(int[] data, Component[] lines);

    /** Client: turn or move the picture on the screen. */
    default void adjustDisplay(int[] data, PoseStack poseStack) {
    }
}
