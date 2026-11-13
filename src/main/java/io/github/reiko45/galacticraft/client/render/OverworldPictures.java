/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.util.MapUtil#getOverworldImageFromRaw and
 * #getMap, client.EventHandlerClient#onRenderPlanetPre, client.screen.DrawGameScreen). Copyright (c) 2023 Team
 * Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.network.MapNetwork;
import io.github.reiko45.galacticraft.world.OverworldMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The client's Overworld pictures (1.12.2 ClientProxyCore.overworldTextureClient / overworldTextureWide and
 * DrawGameScreen's map): asked for the first time one is wanted, drawn in place of the Earth's icon once they come.
 */
public final class OverworldPictures {
    private static final ResourceLocation EARTH_ICON = GalacticraftCore.id("textures/gui/celestialbodies/earth.png");
    /** The 48 x 48 Earth (1.12.2 overworldTextureClient): the strip's west end. */
    public static final ResourceLocation SQUARE = GalacticraftCore.id("dynamic/overworld_square");
    /** The whole 192 x 48 strip (1.12.2 overworldTextureWide), wrapped round a turning planet. */
    public static final ResourceLocation WIDE = GalacticraftCore.id("dynamic/overworld_wide");
    /** Local maps kept for the screens nearby. */
    private static final int LOCAL_KEEP = 8;

    private static boolean requested;
    private static boolean ready;
    private static final Map<Long, ResourceLocation> LOCAL = new LinkedHashMap<>();
    private static final Set<Long> LOCAL_REQUESTED = new HashSet<>();

    private OverworldPictures() {
    }

    /** The Earth's picture, or the fallback until it has come (1.12.2 CelestialBodyRenderEvent.Pre for the Overworld). */
    public static ResourceLocation earthOr(ResourceLocation fallback) {
        request(MapNetwork.STRIP, BlockPos.ZERO);
        return ready ? SQUARE : fallback;
    }

    /** The whole strip for a turning planet, or null until it has come. */
    @Nullable
    public static ResourceLocation earthWide() {
        request(MapNetwork.STRIP, BlockPos.ZERO);
        return ready ? WIDE : null;
    }

    /** The map round a Display Screen in the Overworld, or null until it has come. */
    @Nullable
    public static ResourceLocation localMap(BlockPos screen) {
        final long key = ChunkPos.asLong(screen.getX() >> 4, screen.getZ() >> 4);
        final ResourceLocation map = LOCAL.get(key);
        if (map == null && LOCAL_REQUESTED.add(key)) {
            send(new MapNetwork.RequestMap(MapNetwork.LOCAL, screen));
        }
        return map;
    }

    private static void request(int kind, BlockPos pos) {
        if (!requested && send(new MapNetwork.RequestMap(kind, pos))) {
            requested = true;
        }
    }

    private static boolean send(CustomPacketPayload payload) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(payload)) {
            return false;
        }
        PacketDistributor.sendToServer(payload);
        return true;
    }

    public static void receive(MapNetwork.MapImage image) {
        final RandomSource random = RandomSource.create(0L);
        if (image.kind() == MapNetwork.STRIP) {
            if (image.data().length != OverworldMap.WIDTH * OverworldMap.HEIGHT * 2) {
                return; // no picture of this world: the icon stays
            }
            final int[] palette = palette();
            if (palette == null) {
                return;
            }
            final int[] picture = OverworldMap.earthPicture(OverworldMap.stripColours(image.data(), random), palette, 64);
            upload(WIDE, picture, OverworldMap.WIDTH, OverworldMap.WIDTH, OverworldMap.HEIGHT);
            upload(SQUARE, picture, OverworldMap.WIDTH, OverworldMap.HEIGHT, OverworldMap.HEIGHT);
            ready = true;
        } else if (image.kind() == MapNetwork.LOCAL && image.data().length == OverworldMap.LOCAL_CELLS * OverworldMap.LOCAL_CELLS * 2) {
            final ChunkPos chunk = new ChunkPos(image.pos());
            final ResourceLocation id = GalacticraftCore.id("dynamic/local_map_" + chunk.x + "_" + chunk.z);
            upload(id, OverworldMap.localPicture(image.data(), random), OverworldMap.LOCAL, OverworldMap.LOCAL, OverworldMap.LOCAL);
            LOCAL.remove(chunk.toLong());
            LOCAL.put(chunk.toLong(), id);
            if (LOCAL.size() > LOCAL_KEEP) {
                final Iterator<Map.Entry<Long, ResourceLocation>> oldest = LOCAL.entrySet().iterator();
                final Map.Entry<Long, ResourceLocation> entry = oldest.next();
                Minecraft.getInstance().getTextureManager().release(entry.getValue());
                LOCAL_REQUESTED.remove(entry.getKey());
                oldest.remove();
            }
        }
    }

    /** The Earth icon's pixels, ARGB, 64 wide. */
    @Nullable
    private static int[] palette() {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(EARTH_ICON); NativeImage icon = NativeImage.read(in)) {
            final int[] argb = new int[64 * 64];
            for (int y = 0; y < Math.min(64, icon.getHeight()); y++) {
                for (int x = 0; x < Math.min(64, icon.getWidth()); x++) {
                    final int abgr = icon.getPixelRGBA(x, y);
                    argb[y * 64 + x] = abgr & 0xFF00FF00 | (abgr & 0xFF) << 16 | abgr >> 16 & 0xFF;
                }
            }
            return argb;
        } catch (IOException e) {
            GalacticraftCore.LOGGER.error("Galacticraft could not read the Earth icon", e);
            return null;
        }
    }

    /** The top-left width x height of an RGB picture (row-major, stride wide) as a texture. */
    private static void upload(ResourceLocation id, int[] rgb, int stride, int width, int height) {
        final NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int col = rgb[y * stride + x];
                image.setPixelRGBA(x, y, 0xFF000000 | (col & 0xFF) << 16 | col & 0xFF00 | col >> 16 & 0xFF);
            }
        }
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
    }

    /** Leaving a world: the next world has its own pictures. */
    public static void reset() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (ready) {
            minecraft.getTextureManager().release(SQUARE);
            minecraft.getTextureManager().release(WIDE);
        }
        LOCAL.values().forEach(minecraft.getTextureManager()::release);
        LOCAL.clear();
        LOCAL_REQUESTED.clear();
        requested = false;
        ready = false;
    }
}
