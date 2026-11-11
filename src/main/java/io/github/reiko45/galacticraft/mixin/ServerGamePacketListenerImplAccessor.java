package io.github.reiko45.galacticraft.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets players float in orbit without being kicked for flying (1.12.2 reset the same counters). */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
    @Accessor("aboveGroundTickCount")
    void galacticraftcore$setAboveGroundTickCount(int ticks);

    @Accessor("aboveGroundVehicleTickCount")
    void galacticraftcore$setAboveGroundVehicleTickCount(int ticks);
}
