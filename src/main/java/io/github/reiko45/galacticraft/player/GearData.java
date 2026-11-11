package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What other players see of someone's Galacticraft gear (1.12.2 PlayerGearData): mask, gear tubes, the two tanks
 * (1 light/green, 2 medium/orange, 3 heavy or infinite/red), frequency module dish, parachute colour (-1 none) and
 * whether the parachute is open.
 */
public record GearData(boolean mask, boolean gear, int leftTank, int rightTank, boolean frequencyModule, int parachute, boolean parachuteOpen) {
    public static final GearData NONE = new GearData(false, false, 0, 0, false, -1, false);

    public static final StreamCodec<ByteBuf, GearData> STREAM_CODEC = StreamCodec.of((buf, gear) -> {
        buf.writeByte((gear.mask ? 1 : 0) | (gear.gear ? 2 : 0) | (gear.frequencyModule ? 4 : 0) | (gear.parachuteOpen ? 8 : 0));
        buf.writeByte(gear.leftTank);
        buf.writeByte(gear.rightTank);
        buf.writeByte(gear.parachute);
    }, buf -> {
        final int flags = buf.readByte();
        return new GearData((flags & 1) != 0, (flags & 2) != 0, buf.readByte(), buf.readByte(), (flags & 4) != 0, buf.readByte(), (flags & 8) != 0);
    });

    public static GearData of(GCPlayerData data) {
        final ItemStack parachute = data.gear(GearSlot.PARACHUTE);
        return new GearData(!data.gear(GearSlot.MASK).isEmpty(), !data.gear(GearSlot.GEAR).isEmpty(),
                tankLook(data.gear(GearSlot.LEFT_TANK)), tankLook(data.gear(GearSlot.RIGHT_TANK)),
                !data.gear(GearSlot.FREQUENCY_MODULE).isEmpty(),
                parachute.getItem() instanceof ParachuteItem item ? item.color() : -1, data.usingParachute);
    }

    private static int tankLook(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem tank) {
            return tank.tier();
        }
        return stack.getItem() instanceof InfiniteOxygenTankItem ? 3 : 0;
    }
}
