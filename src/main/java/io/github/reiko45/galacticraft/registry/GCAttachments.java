package io.github.reiko45.galacticraft.registry;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments (1.12.2 capabilities attached to entities). */
public final class GCAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GalacticraftCore.MOD_ID);

    /** 1.12.2 GCPlayerStats. Copied on respawn by GCPlayerHandler (items only when they were not dropped). */
    public static final Supplier<AttachmentType<GCPlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data",
            () -> AttachmentType.serializable(GCPlayerData::new).build());

    private GCAttachments() {
    }
}
