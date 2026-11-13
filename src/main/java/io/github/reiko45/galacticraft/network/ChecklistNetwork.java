/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.network.PacketSimple S_UPDATE_CHECKLIST).
 * Copyright (c) 2023 Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.network;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.item.PrelaunchChecklistItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The Pre-Launch Checklist's ticks, sent to the server to keep on the book in hand. */
public final class ChecklistNetwork {
    private ChecklistNetwork() {
    }

    /** 1.12.2 S_UPDATE_CHECKLIST (1.12.2 always wrote to the main hand). */
    public record UpdateChecklist(InteractionHand hand, CompoundTag data) implements CustomPacketPayload {
        public static final Type<UpdateChecklist> TYPE = new Type<>(GalacticraftCore.id("update_checklist"));
        public static final StreamCodec<ByteBuf, UpdateChecklist> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, payload -> payload.hand() == InteractionHand.OFF_HAND, ByteBufCodecs.COMPOUND_TAG, UpdateChecklist::data,
                (off, data) -> new UpdateChecklist(off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, data));

        @Override
        public Type<UpdateChecklist> type() {
            return TYPE;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(UpdateChecklist.TYPE, UpdateChecklist.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            final ItemStack stack = context.player().getItemInHand(payload.hand());
            if (stack.getItem() instanceof PrelaunchChecklistItem && payload.data().size() <= 64) {
                PrelaunchChecklistItem.setChecklist(stack, payload.data());
            }
        }));
    }
}
