package io.github.reiko45.galacticraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.network.GCNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Rocket and lander keys (1.12.2 KeyHandlerClient and TickHandlerClient): Jump ignites the rocket you sit in, the
 * Spaceship Inventory key (F) opens its cargo and fuel, and in a lander Forward/Back tilt it, Left/Right turn it, Jump
 * brakes and Sneak dives. Also the open parachute's slow fall, which 1.12.2 ran on the client too.
 */
public final class VehicleControls {
    public static final String CATEGORY = "key.categories.galacticraftcore";
    public static final KeyMapping SPACESHIP_INVENTORY = new KeyMapping("key.galacticraftcore.spaceship_inventory",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);

    private static boolean jumpWasDown;
    private static int lastLanderKeys = -1;

    private VehicleControls() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SPACESHIP_INVENTORY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // 1.12.2 PlayerClient#onUpdate: an open parachute holds the fall at 0.5 blocks per tick and halves sideways speed
        if (ClientPayloads.gear(minecraft.player.getId()).parachuteOpen() && !minecraft.player.getAbilities().flying
                && !minecraft.player.isInWater() && !minecraft.player.isPassenger()) {
            final net.minecraft.world.phys.Vec3 motion = minecraft.player.getDeltaMovement();
            minecraft.player.setDeltaMovement(motion.x * 0.5D, -0.5D, motion.z * 0.5D);
        }
        final Entity vehicle = minecraft.player.getVehicle();
        final Options options = minecraft.options;
        final boolean jumpDown = minecraft.screen == null && options.keyJump.isDown();
        if (vehicle instanceof Tier1RocketEntity) {
            if (jumpDown && !jumpWasDown) {
                PacketDistributor.sendToServer(new GCNetwork.IgniteRocket());
            }
            while (SPACESHIP_INVENTORY.consumeClick()) {
                PacketDistributor.sendToServer(new GCNetwork.OpenRocketCargo());
            }
        } else {
            while (SPACESHIP_INVENTORY.consumeClick()) {
                // only meaningful in a rocket
            }
        }
        jumpWasDown = jumpDown;
        if (vehicle instanceof LanderEntity) {
            int keys = 0;
            if (minecraft.screen == null) {
                keys |= options.keyUp.isDown() ? LanderEntity.KEY_PITCH_DOWN : 0;
                keys |= options.keyDown.isDown() ? LanderEntity.KEY_PITCH_UP : 0;
                keys |= options.keyLeft.isDown() ? LanderEntity.KEY_YAW_LEFT : 0;
                keys |= options.keyRight.isDown() ? LanderEntity.KEY_YAW_RIGHT : 0;
                keys |= options.keyJump.isDown() ? LanderEntity.KEY_SLOW : 0;
                keys |= options.keyShift.isDown() ? LanderEntity.KEY_FAST : 0;
            }
            if (keys != lastLanderKeys) {
                PacketDistributor.sendToServer(new GCNetwork.LanderKeys(keys));
                lastLanderKeys = keys;
            }
        } else {
            lastLanderKeys = -1;
        }
    }
}
