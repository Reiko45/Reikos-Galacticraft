package io.github.reiko45.galacticraft.player;

import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.InfiniteOxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.item.gear.ParachuteItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What other players see of someone's Galacticraft gear (1.12.2 PlayerGearData): mask, gear tubes, the two tanks
 * (1 light/green, 2 medium/orange, 3 heavy or infinite/red), frequency module dish and parachute colour (-1 none).
 */
public record GearData(boolean mask, boolean gear, int leftTank, int rightTank, boolean frequencyModule, int parachute) {
    public static final GearData NONE = new GearData(false, false, 0, 0, false, -1);

    public static final StreamCodec<ByteBuf, GearData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GearData::mask,
            ByteBufCodecs.BOOL, GearData::gear,
            ByteBufCodecs.VAR_INT, GearData::leftTank,
            ByteBufCodecs.VAR_INT, GearData::rightTank,
            ByteBufCodecs.BOOL, GearData::frequencyModule,
            ByteBufCodecs.VAR_INT, GearData::parachute,
            GearData::new);

    public static GearData of(GCPlayerData data) {
        final ItemStack parachute = data.gear(GearSlot.PARACHUTE);
        return new GearData(!data.gear(GearSlot.MASK).isEmpty(), !data.gear(GearSlot.GEAR).isEmpty(),
                tankLook(data.gear(GearSlot.LEFT_TANK)), tankLook(data.gear(GearSlot.RIGHT_TANK)),
                !data.gear(GearSlot.FREQUENCY_MODULE).isEmpty(),
                parachute.getItem() instanceof ParachuteItem item ? item.color() : -1);
    }

    private static int tankLook(ItemStack stack) {
        if (stack.getItem() instanceof OxygenTankItem tank) {
            return tank.tier();
        }
        return stack.getItem() instanceof InfiniteOxygenTankItem ? 3 : 0;
    }
}
