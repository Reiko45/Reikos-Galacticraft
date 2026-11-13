/*
 * Derived from Galacticraft Legacy (micdoodle8.mods.galacticraft.core.client.screen.GameScreenText). Copyright (c) 2023
 * Team Galacticraft. Licensed under the MIT license (see LICENSE-GALACTICRAFT-LEGACY).
 */
package io.github.reiko45.galacticraft.client.screen.display;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.reiko45.galacticraft.block.entity.DisplayScreenBlockEntity;
import io.github.reiko45.galacticraft.block.entity.TelemetryBlockEntity;
import io.github.reiko45.galacticraft.entity.Telemetry;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.DyeColor;

/**
 * 1.12.2 GameScreenText (screen type 5): the name and five lines of what the nearest Telemetry Unit (within 32 blocks)
 * sees, with a picture of it on the right: for a mob or player "ouch!!!!" while hurt, health, heart rate, food and
 * oxygen; rockets and the Astro Miner word their own. With no unit, the time of day. The text is scaled to fit.
 */
public final class GameScreenText {
    private static final int WHITE = 0xFFF0D8FF; // 1.12.2 to32BitColor(255, 240, 216, 255)
    private static final Map<DisplayScreenBlockEntity, Picture> PICTURES = new WeakHashMap<>();

    /** The entity last drawn on a screen (1.12.2 telemetryLastClass / Name / Entity). */
    private record Picture(String name, Entity entity) {
    }

    private GameScreenText() {
    }

    /** 1.12.2 makeSpeedString: "Speed: 1.23 m/s" from hundredths. */
    public static Component speed(int speed100) {
        final int whole = speed100 / 100;
        final int hundredths = speed100 % 100;
        return Component.translatable("gui.galacticraftcore.rocket.speed").append(": " + whole + "." + (hundredths > 9 ? "" : "0") + hundredths + " ")
                .append(Component.translatable("gui.galacticraftcore.lander.velocityu"));
    }

    /** 1.12.2 makeTimeString, from a day time in hundredths of a second (one tick: 360). */
    private static String time(int hundredths) {
        final int hrs = hundredths / 360000;
        final int mins = hundredths / 6000 - hrs * 60;
        final int secs = hundredths / 100 - hrs * 3600 - mins * 60;
        return (hrs > 9 ? "" : "0") + hrs + ":" + (mins > 9 ? "" : "0") + mins + ":" + (secs > 9 ? "" : "0") + secs;
    }

    /** 1.12.2 makeOxygenString: seconds left (one unit every 9 ticks). */
    private static String oxygen(int oxygen) {
        return oxygen * 9 / 20 + "." + oxygen * 9 % 20 / 2;
    }

    @Nullable
    private static Entity picture(DisplayScreenBlockEntity driver, ClientLevel level, TelemetryBlockEntity telemetry) {
        final String name = telemetry.clientName();
        if (name.isEmpty()) {
            PICTURES.remove(driver);
            return null;
        }
        final Picture last = PICTURES.get(driver);
        if (last != null && last.name().equals(name)) {
            return last.entity();
        }
        Entity entity = null;
        if (name.startsWith("$")) {
            UUID uuid;
            try {
                uuid = UUID.fromString(telemetry.clientUuid());
            } catch (IllegalArgumentException e) {
                uuid = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            entity = new RemotePlayer(level, new GameProfile(uuid, name.substring(1)));
        } else {
            final EntityType<?> type = EntityType.byString(name).orElse(null);
            if (type != null) {
                entity = type.create(level);
            }
        }
        if (entity != null) {
            PICTURES.put(driver, new Picture(name, entity));
        }
        return entity;
    }

    /** 1.12.2: dress the picture as the real one is. */
    private static void dress(Entity entity, int[] data) {
        if (entity instanceof Horse horse) {
            withSaveData(horse, tag -> tag.putInt("Variant", data[4])); // its setter is private; colour and markings packed
        } else if (entity instanceof Villager villager && data[3] >= 0) {
            final var profession = BuiltInRegistries.VILLAGER_PROFESSION.byId(data[3]);
            if (profession != null) {
                villager.setVillagerData(villager.getVillagerData().setProfession(profession));
            }
            villager.setAge(data[4]);
        } else if (entity instanceof Wolf wolf && data[3] >= 0) {
            withSaveData(wolf, tag -> tag.putByte("CollarColor", (byte) data[3]));
            wolf.setIsInterested(data[4] == 1);
        } else if (entity instanceof Sheep sheep && data[3] >= 0) {
            sheep.setColor(DyeColor.byId(data[3]));
            sheep.setSheared(data[4] == 1);
        } else if (entity instanceof Zombie zombie) {
            zombie.setBaby(data[4] == 1);
        }
    }

    private static void withSaveData(LivingEntity entity, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> change) {
        final net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        entity.addAdditionalSaveData(tag);
        change.accept(tag);
        entity.readAdditionalSaveData(tag);
    }

    public static void render(DisplayScreenBlockEntity driver, float ticks, float sizeX, float sizeY, PoseStack poseStack, MultiBufferSource buffers,
                              float partialTick) {
        final float frameBx = sizeX - GameScreens.FRAME;
        final float frameBy = sizeY - GameScreens.FRAME;
        GameScreens.background(poseStack, buffers, GameScreens.FRAME, GameScreens.FRAME, frameBx, frameBy, 0.0F);
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final TelemetryBlockEntity telemetry = TelemetryBlockEntity.nearest(level, driver.getBlockPos());
        final Component[] lines = {Component.translatable("gui.galacticraftcore.display.nolink"), Component.empty(), Component.empty(),
                Component.empty(), Component.empty()};
        Component name = Component.empty();
        Entity entity = null;
        if (telemetry != null && telemetry.clientData().length >= 3) {
            final int[] data = telemetry.clientData();
            entity = picture(driver, level, telemetry);
            if (entity != null) {
                name = entity instanceof RemotePlayer player ? Component.literal(player.getGameProfile().getName()) : entity.getName();
                dress(entity, data);
            }
            if (entity instanceof Telemetry reporting) {
                reporting.receiveData(data, lines);
            } else if (entity instanceof LivingEntity) {
                lines[0] = data[0] > 0 ? Component.translatable("gui.galacticraftcore.player.ouch") : Component.empty();
                lines[1] = data[1] >= 0 ? Component.translatable("gui.galacticraftcore.player.health").append(": " + data[1] + "%") : Component.empty();
                lines[2] = Component.literal(data[2] + " ").append(Component.translatable("gui.galacticraftcore.player.bpm"));
                if (data[3] > -1) {
                    lines[3] = Component.translatable("gui.galacticraftcore.player.food").append(": " + data[3] + "%");
                }
                if (data[4] > -1) {
                    final int air = data[4] % 4096 + data[4] / 4096;
                    lines[4] = Component.translatable("gui.galacticraftcore.oxygen").append(": ")
                            .append(air == 180 || air == 90 ? Component.literal("OK") : Component.literal(oxygen(air)).append(Component.translatable("gui.galacticraftcore.seconds")));
                }
            } else if (data[2] >= 0) {
                lines[2] = speed(data[2]);
            }
        } else {
            final int time = (int) ((level.getDayTime() + 6000L) % 24000L);
            lines[2] = Component.literal(time(time * 360));
        }
        // 1.12.2's layout: fit 155 x 60 pixels of text (less a line for each empty last line) inside the frame
        final int textWidth = 155;
        int textHeight = 60;
        if (lines[3].getString().isEmpty()) {
            textHeight -= 10;
        }
        if (lines[4].getString().isEmpty()) {
            textHeight -= 10;
        }
        float borders = GameScreens.FRAME * 2 + 0.05F * Math.min(sizeX, sizeY);
        float scale = (sizeY - borders) / textHeight < (sizeX - borders) / textWidth ? sizeY : sizeX;
        borders = GameScreens.FRAME * 2 + 0.05F * scale;
        final float scaleX = (sizeX - borders) / textWidth;
        final float scaleY = (sizeY - borders) / textHeight;
        scale = scaleY < scaleX ? sizeY : sizeX;
        final float scaleText = Math.min(scaleX, scaleY);
        final float border = GameScreens.FRAME + 0.025F * scale;
        final float marginX = entity != null ? (sizeX - borders) / 2 : 0.0F;
        final float offsetX = (sizeX - borders - textWidth * scaleText) / 2 + marginX;
        final float offsetY = (sizeY - borders - textHeight * scaleText) / 2 + scaleText;
        poseStack.pushPose();
        poseStack.translate(border + offsetX, border + offsetY, 0.0F);
        poseStack.scale(scaleText, scaleText, 1.0F);
        final Font font = Minecraft.getInstance().font;
        int y = 0;
        for (Component line : new Component[] {name, lines[0], lines[1], lines[2], lines[3], lines[4]}) {
            font.drawInBatch(line, 0, y, WHITE, false, poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            y += 10;
        }
        if (entity != null) {
            poseStack.translate(-marginX / 2 / scaleText, textHeight / 2.0F + (-offsetY + (sizeY - borders) / 2) / scaleText, -0.0005F);
            final float factor = 38.0F / (float) Math.pow(Math.max(entity.getBbHeight(), entity.getBbWidth()), 0.65D);
            poseStack.scale(factor, factor, 0.0015F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (entity instanceof Telemetry reporting) {
                reporting.adjustDisplay(telemetry.clientData(), poseStack);
            }
            entity.setYRot(0.0F);
            entity.yRotO = 0.0F;
            entity.setXRot(0.0F);
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = living.yBodyRotO = 0.0F;
                living.yHeadRot = living.yHeadRotO = 0.0F;
            }
            Minecraft.getInstance().getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D, 0.0F, ticks % 1.0F, poseStack, buffers,
                    LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }
}
