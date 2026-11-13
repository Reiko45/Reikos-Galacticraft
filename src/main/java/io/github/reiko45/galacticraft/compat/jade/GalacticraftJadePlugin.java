package io.github.reiko45.galacticraft.compat.jade;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ProcessingMachineBlockEntity;
import io.github.reiko45.galacticraft.machine.MachineStatus;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

/**
 * Jade (optional; 1.12.2 had no WAILA plugin): Galacticraft machines show their energy in gJ (instead of Jade's FE
 * bar), a processing machine its progress and status, and an owned block (Launch Controller, Laser Turret) its owner.
 */
@WailaPlugin(GalacticraftCore.MOD_ID)
public class GalacticraftJadePlugin implements IWailaPlugin {
    static final ResourceLocation ENERGY = GalacticraftCore.id("energy");
    static final ResourceLocation PROGRESS = GalacticraftCore.id("progress");
    static final ResourceLocation MACHINE = GalacticraftCore.id("machine");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(Energy.INSTANCE, MachineBlockEntity.class);
        registration.registerProgress(Progress.INSTANCE, ProcessingMachineBlockEntity.class);
        registration.registerBlockDataProvider(Machine.INSTANCE, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(Energy.INSTANCE);
        registration.registerProgressClient(Progress.INSTANCE);
        registration.registerBlockComponent(Machine.INSTANCE, MachineBlock.class);
    }

    /** The machine's own store, in gJ. */
    enum Energy implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, EnergyView> {
        INSTANCE;

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (accessor.getTarget() instanceof MachineBlockEntity machine && machine.energy().getCapacityGJ() > 0.0F) {
                return List.of(new ViewGroup<>(List.of(EnergyView.of((long) Math.floor(machine.energy().getStoredGJ()),
                        (long) machine.energy().getCapacityGJ()))));
            }
            return null;
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, tag -> EnergyView.read(tag, "gJ"), null);
        }

        @Override
        public ResourceLocation getUid() {
            return ENERGY;
        }
    }

    /** How far along the current job is. */
    enum Progress implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, ProgressView> {
        INSTANCE;

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (accessor.getTarget() instanceof ProcessingMachineBlockEntity machine && machine.progress() > 0 && machine.processTime() > 0) {
                return List.of(new ViewGroup<>(List.of(ProgressView.create((float) machine.progress() / machine.processTime()))));
            }
            return null;
        }

        @Override
        public List<ClientViewGroup<ProgressView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, ProgressView::read, null);
        }

        @Override
        public ResourceLocation getUid() {
            return PROGRESS;
        }
    }

    /** A processing machine's status line, and an owned block's owner. */
    enum Machine implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            final Object target = accessor.getBlockEntity();
            if (target instanceof ProcessingMachineBlockEntity machine) {
                data.putInt("Status", machine.status().ordinal());
            }
            if (target instanceof LaunchControllerBlockEntity controller && !controller.ownerName().isEmpty()) {
                data.putString("Owner", controller.ownerName());
            } else if (target instanceof LaserTurretBlockEntity turret && !turret.ownerName().isEmpty()) {
                data.putString("Owner", turret.ownerName());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            final CompoundTag data = accessor.getServerData();
            if (data.contains("Status")) {
                final MachineStatus[] statuses = MachineStatus.values();
                final int status = data.getInt("Status");
                if (status >= 0 && status < statuses.length) {
                    tooltip.add(Component.translatable("gui.galacticraftcore.status").append(": ").append(statuses[status].text()));
                }
            }
            if (data.contains("Owner")) {
                tooltip.add(Component.translatable("gui.galacticraftcore.launch_controller.owner").append(": " + data.getString("Owner")));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return MACHINE;
        }
    }
}
