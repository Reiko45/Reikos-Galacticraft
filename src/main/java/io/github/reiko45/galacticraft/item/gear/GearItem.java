package io.github.reiko45.galacticraft.item.gear;

import io.github.reiko45.galacticraft.player.GCPlayerData;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Wearable Galacticraft gear (1.12.2 IClickableItem): right-click puts it into the first free matching slot of the
 * Galacticraft inventory.
 */
public class GearItem extends Item {
    private final List<GearSlot> slots;

    public GearItem(Properties properties, GearSlot... slots) {
        super(properties.stacksTo(1));
        this.slots = List.of(slots);
    }

    public boolean fits(GearSlot slot) {
        return this.slots.contains(slot);
    }

    /** Put it into the first free slot it fits; what's left (empty if it went in). */
    public static ItemStack equip(ServerPlayer player, ItemStack stack) {
        if (stack.getItem() instanceof GearItem gear) {
            final GCPlayerData data = GCPlayerData.get(player);
            for (GearSlot slot : gear.slots) {
                if (data.gear(slot).isEmpty()) {
                    data.setGear(slot, stack.copy());
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            final GCPlayerData data = GCPlayerData.get(serverPlayer);
            for (GearSlot slot : this.slots) {
                if (data.gear(slot).isEmpty()) {
                    data.setGear(slot, stack.copy());
                    return InteractionResultHolder.success(ItemStack.EMPTY);
                }
            }
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.consume(stack);
    }
}
