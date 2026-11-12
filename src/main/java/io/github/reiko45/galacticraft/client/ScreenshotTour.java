package io.github.reiko45.galacticraft.client;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.SlimelingEntity;
import io.github.reiko45.galacticraft.block.FluidPipeBlock;
import io.github.reiko45.galacticraft.block.MachineBlock;
import io.github.reiko45.galacticraft.block.ParachestBlock;
import io.github.reiko45.galacticraft.block.entity.FuelLoaderBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ParachestBlockEntity;
import io.github.reiko45.galacticraft.client.screen.CelestialSelectionScreen;
import io.github.reiko45.galacticraft.entity.LanderEntity;
import io.github.reiko45.galacticraft.entity.ParachestEntity;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.Tier1RocketItem;
import io.github.reiko45.galacticraft.menu.CargoMenu;
import io.github.reiko45.galacticraft.registry.GCEntities;
import io.github.reiko45.galacticraft.menu.NasaWorkbenchMenu;
import io.github.reiko45.galacticraft.network.GCNetwork;
import io.github.reiko45.galacticraft.travel.SpaceTravel;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import io.github.reiko45.galacticraft.player.GearData;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import io.github.reiko45.galacticraft.block.entity.FluidTankBlockEntity;
import io.github.reiko45.galacticraft.block.entity.RefineryBlockEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.block.entity.OxygenCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDecompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenDistributorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenMachineBlockEntity;
import io.github.reiko45.galacticraft.block.entity.OxygenSealerBlockEntity;
import io.github.reiko45.galacticraft.item.gear.GearSlot;
import io.github.reiko45.galacticraft.item.gear.OxygenTankItem;
import io.github.reiko45.galacticraft.menu.ExtendedInventoryMenu;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.world.GCDimensions;
import net.minecraft.client.CameraType;
import io.github.reiko45.galacticraft.registry.GCFluids;
import io.github.reiko45.galacticraft.block.entity.CircuitFabricatorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CoalGeneratorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.CompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricCompressorBlockEntity;
import io.github.reiko45.galacticraft.block.entity.ElectricFurnaceBlockEntity;
import io.github.reiko45.galacticraft.block.entity.EnergyStorageBlockEntity;
import io.github.reiko45.galacticraft.block.entity.MachineBlockEntity;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import io.github.reiko45.galacticraft.registry.GCItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import io.github.reiko45.galacticraft.compat.jei.GalacticraftJeiPlugin;
import net.neoforged.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development aid, inert unless the client is started with {@code -Dgalacticraftcore.screenshotTour=true}
 * ({@code ./gradlew runScreenshotTour}): creates a flat creative world, builds a row of machines on the integrated
 * server, opens every machine GUI and saves screenshots to {@code screenshots/}, then shows the solar and oil
 * scene and quits. Used to check GUI
 * layouts and block models without a human at the keyboard.
 */
public final class ScreenshotTour {
    private static final String PROPERTY = "galacticraftcore.screenshotTour";
    /** Viewpoints of the solar / oil scene: offset x, y, z from its origin, yaw, pitch. */
    private static final double[][] VIEWS = {{1.5, 2.5, 8.5, 180.0, 20.0}, {4.5, 2.0, 3.5, 160.0, 35.0}, {-5.0, 5.0, 6.5, 180.0, 30.0}};

    private final List<BlockPos> machines = new ArrayList<>();
    private BlockPos oilScene = BlockPos.ZERO;
    private BlockPos oxygenScene = BlockPos.ZERO;
    private BlockPos rocketScene = BlockPos.ZERO;
    private java.util.UUID rocketId;
    /** Steps after the machine GUIs, one every 30 ticks. */
    private final List<BiConsumer<Minecraft, MinecraftServer>> extras = new ArrayList<>();
    private boolean worldRequested;
    private int ticks;
    private int step;

    private ScreenshotTour() {
    }

    public static void registerIfEnabled() {
        if (Boolean.getBoolean(PROPERTY)) {
            final ScreenshotTour tour = new ScreenshotTour();
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tour.tick());
            GalacticraftCore.LOGGER.info("Screenshot tour enabled");
        }
    }

    private void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!this.worldRequested && minecraft.screen instanceof TitleScreen) {
            this.worldRequested = true;
            final String name = "galacticraft_tour_" + System.currentTimeMillis();
            minecraft.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1234L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    minecraft.screen);
            return;
        }
        final MinecraftServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null) {
            return;
        }
        this.ticks++;
        if (this.ticks == 40) {
            server.execute(() -> build(server));
        } else if (this.ticks == 160) {
            shoot(minecraft, "galacticraft_machines");
        } else if (this.ticks > 160 && (this.ticks - 160) % 30 == 0) {
            if (this.step < this.machines.size()) {
                final BlockPos pos = this.machines.get(this.step);
                server.execute(() -> {
                    final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    if (player.level().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                        // Stand next to it: menus close at once when the player is out of reach.
                        player.teleportTo(player.serverLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 3.5, 180.0F, 15.0F);
                        if (machine instanceof RefineryBlockEntity) {
                            machine.energy().setStored(machine.energy().getCapacityGJ()); // it has been refining all along
                        }
                        if (machine instanceof io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity controller) {
                            controller.openMenu(player);
                        } else {
                            player.openMenu(machine, pos);
                        }
                    }
                });
                this.step++;
            } else if (this.step - this.machines.size() < this.extras.size()) {
                this.extras.get(this.step - this.machines.size()).accept(minecraft, server);
                this.step++;
            } else {
                minecraft.stop();
            }
        } else if (this.ticks > 160 && (this.ticks - 160) % 30 == 20 && minecraft.screen != null && this.step <= this.machines.size()) {
            final String name = minecraft.screen.getTitle().getString().toLowerCase().replace(' ', '_');
            shoot(minecraft, "gui_" + name);
            server.execute(() -> server.getPlayerList().getPlayers().get(0).closeContainer());
        }
    }

    private static void shoot(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(),
                message -> GalacticraftCore.LOGGER.info("Screenshot tour: {}", message.getString()));
    }

    private void build(MinecraftServer server) {
        final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        final ServerLevel level = player.serverLevel();
        player.setGameMode(GameType.CREATIVE);
        // Hover where the views are taken instead of dropping to the ground after each teleport.
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        level.setDayTime(6000);
        final BlockPos base = player.blockPosition().offset(0, 0, -5);
        player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 6.5, 180.0F, 15.0F);

        // Front row, all facing the camera (south).
        final Block[] row = {GCBlocks.COAL_GENERATOR.get(), GCBlocks.ENERGY_STORAGE_MODULE.get(), GCBlocks.COMPRESSOR.get(),
                GCBlocks.ELECTRIC_COMPRESSOR.get(), GCBlocks.ELECTRIC_FURNACE.get(), GCBlocks.CIRCUIT_FABRICATOR.get(),
                GCBlocks.ENERGY_STORAGE_CLUSTER.get(), GCBlocks.ADVANCED_COMPRESSOR.get(), GCBlocks.ELECTRIC_ARC_FURNACE.get()};
        for (int i = 0; i < row.length; i++) {
            final BlockPos pos = base.offset(i - row.length / 2, 0, 0);
            level.setBlockAndUpdate(pos, row[i].defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            this.machines.add(pos);
        }
        // A powered setup on the row above: Storage Module (output faces west) -> wires -> Electric Furnace.
        final BlockPos module = base.offset(2, 2, 0);
        level.setBlockAndUpdate(module, GCBlocks.ENERGY_STORAGE_MODULE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        for (int dx = 1; dx <= 3; dx++) {
            level.setBlockAndUpdate(module.west(dx), GCBlocks.ALUMINUM_WIRE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(module.west(3).above(), GCBlocks.HEAVY_ALUMINUM_WIRE.get().defaultBlockState());
        final BlockPos furnace = module.west(4);
        level.setBlockAndUpdate(furnace, GCBlocks.ELECTRIC_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(base.offset(-6, -1, 0), Blocks.STONE.defaultBlockState());

        if (level.getBlockEntity(module) instanceof EnergyStorageBlockEntity storage) {
            storage.energy().setStored(400_000.0F);
        }
        if (level.getBlockEntity(furnace) instanceof ElectricFurnaceBlockEntity powered) {
            powered.inventory().setStackInSlot(1, new ItemStack(Items.RAW_IRON, 32));
        }
        // Give the GUIs something to show.
        fill(level, this.machines.get(0), be -> {
            if (be instanceof CoalGeneratorBlockEntity generator) {
                generator.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 16));
            }
        });
        fill(level, this.machines.get(1), be -> be.energy().setStored(250_000.0F));
        fill(level, this.machines.get(2), be -> {
            if (be instanceof CompressorBlockEntity compressor) {
                compressor.inventory().setStackInSlot(0, new ItemStack(Items.COAL, 8));
                compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START, new ItemStack(Items.IRON_INGOT, 8));
                compressor.inventory().setStackInSlot(CompressorBlockEntity.GRID_START + 1, new ItemStack(Items.IRON_INGOT, 8));
            }
        });
        fill(level, this.machines.get(3), be -> {
            if (be instanceof ElectricCompressorBlockEntity compressor) {
                compressor.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
                compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START, new ItemStack(GCItems.TIN_INGOT.get(), 16));
                compressor.inventory().setStackInSlot(ElectricCompressorBlockEntity.GRID_START + 1, new ItemStack(GCItems.TIN_INGOT.get(), 16));
            }
        });
        fill(level, this.machines.get(5), be -> {
            if (be instanceof CircuitFabricatorBlockEntity fabricator) {
                fabricator.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
                fabricator.inventory().setStackInSlot(1, new ItemStack(Items.DIAMOND, 4));
                fabricator.inventory().setStackInSlot(2, new ItemStack(GCItems.RAW_SILICON.get(), 4));
                fabricator.inventory().setStackInSlot(3, new ItemStack(GCItems.RAW_SILICON.get(), 4));
                fabricator.inventory().setStackInSlot(4, new ItemStack(Items.REDSTONE, 4));
                fabricator.inventory().setStackInSlot(5, new ItemStack(Items.LAPIS_LAZULI, 4));
            }
        });
        this.machines.add(furnace);
        buildOilScene(level, base.offset(0, 0, -9));
        buildOxygenScene(level, base.offset(0, 0, -20));
        buildRocketScene(level, base.offset(0, 0, -34));

        // The solar / oil scene from a few viewpoints: teleport, then shoot on the next step.
        for (int i = 0; i < VIEWS.length; i++) {
            final double[] v = VIEWS[i];
            final String name = "galacticraft_oil_and_solar_" + i;
            this.extras.add((minecraft, srv) -> srv.execute(() -> {
                final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
                p.teleportTo(p.serverLevel(), this.oilScene.getX() + v[0], this.oilScene.getY() + v[1], this.oilScene.getZ() + v[2],
                        (float) v[3], (float) v[4]);
            }));
            this.extras.add((minecraft, srv) -> shoot(minecraft, name));
        }
        // Oxygen machines from the front
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            p.teleportTo(p.serverLevel(), this.oxygenScene.getX() + 2.5, this.oxygenScene.getY() + 2.5, this.oxygenScene.getZ() + 9.5, 180.0F, 15.0F);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "galacticraft_oxygen"));
        // The Galacticraft inventory tab, wearing a full oxygen setup
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            final GCPlayerData data = GCPlayerData.get(p);
            data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
            data.setGear(GearSlot.GEAR, new ItemStack(GCItems.OXYGEN_GEAR.get()));
            data.setGear(GearSlot.LEFT_TANK, GCItems.HEAVY_OXYGEN_TANK.get().full());
            data.setGear(GearSlot.RIGHT_TANK, GCItems.MEDIUM_OXYGEN_TANK.get().full());
            data.setGear(GearSlot.PARACHUTE, new ItemStack(GCItems.PARACHUTES.get(13).get()));
            data.setGear(GearSlot.FREQUENCY_MODULE, new ItemStack(GCItems.FREQUENCY_MODULE.get()));
            ExtendedInventoryMenu.open(p);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_galacticraft_inventory"));
        this.extras.add((minecraft, srv) -> {
            minecraft.player.closeContainer();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "player_gear_front"));
        this.extras.add((minecraft, srv) -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "player_gear_back"));
        // On the Moon: a sealed glass room (torch inside burns, the one outside doesn't), the HUD, then the warning
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> buildMoonScene(srv));
        });
        for (int i = 0; i < 9; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // let the sealer run its first check (126 ticks after placing)
        }
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_oxygen_hud"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            GCPlayerData.get(p).setGear(GearSlot.MASK, ItemStack.EMPTY);
        }));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_oxygen_warning"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> srv.getPlayerList().getPlayers().get(0).setGameMode(GameType.CREATIVE)));
        moonSteps();
        dungeonSteps();
        rocketSteps();
        // JEI recipe categories, when JEI is installed.
        if (ModList.get().isLoaded("jei")) {
            final String[] categories = {"compressing", "circuit_fabricating", "refining"};
            for (int i = 0; i < categories.length; i++) {
                final int category = i;
                this.extras.add((minecraft, srv) -> {
                    if (!GalacticraftJeiPlugin.showCategory(category)) {
                        GalacticraftCore.LOGGER.warn("Screenshot tour: JEI runtime not available");
                    }
                });
                this.extras.add((minecraft, srv) -> {
                    shoot(minecraft, "jei_" + categories[category]);
                    minecraft.setScreen(null);
                });
            }
        }
    }

    /**
     * A launch pad with a Tier 1 Rocket and a Fuel Loader, a NASA Workbench, a Para-chest and a landed lander. The Fuel
     * Loader GUI is shot with the machines.
     */
    private void buildRocketScene(ServerLevel level, BlockPos origin) {
        this.rocketScene = origin;
        for (int x = -9; x <= 10; x++) {
            for (int z = -4; z <= 4; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(origin.offset(dx, 0, dz), GCBlocks.LANDING_PAD.get().defaultBlockState());
            }
        }
        final BlockPos loader = origin.offset(2, 0, 1);
        level.setBlockAndUpdate(loader, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(loader) instanceof FuelLoaderBlockEntity fuelLoader) {
            fuelLoader.energy().setStored(16_000.0F);
            fuelLoader.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            fuelLoader.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
        }
        this.machines.add(loader);
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(level, origin.getX() + 0.5D, origin.getY() + 0.4D, origin.getZ() + 0.5D, 3, 0);
        rocket.setYRot(180.0F);
        level.addFreshEntity(rocket);
        this.rocketId = rocket.getUUID();
        final BlockPos bench = origin.offset(-5, 0, 0);
        level.setBlockAndUpdate(bench, GCBlocks.NASA_WORKBENCH.get().defaultBlockState());
        GCBlocks.NASA_WORKBENCH.get().setPlacedBy(level, bench, level.getBlockState(bench), null, new ItemStack(GCBlocks.NASA_WORKBENCH.get()));
        final BlockPos chest = origin.offset(4, 0, 2);
        level.setBlockAndUpdate(chest, GCBlocks.PARACHEST.get().defaultBlockState().setValue(ParachestBlock.COLOR, 13));
        if (level.getBlockEntity(chest) instanceof ParachestBlockEntity parachest) {
            final NonNullList<ItemStack> cargo = NonNullList.withSize(20, ItemStack.EMPTY);
            cargo.set(0, new ItemStack(Items.DIAMOND, 5));
            cargo.set(4, new ItemStack(GCItems.CANNED_BEEF.get(), 12));
            cargo.set(18, new ItemStack(GCBlocks.LANDING_PAD.get(), 9));
            cargo.set(19, Tier1RocketItem.create(1, 0));
            parachest.fill(cargo, 2_500);
        }
        final LanderEntity lander = LanderEntity.create(level, origin.getX() + 8.0D, origin.getY(), origin.getZ() - 1.0D,
                NonNullList.withSize(20, ItemStack.EMPTY), 1_000);
        lander.setOnGround(true);
        lander.setYRot(200.0F);
        level.addFreshEntity(lander);
    }

    private static int surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4); // getHeight only reads loaded chunks (unloaded ones give the bottom of the world)
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /**
     * On the Moon: a footprint trail walked by the real stepping code, a hot and a cold Fallen Meteor, a meteor coming
     * down, the sky at noon and at midnight, and a cave.
     */
    private void moonSteps() {
        // Footprints: walk east across the turf
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (moon == null) {
                return;
            }
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = false;
            final GCPlayerData data = GCPlayerData.get(p);
            final double z = 9.5D;
            double x = 8.5D;
            p.teleportTo(moon, x, surface(moon, (int) x, (int) z), z, -90.0F, 0.0F);
            data.lastFootX = p.getX();
            data.lastFootZ = p.getZ();
            for (int i = 0; i < 45; i++) {
                x += 0.3D;
                p.teleportTo(moon, x, surface(moon, (int) Math.floor(x), (int) z), z + Math.sin(i / 6.0D) * 0.6D, -90.0F, 0.0F);
                io.github.reiko45.galacticraft.footprint.Footprints.step(p, data);
            }
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(moon, 6.5D, surface(moon, 6, 12) + 2.5D, 12.5D, -125.0F, 38.0F);
            // Items fall slowly here (logged as a check of the gravity mixin)
            final net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(moon, 0, 100, 0, new ItemStack(Items.STONE));
            GalacticraftCore.LOGGER.info("Screenshot tour: item gravity on the Moon {}, arrow gravity {}", item.getGravity(),
                    new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, moon).getGravity());
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_footprints"));
        // Fallen meteors: one hot from the sky, one placed by hand (cold); and a meteor on its way down
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (moon == null) {
                return;
            }
            final BlockPos hot = new BlockPos(14, surface(moon, 14, -12), -12);
            final BlockPos cold = new BlockPos(16, surface(moon, 16, -12), -12);
            moon.setBlockAndUpdate(hot, GCBlocks.FALLEN_METEOR.get().defaultBlockState());
            moon.setBlockAndUpdate(cold, GCBlocks.FALLEN_METEOR.get().defaultBlockState());
            GCBlocks.FALLEN_METEOR.get().setPlacedBy(moon, cold, moon.getBlockState(cold), p, new ItemStack(GCBlocks.FALLEN_METEOR.get()));
            p.teleportTo(moon, 15.0D, hot.getY() + 2.0D, -8.5D, 180.0F, 20.0F);
            // 30 ticks from now it has fallen 18 blocks to about eye level, 9 blocks ahead
            moon.addFreshEntity(io.github.reiko45.galacticraft.entity.MeteorEntity.create(moon, 12.5D, hot.getY() + 2.0D + 18.0D, -17.5D,
                    0.0D, 0.0D, 0.0D, 6));
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_meteors"));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_meteor_crater"));
        // The sky: Earth overhead to the south, the sun, and stars even at noon
        final long[] times = {6000L, 18000L};
        final String[] names = {"moon_sky_noon", "moon_sky_midnight"};
        for (int i = 0; i < times.length; i++) {
            final long time = times[i];
            final String name = names[i];
            this.extras.add((minecraft, srv) -> srv.execute(() -> {
                final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
                if (moon != null) {
                    srv.overworld().setDayTime(time); // other worlds share the Overworld's clock
                    final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
                    p.teleportTo(moon, 2.5D, surface(moon, 2, 20) + 2.0D, 20.5D, 10.0F, -55.0F);
                }
            }));
            this.extras.add((minecraft, srv) -> shoot(minecraft, name));
        }
        // A cave, lit by night vision
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (moon == null) {
                return;
            }
            srv.overworld().setDayTime(6000L);
            final BlockPos cave = findCave(moon);
            if (cave == null) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no Moon cave found near spawn");
                return;
            }
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 200, 0, false, false));
            p.teleportTo(moon, cave.getX() + 0.5D, cave.getY(), cave.getZ() + 0.5D, 45.0F, 10.0F);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_cave"));
    }

    private BlockPos dungeonBoss;
    private BlockPos dungeonTreasure;
    private BlockPos dungeonCrater;

    private static ServerPlayer tourPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    /**
     * Phase 5b: the evolved mobs and the Skeleton Boss in a row; the nearest Moon dungeon found by the real structure
     * placement (crater, boss room with the boss woken up, treasure room); the boss's death filling the treasure chest;
     * the NASA Workbench pages (Add New Schematic, Moon Buggy); and buggies on a Buggy Fueling Pad, with one driven.
     */
    private void dungeonSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null) {
                return;
            }
            srv.setDifficulty(Difficulty.NORMAL, true); // monsters vanish on Peaceful
            srv.overworld().setDayTime(18000L); // zombies and skeletons burn by day (until the airless Moon puts them out)
            final ServerPlayer p = tourPlayer(srv);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
            final net.minecraft.world.entity.EntityType<?>[] types = {GCEntities.EVOLVED_ZOMBIE.get(), GCEntities.EVOLVED_SKELETON.get(),
                    GCEntities.EVOLVED_CREEPER.get(), GCEntities.EVOLVED_SPIDER.get(), GCEntities.EVOLVED_ENDERMAN.get(), GCEntities.SKELETON_BOSS.get()};
            for (int i = 0; i < types.length; i++) {
                final int x = -30 + i * 3;
                final net.minecraft.world.entity.Entity mob = types[i].create(moon);
                if (mob instanceof net.minecraft.world.entity.Mob living) {
                    living.moveTo(x + 0.5D, surface(moon, x, -30), -29.5D, 180.0F, 0.0F);
                    living.setYHeadRot(180.0F);
                    living.setYBodyRot(180.0F);
                    living.setNoAi(true);
                    living.setPersistenceRequired();
                    if (living instanceof io.github.reiko45.galacticraft.entity.SkeletonBossEntity) {
                        living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    }
                    moon.addFreshEntity(living);
                }
            }
            p.teleportTo(moon, -22.5D, surface(moon, -22, -38) + 2.5D, -39.5D, 0.0F, 12.0F);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_evolved_mobs"));
        // The tour world has structures off, so build the dungeon of the first 44x44 chunk region where the structure
        // set puts it, like /place structure does, then look down its crater
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null) {
                return;
            }
            final var structure = moon.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolderOrThrow(io.github.reiko45.galacticraft.data.GCWorldgenProvider.MOON_DUNGEON);
            final var generator = moon.getChunkSource().getGenerator();
            final var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)
                    moon.getChunkSource().getGeneratorState().getPlacementsForStructure(structure).get(0);
            final net.minecraft.world.level.ChunkPos chunk = placement.getPotentialStructureChunk(moon.getSeed(), 0, 0);
            final net.minecraft.world.level.levelgen.structure.StructureStart start = structure.value().generate(moon.registryAccess(), generator,
                    generator.getBiomeSource(), moon.getChunkSource().randomState(), moon.getStructureManager(), moon.getSeed(), chunk, 0, moon,
                    biome -> true);
            if (!start.isValid()) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no Moon dungeon start at {}", chunk);
                return;
            }
            final var box = start.getBoundingBox();
            net.minecraft.world.level.ChunkPos.rangeClosed(new net.minecraft.world.level.ChunkPos(box.minX() >> 4, box.minZ() >> 4),
                    new net.minecraft.world.level.ChunkPos(box.maxX() >> 4, box.maxZ() >> 4)).forEach(pos -> {
                moon.getChunk(pos.x, pos.z);
                start.placeInChunk(moon, moon.structureManager(), generator, moon.getRandom(), new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        pos.getMinBlockX(), moon.getMinBuildHeight(), pos.getMinBlockZ(), pos.getMaxBlockX(), moon.getMaxBuildHeight(), pos.getMaxBlockZ()), pos);
            });
            final BlockPos found = chunk.getMiddleBlockPosition(io.github.reiko45.galacticraft.world.dungeon.DungeonConfig.MOON.y());
            for (var piece : start.getPieces()) {
                if (piece instanceof io.github.reiko45.galacticraft.world.dungeon.DungeonPiece room) {
                    final BlockPos centre = room.getBoundingBox().getCenter();
                    switch (room.kind()) {
                        case CRATER -> this.dungeonCrater = centre;
                        case BOSS -> this.dungeonBoss = new BlockPos(centre.getX(), room.getBoundingBox().minY() + 1, centre.getZ());
                        case TREASURE -> {
                            this.dungeonTreasure = new BlockPos(centre.getX(), room.getBoundingBox().minY() + 1, centre.getZ());
                            for (BlockPos pos : BlockPos.betweenClosed(room.getBoundingBox().minX(), room.getBoundingBox().minY(), room.getBoundingBox().minZ(),
                                    room.getBoundingBox().maxX(), room.getBoundingBox().maxY(), room.getBoundingBox().maxZ())) {
                                if (moon.getBlockState(pos).is(GCBlocks.TREASURE_CHEST_TIER_1.get())) {
                                    this.dungeonTreasure = pos.immutable();
                                }
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
            GalacticraftCore.LOGGER.info("Screenshot tour: Moon dungeon at {} ({} pieces), boss room {}, treasure {}", found,
                    start.getPieces().size(), this.dungeonBoss, this.dungeonTreasure);
            if (this.dungeonCrater != null) {
                final ServerPlayer p = tourPlayer(srv);
                final int camX = this.dungeonCrater.getX() + 20;
                int top = 0;
                for (int dx = -2; dx <= 2; dx++) {
                    top = Math.max(top, surface(moon, camX + dx, this.dungeonCrater.getZ()));
                }
                p.teleportTo(moon, camX + 0.5D, top + 10.0D, this.dungeonCrater.getZ() + 0.5D, 90.0F, 35.0F);
            }
        }));
        for (int i = 0; i < 4; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // chunks around the dungeon load and generate
        }
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_dungeon_crater"));
        // Into the boss room: entering wakes the boss
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null || this.dungeonBoss == null) {
                return;
            }
            final ServerPlayer p = tourPlayer(srv);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200, 0, false, false));
            p.teleportTo(moon, this.dungeonBoss.getX() + 0.5D, this.dungeonBoss.getY() + 1.0D, this.dungeonBoss.getZ() + 6.5D, 180.0F, 5.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null || this.dungeonBoss == null) {
                return;
            }
            // Hold the boss still facing the camera for the picture
            for (var boss : moon.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.SkeletonBossEntity.class,
                    new net.minecraft.world.phys.AABB(this.dungeonBoss).inflate(20.0D))) {
                boss.setNoAi(true);
                boss.moveTo(this.dungeonBoss.getX() + 0.5D, this.dungeonBoss.getY(), this.dungeonBoss.getZ() + 0.5D, 0.0F, 0.0F);
                boss.setYHeadRot(0.0F);
                boss.setYBodyRot(0.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_dungeon_boss"));
        // The boss dies (the player stays in its room, or it would leave) and fills the treasure chest
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null || this.dungeonBoss == null) {
                return;
            }
            for (var boss : moon.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.SkeletonBossEntity.class,
                    new net.minecraft.world.phys.AABB(this.dungeonBoss).inflate(20.0D))) {
                boss.setNoAi(false);
                boss.kill();
            }
        }));
        for (int i = 0; i < 7; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // 200 ticks of dying
        }
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null || this.dungeonTreasure == null) {
                return;
            }
            final ServerPlayer p = tourPlayer(srv);
            p.teleportTo(moon, this.dungeonTreasure.getX() + 0.5D, this.dungeonTreasure.getY() + 0.5D, this.dungeonTreasure.getZ() + 3.0D, 180.0F, 25.0F);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "moon_dungeon_treasure"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null || this.dungeonTreasure == null) {
                return;
            }
            final ServerPlayer p = tourPlayer(srv);
            if (moon.getBlockEntity(this.dungeonTreasure) instanceof io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity chest) {
                boolean schematic = false;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    schematic |= chest.getItem(slot).getItem() instanceof io.github.reiko45.galacticraft.item.SchematicItem;
                }
                GalacticraftCore.LOGGER.info("Screenshot tour: treasure chest locked {}, schematic inside {}", chest.isLocked(), schematic);
                chest.setLocked(false); // what the Tier 1 Dungeon Key does
                p.teleportTo(moon, this.dungeonTreasure.getX() + 0.5D, this.dungeonTreasure.getY(), this.dungeonTreasure.getZ() + 2.5D, 180.0F, 30.0F);
                p.openMenu(chest);
            } else {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no treasure chest at {}", this.dungeonTreasure);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_treasure_chest"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            tourPlayer(srv).closeContainer();
            srv.setDifficulty(Difficulty.PEACEFUL, true);
            srv.overworld().setDayTime(6000L);
        }));
        buggySteps();
    }

    private BlockPos buggyScene = BlockPos.ZERO;

    private void logBuggies(Minecraft minecraft, MinecraftServer srv, String when) {
        final ServerPlayer p = tourPlayer(srv);
        GalacticraftCore.LOGGER.info("Screenshot tour ({}): server player {} riding {}, client player {} riding {}", when, p.position(),
                p.getVehicle(), minecraft.player == null ? null : minecraft.player.position(), minecraft.player == null ? null : minecraft.player.getVehicle());
    }

    private void buggySteps() {
        // A Buggy Fueling Pad with a Fuel Loader and a buggy on it, two more buggies with storage boxes
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
            if (moon == null) {
                return;
            }
            final int y = surface(moon, 40, 0);
            this.buggyScene = new BlockPos(40, y, 0);
            for (int x = -8; x <= 12; x++) {
                for (int z = -8; z <= 14; z++) {
                    for (int dy = -2; dy <= 10; dy++) {
                        moon.setBlockAndUpdate(this.buggyScene.offset(x, dy, z), dy < 0 ? GCBlocks.MOON_TURF.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    moon.setBlockAndUpdate(this.buggyScene.offset(dx, 0, dz), GCBlocks.BUGGY_FUELING_PAD.get().defaultBlockState());
                }
            }
            final BlockPos loader = this.buggyScene.offset(-2, 0, 1);
            moon.setBlockAndUpdate(loader, GCBlocks.FUEL_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (moon.getBlockEntity(loader) instanceof FuelLoaderBlockEntity fuelLoader) {
                fuelLoader.energy().setStored(16_000.0F);
                fuelLoader.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            }
            final double[][] buggies = {{0.5, 0.2, 0.5, 200.0, 0}, {5.0, 0.0, -2.5, 150.0, 2}, {6.0, 0.0, 3.0, 100.0, 3}};
            for (double[] b : buggies) {
                final var buggy = io.github.reiko45.galacticraft.entity.BuggyEntity.create(moon, this.buggyScene.getX() + b[0],
                        this.buggyScene.getY() + b[1], this.buggyScene.getZ() + b[2], (int) b[4], 0);
                buggy.setYRot((float) b[3]);
                moon.addFreshEntity(buggy);
            }
            final ServerPlayer p = tourPlayer(srv);
            p.getInventory().setItem(3, io.github.reiko45.galacticraft.item.BuggyItem.create(3, 0));
            p.getInventory().setItem(4, new ItemStack(GCBlocks.BUGGY_FUELING_PAD.get(), 9));
            p.getInventory().setItem(5, new ItemStack(GCItems.BUGGY_SCHEMATIC.get()));
            p.getInventory().setItem(6, new ItemStack(GCItems.TIER_1_DUNGEON_KEY.get()));
            p.getInventory().setItem(7, new ItemStack(GCBlocks.TREASURE_CHEST_TIER_1.get()));
            p.getInventory().setItem(8, new ItemStack(GCItems.SKELETON_BOSS_SPAWN_EGG.get()));
            p.teleportTo(moon, this.buggyScene.getX() + 3.0D, this.buggyScene.getY() + 4.0D, this.buggyScene.getZ() + 11.0D, 165.0F, 22.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive after the long teleport
        this.extras.add((minecraft, srv) -> {
        }); // (still arriving)
        this.extras.add((minecraft, srv) -> {
            logBuggies(minecraft, srv, "scene");
            shoot(minecraft, "moon_buggies");
        });
        // Drive the buggy off the pad (the Fuel Loader has filled it)
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            srv.execute(() -> {
                final ServerLevel moon = srv.getLevel(GCDimensions.MOON);
                final ServerPlayer p = tourPlayer(srv);
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                for (var buggy : moon.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.BuggyEntity.class,
                        new net.minecraft.world.phys.AABB(this.buggyScene).inflate(1.0D))) {
                    GalacticraftCore.LOGGER.info("Screenshot tour: buggy on the fueling pad has {} mB of fuel", buggy.fuel());
                    p.startRiding(buggy, true);
                }
            });
        });
        this.extras.add((minecraft, srv) -> minecraft.options.keyUp.setDown(true)); // hold W: accelerate
        this.extras.add((minecraft, srv) -> minecraft.options.keyLeft.setDown(true)); // and turn left a little
        this.extras.add((minecraft, srv) -> minecraft.options.keyLeft.setDown(false));
        this.extras.add((minecraft, srv) -> {
            logBuggies(minecraft, srv, "driving");
            shoot(minecraft, "moon_buggy_driving");
        });
        this.extras.add((minecraft, srv) -> minecraft.options.keyUp.setDown(false));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            if (p.getVehicle() instanceof io.github.reiko45.galacticraft.entity.BuggyEntity buggy) {
                GalacticraftCore.LOGGER.info("Screenshot tour: buggy driven to {} with {} mB left", buggy.blockPosition(), buggy.fuel());
                CargoMenu.openBuggy(p, buggy);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_buggy"));
        // The workbench pages: Add New Schematic with the buggy schematic in, then the unlocked Moon Buggy page
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                p.closeContainer();
                p.stopRiding();
                final BlockPos bench = this.rocketScene.offset(-5, 0, 0);
                p.teleportTo(srv.overworld(), bench.getX() + 0.5, bench.getY(), bench.getZ() + 3.5, 180.0F, 15.0F);
                io.github.reiko45.galacticraft.menu.Schematics.open(p, io.github.reiko45.galacticraft.menu.Schematics.ADD_SCHEMATIC, srv.overworld(), bench);
                p.containerMenu.getSlot(0).set(new ItemStack(GCItems.BUGGY_SCHEMATIC.get()));
            });
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_add_new_schematic"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            io.github.reiko45.galacticraft.menu.Schematics.action(p, io.github.reiko45.galacticraft.menu.Schematics.ACTION_UNLOCK);
            io.github.reiko45.galacticraft.menu.Schematics.action(p, io.github.reiko45.galacticraft.menu.Schematics.ACTION_BACK);
            if (p.containerMenu instanceof io.github.reiko45.galacticraft.menu.BuggyBenchMenu) {
                for (int i = 1; i <= 19; i++) {
                    // menu slots go row by row, the 1.12.2 matrix numbers go down the columns
                    final int index = p.containerMenu.getSlot(i).getContainerSlot();
                    final Item part = index == io.github.reiko45.galacticraft.menu.BuggyBenchMenu.SEAT ? GCItems.BUGGY_SEAT.get()
                            : index <= 12 ? GCItems.HEAVY_PLATING.get() : index <= 16 ? GCItems.BUGGY_WHEEL.get() : GCItems.BUGGY_STORAGE_BOX.get();
                    p.containerMenu.getSlot(i).set(new ItemStack(part));
                }
            } else {
                GalacticraftCore.LOGGER.warn("Screenshot tour: Back from Add New Schematic opened {}", p.containerMenu);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_moon_buggy"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> tourPlayer(srv).closeContainer()));
    }

    /** The cave space closest to the Moon's spawn with room to stand: air with rock below and air above, under the surface. */
    @org.jetbrains.annotations.Nullable
    private static BlockPos findCave(ServerLevel moon) {
        BlockPos best = null;
        for (int x = -48; x <= 48; x++) {
            for (int z = -48; z <= 48; z++) {
                final int top = surface(moon, x, z) - 6;
                for (int y = 8; y < top; y++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (moon.getBlockState(pos).isAir() && moon.getBlockState(pos.above()).isAir() && moon.getBlockState(pos.above(2)).isAir()
                            && !moon.getBlockState(pos.below()).isAir() && (best == null || x * x + z * z < best.getX() * best.getX() + best.getZ() * best.getZ())) {
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /** The scene's rocket (looked up again: its chunk may have been unloaded while the tour was on the Moon). */
    private Tier1RocketEntity rocket(MinecraftServer server) {
        return server.overworld().getEntity(this.rocketId) instanceof Tier1RocketEntity rocket ? rocket : null;
    }

    /** The rocket scene, its GUIs, and a flight: countdown, lift-off, destination screen, lander descent to the Moon. */
    private void rocketSteps() {
        final double[][] views = {{0.5, 3.0, 10.5, 180.0, 15.0}, {-9.0, 4.0, 8.0, 225.0, 20.0}, {4.5, 1.2, 4.6, 180.0, 40.0}};
        for (int i = 0; i < views.length; i++) {
            final double[] v = views[i];
            final String name = "galacticraft_rockets_" + i;
            this.extras.add((minecraft, srv) -> srv.execute(() -> {
                final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
                p.teleportTo(srv.overworld(), this.rocketScene.getX() + v[0], this.rocketScene.getY() + v[1], this.rocketScene.getZ() + v[2],
                        (float) v[3], (float) v[4]);
                if (v == views[0]) {
                    // Item icons in the hotbar
                    p.getInventory().setItem(5, Tier1RocketItem.create(1, 0));
                    p.getInventory().setItem(6, new ItemStack(GCBlocks.NASA_WORKBENCH.get()));
                    p.getInventory().setItem(7, new ItemStack(GCBlocks.LANDING_PAD.get(), 9));
                    p.getInventory().setItem(8, new ItemStack(GCBlocks.PARACHEST.get()));
                    // A para-chest coming down next to the landed one
                    p.serverLevel().addFreshEntity(ParachestEntity.create(p.serverLevel(), this.rocketScene.getX() + 6.5D,
                            this.rocketScene.getY() + 11.0D, this.rocketScene.getZ() + 0.5D, NonNullList.withSize(2, ItemStack.EMPTY), 0, 5));
                }
            }));
            this.extras.add((minecraft, srv) -> shoot(minecraft, name));
        }
        // NASA Workbench with every part in place
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            final BlockPos bench = this.rocketScene.offset(-5, 0, 0);
            p.teleportTo(p.serverLevel(), bench.getX() + 0.5, bench.getY(), bench.getZ() + 3.5, 180.0F, 15.0F);
            io.github.reiko45.galacticraft.menu.Schematics.open(p, io.github.reiko45.galacticraft.menu.Schematics.ROCKET_T1, p.serverLevel(), bench);
            final Item[] parts = {null, GCItems.NOSE_CONE.get(), GCItems.HEAVY_PLATING.get(), GCItems.HEAVY_PLATING.get(),
                    GCItems.HEAVY_PLATING.get(), GCItems.HEAVY_PLATING.get(), GCItems.HEAVY_PLATING.get(), GCItems.HEAVY_PLATING.get(),
                    GCItems.HEAVY_PLATING.get(), GCItems.HEAVY_PLATING.get(), GCItems.ROCKET_FINS.get(), GCItems.ROCKET_FINS.get(),
                    GCItems.TIER_1_ROCKET_ENGINE.get(), GCItems.ROCKET_FINS.get(), GCItems.ROCKET_FINS.get(), Items.CHEST, Items.CHEST};
            for (int i = 1; i < parts.length; i++) {
                p.containerMenu.getSlot(i).set(new ItemStack(parts[i]));
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_nasa_workbench"));
        // Para-chest contents
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            p.closeContainer();
            final BlockPos chest = this.rocketScene.offset(4, 0, 2);
            p.teleportTo(p.serverLevel(), chest.getX() + 0.5, chest.getY(), chest.getZ() + 3.5, 180.0F, 15.0F);
            if (p.serverLevel().getBlockEntity(chest) instanceof ParachestBlockEntity parachest) {
                parachest.open(p);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_parachest"));
        // Into the rocket: its cargo screen (the Fuel Loader has been filling it), then ignition
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            p.closeContainer();
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            GCPlayerData.get(p).setGear(GearSlot.PARACHUTE, new ItemStack(GCItems.PARACHUTES.get(13).get()));
            final Tier1RocketEntity rocket = rocket(srv);
            if (rocket != null) {
                // The Fuel Loader only works while its chunk is loaded; make sure there is enough for the flight
                rocket.setFuel(Math.max(rocket.fuel(), 600));
                p.startRiding(rocket, true);
                CargoMenu.openRocket(p, rocket);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_rocket_cargo"));
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            srv.execute(() -> {
                final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
                p.closeContainer();
                SpaceTravel.ignite(p);
            });
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "rocket_countdown"));
        for (int i = 0; i < 13; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // the rest of the 400 tick countdown
        }
        this.extras.add((minecraft, srv) -> shoot(minecraft, "rocket_launch"));
        // Skip the long climb: put the rocket just under the edge of space
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final Tier1RocketEntity rocket = rocket(srv);
            if (rocket != null) {
                rocket.setPos(rocket.getX(), Tier1RocketEntity.EXIT_HEIGHT - 2.0D, rocket.getZ());
            }
        }));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_selection"));
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof CelestialSelectionScreen selection) {
                selection.select(GCDimensions.MOON);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_selection_selected"));
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null);
            PacketDistributor.sendToServer(new GCNetwork.ChooseDestination(GCDimensions.MOON.location()));
        });
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "lander_descent"));
        // Back to Earth instead: out of the lander, into space again, choose the Overworld; the parachute opens
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
            if (p.getVehicle() != null) {
                p.getVehicle().discard();
            }
            final GCPlayerData data = GCPlayerData.get(p);
            data.inSpace = true;
            data.spacePos = p.position();
            data.launchedFromX = this.rocketScene.getX() + 0.5D;
            data.launchedFromZ = this.rocketScene.getZ() + 0.5D;
            SpaceTravel.choose(p, net.minecraft.world.level.Level.OVERWORLD);
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "parachute_descent"));
        this.extras.add((minecraft, srv) -> minecraft.options.setCameraType(CameraType.FIRST_PERSON));
        stationSteps();
    }

    /**
     * Phase 6: from the destination screen, build a space station (the Create panel), fly to it, look round inside and
     * outside with the Earth below, then fix two Spin Thrusters to it and let it spin up.
     */
    private void stationSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            final GCPlayerData data = GCPlayerData.get(p);
            data.usingParachute = false;
            data.inSpace = true;
            data.spacePos = p.position();
            data.launchedFromX = this.rocketScene.getX() + 0.5D;
            data.launchedFromZ = this.rocketScene.getZ() + 0.5D;
            GCNetwork.sendToPlayer(p, SpaceTravel.selectionPayload(p));
        }));
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof CelestialSelectionScreen selection) {
                selection.select(net.minecraft.world.level.Level.OVERWORLD);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_station_create"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> SpaceTravel.createStation(tourPlayer(srv))));
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof CelestialSelectionScreen selection) {
                selection.select(GCDimensions.ORBIT);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_station"));
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null);
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                final var station = io.github.reiko45.galacticraft.world.station.SpaceStations.get(srv).ownedBy(p.getUUID());
                if (station == null) {
                    GalacticraftCore.LOGGER.warn("Screenshot tour: no space station was built");
                    return;
                }
                this.stationBase = station.base();
                SpaceTravel.choose(p, GCDimensions.ORBIT, station.id());
            });
        });
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        // Inside: the arrival block in the room, the bay beyond
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel orbit = srv.getLevel(GCDimensions.ORBIT);
            if (orbit != null && this.stationBase != null) {
                tourPlayer(srv).teleportTo(orbit, this.stationBase.getX() + 3.5D, this.stationBase.getY(), this.stationBase.getZ() + 0.5D, 90.0F, 10.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "station_inside"));
        // Outside, floating off the station's south side with the Earth below
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel orbit = srv.getLevel(GCDimensions.ORBIT);
            if (orbit != null && this.stationBase != null) {
                tourPlayer(srv).teleportTo(orbit, this.stationBase.getX() - 1.5D, this.stationBase.getY() + 5.0D, this.stationBase.getZ() + 16.5D, 180.0F, 30.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> {
            logFreefall(minecraft, "exterior");
            shoot(minecraft, "station_outside");
        });
        // Two Spin Thrusters on the south wall, both the same way: the station spins up
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel orbit = srv.getLevel(GCDimensions.ORBIT);
            if (orbit == null || this.stationBase == null) {
                return;
            }
            final BlockPos corner = this.stationBase.offset(io.github.reiko45.galacticraft.world.station.StationStructure.HULL_OFFSET);
            for (BlockPos pos : new BlockPos[]{corner.offset(7, 3, 7), corner.offset(13, 3, 7)}) {
                orbit.setBlockAndUpdate(pos, GCBlocks.SPIN_THRUSTER.get().defaultBlockState()
                        .setValue(io.github.reiko45.galacticraft.block.SpinThrusterBlock.FACING, Direction.SOUTH));
            }
            tourPlayer(srv).getInventory().setItem(2, new ItemStack(GCBlocks.SPIN_THRUSTER.get()));
        }));
        for (int i = 0; i < 3; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // spinning up
        }
        this.extras.add((minecraft, srv) -> {
            final var spin = io.github.reiko45.galacticraft.world.station.StationSpin.Client.at(this.stationBase == null ? BlockPos.ZERO : this.stationBase);
            GalacticraftCore.LOGGER.info("Screenshot tour: station spin {} rad/tick (firing {}), sky turned to {}", spin == null ? null : spin.angularVelocity(),
                    spin == null ? null : spin.firing(), io.github.reiko45.galacticraft.client.render.OrbitSkyRenderer.spinAngle());
            shoot(minecraft, "station_spinning");
        });
        // The Earth below, past the station's open bay
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel orbit = srv.getLevel(GCDimensions.ORBIT);
            if (orbit != null && this.stationBase != null) {
                tourPlayer(srv).teleportTo(orbit, this.stationBase.getX() - 4.5D, this.stationBase.getY() + 6.0D, this.stationBase.getZ() + 9.5D, 160.0F, 62.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "orbit_earth_below"));
        marsSteps();
    }

    private BlockPos stationBase;
    private BlockPos marsScene;

    /**
     * Phase 7a: the Mars blocks and a Tier 2 Rocket on its pad; the Tier 2 workbench page; desh armour; Mars on the
     * destination screen; the landing balloons coming down and bouncing; then Mars itself by day and night, and a cavern.
     */
    private void marsSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            final ServerLevel level = srv.overworld();
            this.marsScene = this.rocketScene.offset(16, 0, 0);
            buildMarsScene(level, this.marsScene);
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.teleportTo(level, this.marsScene.getX() + 0.5D, this.marsScene.getY() + 6.0D, this.marsScene.getZ() + 17.5D, 180.0F, 18.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks and the rocket arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_blocks_and_tier_2_rocket"));
        // The Tier 2 page, unlocked with the schematic from the Moon dungeon, every part in place
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            GCPlayerData.get(p).unlockSchematic(io.github.reiko45.galacticraft.menu.Schematics.ROCKET_T2.id());
            final BlockPos bench = this.rocketScene.offset(-5, 0, 0);
            p.teleportTo(srv.overworld(), bench.getX() + 0.5, bench.getY(), bench.getZ() + 3.5, 180.0F, 15.0F);
            io.github.reiko45.galacticraft.menu.Schematics.open(p, io.github.reiko45.galacticraft.menu.Schematics.ROCKET_T2, srv.overworld(), bench);
            if (p.containerMenu instanceof io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu menu) {
                for (int i = 1; i < io.github.reiko45.galacticraft.menu.Tier2RocketBenchMenu.MATRIX_SIZE; i++) {
                    final int index = menu.getSlot(i).getContainerSlot();
                    final Item part = switch (index) {
                        case 1 -> GCItems.NOSE_CONE.get();
                        case 12, 16 -> GCItems.TIER_1_BOOSTER.get();
                        case 13, 14, 17, 18 -> GCItems.ROCKET_FINS.get();
                        case 15 -> GCItems.TIER_1_ROCKET_ENGINE.get();
                        case 19, 20, 21 -> Items.CHEST;
                        default -> GCItems.HEAVY_DUTY_PLATE.get();
                    };
                    menu.getSlot(i).set(new ItemStack(part));
                }
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_tier_2_rocket_bench"));
        // Desh armour and a desh pickaxe
        this.extras.add((minecraft, srv) -> {
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                p.closeContainer();
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(GCItems.DESH_HELMET.get()));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(GCItems.DESH_CHESTPLATE.get()));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(GCItems.DESH_LEGGINGS.get()));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(GCItems.DESH_BOOTS.get()));
                p.getInventory().selected = 0;
                p.getInventory().setItem(0, new ItemStack(GCItems.DESH_PICKAXE.get()));
                final GCPlayerData data = GCPlayerData.get(p);
                data.setGear(GearSlot.MASK, ItemStack.EMPTY);
                data.setGear(GearSlot.GEAR, ItemStack.EMPTY);
                data.setGear(GearSlot.LEFT_TANK, ItemStack.EMPTY);
                data.setGear(GearSlot.RIGHT_TANK, ItemStack.EMPTY);
                p.teleportTo(srv.overworld(), this.marsScene.getX() - 4.5D, this.marsScene.getY(), this.marsScene.getZ() + 6.5D, 0.0F, 0.0F);
            });
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "desh_armor"));
        // Mars on the destination screen, reachable from a Tier 2 Rocket
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                final GCPlayerData data = GCPlayerData.get(p);
                // Back into oxygen gear for Mars
                data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
                data.setGear(GearSlot.GEAR, new ItemStack(GCItems.OXYGEN_GEAR.get()));
                data.setGear(GearSlot.LEFT_TANK, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                data.setGear(GearSlot.RIGHT_TANK, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                data.inSpace = true;
                data.spacePos = p.position();
                data.rocketTier = 2;
                data.rocketStorage = 1;
                data.rocketFuel = 300;
                data.rocketCargo = NonNullList.withSize(20, ItemStack.EMPTY);
                data.launchedFromX = 8.5D;
                data.launchedFromZ = 8.5D;
                GCNetwork.sendToPlayer(p, SpaceTravel.selectionPayload(p));
            });
        });
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof CelestialSelectionScreen selection) {
                selection.select(GCDimensions.MARS);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_mars"));
        // Landing balloons: a slow fall under a parachute from y 900, then a long run of bounces
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null);
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            srv.overworld().setDayTime(5000L);
            srv.execute(() -> SpaceTravel.choose(tourPlayer(srv), GCDimensions.MARS));
        });
        this.extras.add((minecraft, srv) -> {
        }); // 45 ticks waiting for the ground to load, then falling: still above y 500, under the parachute
        this.extras.add((minecraft, srv) -> {
            logBalloons(srv, "descent");
            shoot(minecraft, "landing_balloons_descent");
        });
        for (int i = 0; i < 7; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // down, and through the first few bounces
        }
        this.extras.add((minecraft, srv) -> {
            logBalloons(srv, "bouncing");
            shoot(minecraft, "landing_balloons_bounce");
        });
        // Mars by day: the butterscotch sky and the dusty plains and hills
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
                final ServerPlayer p = tourPlayer(srv);
                if (mars == null) {
                    GalacticraftCore.LOGGER.warn("Screenshot tour: no Mars dimension");
                    return;
                }
                if (p.getVehicle() != null) {
                    final Entity vehicle = p.getVehicle();
                    p.stopRiding();
                    vehicle.discard();
                }
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                srv.overworld().setDayTime(4000L);
                p.teleportTo(mars, 40.5D, surface(mars, 40, 40) + 6.0D, 40.5D, 135.0F, 8.0F);
            });
        });
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_surface"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars != null) {
                srv.overworld().setDayTime(5000L);
                tourPlayer(srv).teleportTo(mars, 40.5D, surface(mars, 40, 40) + 3.0D, 40.5D, -90.0F, -68.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_sky_day"));
        this.extras.add((minecraft, srv) -> srv.overworld().setDayTime(17000L));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_sky_night"));
        // A cavern: vines glowing from the ceiling, sludge on the floor
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null) {
                return;
            }
            srv.overworld().setDayTime(6000L);
            final List<BlockPos> centres = io.github.reiko45.galacticraft.world.MarsCaverns.centres(mars.getSeed(), 0, 0, 12);
            if (centres.isEmpty()) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no Mars cavern near spawn");
                return;
            }
            centres.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
            final BlockPos centre = centres.get(0);
            mars.getChunk(centre);
            GalacticraftCore.LOGGER.info("Screenshot tour: Mars cavern centred at {}", centre);
            final ServerPlayer p = tourPlayer(srv);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 400, 0, false, false));
            p.teleportTo(mars, centre.getX() - 18.5D, 24.0D, centre.getZ() + 0.5D, -90.0F, 8.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_cavern"));
        marsMobSteps();
    }

    private BlockPos marsZoo;
    private java.util.UUID tourSlimeling;
    private BlockPos marsDungeonCrater;
    private BlockPos marsDungeonBoss;
    private BlockPos marsDungeonTreasure;

    /**
     * Phase 7b: slimelings, their eggs, a sludgeling, creeper eggs and the Tier 2 chest; the Mars dungeon, its Creeper
     * Boss (whole, then down to one head) and treasure; and a slimeling's panel, feeding screen and inventory.
     */
    private void marsMobSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null) {
                return;
            }
            srv.setDifficulty(Difficulty.NORMAL, true); // monsters vanish on Peaceful
            srv.overworld().setDayTime(5000L);
            this.marsZoo = new BlockPos(-100, surface(mars, -100, -100) + 1, -100);
            buildMarsZoo(mars, this.marsZoo);
            final ServerPlayer p = tourPlayer(srv);
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            p.teleportTo(mars, this.marsZoo.getX() + 0.5D, this.marsZoo.getY() + 2.6D, this.marsZoo.getZ() + 7.5D, 180.0F, 18.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_slimelings_and_eggs"));
        // The Mars dungeon of the first 44x44 chunk region, built as on the Moon (structures are off in the tour world)
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null) {
                return;
            }
            final var structure = mars.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolderOrThrow(io.github.reiko45.galacticraft.data.GCWorldgenProvider.MARS_DUNGEON);
            final var generator = mars.getChunkSource().getGenerator();
            final var placements = mars.getChunkSource().getGeneratorState().getPlacementsForStructure(structure);
            if (placements.isEmpty()) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no Mars dungeon placement");
                return;
            }
            final var placement = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) placements.get(0);
            final net.minecraft.world.level.ChunkPos chunk = placement.getPotentialStructureChunk(mars.getSeed(), 0, 0);
            final net.minecraft.world.level.levelgen.structure.StructureStart start = structure.value().generate(mars.registryAccess(), generator,
                    generator.getBiomeSource(), mars.getChunkSource().randomState(), mars.getStructureManager(), mars.getSeed(), chunk, 0, mars,
                    biome -> true);
            if (!start.isValid()) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no Mars dungeon start at {}", chunk);
                return;
            }
            final var box = start.getBoundingBox();
            net.minecraft.world.level.ChunkPos.rangeClosed(new net.minecraft.world.level.ChunkPos(box.minX() >> 4, box.minZ() >> 4),
                    new net.minecraft.world.level.ChunkPos(box.maxX() >> 4, box.maxZ() >> 4)).forEach(pos -> {
                mars.getChunk(pos.x, pos.z);
                start.placeInChunk(mars, mars.structureManager(), generator, mars.getRandom(), new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        pos.getMinBlockX(), mars.getMinBuildHeight(), pos.getMinBlockZ(), pos.getMaxBlockX(), mars.getMaxBuildHeight(), pos.getMaxBlockZ()), pos);
            });
            for (var piece : start.getPieces()) {
                if (piece instanceof io.github.reiko45.galacticraft.world.dungeon.DungeonPiece room) {
                    final BlockPos centre = room.getBoundingBox().getCenter();
                    switch (room.kind()) {
                        case CRATER -> this.marsDungeonCrater = centre;
                        case BOSS -> this.marsDungeonBoss = new BlockPos(centre.getX(), room.getBoundingBox().minY() + 1, centre.getZ());
                        case TREASURE -> {
                            for (BlockPos pos : BlockPos.betweenClosed(room.getBoundingBox().minX(), room.getBoundingBox().minY(), room.getBoundingBox().minZ(),
                                    room.getBoundingBox().maxX(), room.getBoundingBox().maxY(), room.getBoundingBox().maxZ())) {
                                if (mars.getBlockState(pos).is(GCBlocks.TREASURE_CHEST_TIER_2.get())) {
                                    this.marsDungeonTreasure = pos.immutable();
                                }
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
            GalacticraftCore.LOGGER.info("Screenshot tour: Mars dungeon in {} ({} pieces), boss room {}, treasure {}", chunk, start.getPieces().size(),
                    this.marsDungeonBoss, this.marsDungeonTreasure);
            if (this.marsDungeonCrater != null) {
                final int camX = this.marsDungeonCrater.getX() + 20;
                int top = 0;
                for (int dx = -2; dx <= 2; dx++) {
                    top = Math.max(top, surface(mars, camX + dx, this.marsDungeonCrater.getZ()));
                }
                tourPlayer(srv).teleportTo(mars, camX + 0.5D, top + 10.0D, this.marsDungeonCrater.getZ() + 0.5D, 90.0F, 35.0F);
            }
        }));
        for (int i = 0; i < 4; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // chunks around the dungeon load and generate
        }
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_dungeon_crater"));
        // Into the boss room, which wakes the Creeper Boss
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null || this.marsDungeonBoss == null) {
                return;
            }
            final ServerPlayer p = tourPlayer(srv);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200, 0, false, false));
            p.teleportTo(mars, this.marsDungeonBoss.getX() + 0.5D, this.marsDungeonBoss.getY() + 1.0D, this.marsDungeonBoss.getZ() + 10.5D, 180.0F, -8.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null || this.marsDungeonBoss == null) {
                return;
            }
            // Hold the boss still facing the camera, with one of its TNT on the way
            for (var boss : mars.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.CreeperBossEntity.class,
                    new net.minecraft.world.phys.AABB(this.marsDungeonBoss).inflate(20.0D))) {
                boss.setNoAi(true);
                boss.moveTo(this.marsDungeonBoss.getX() + 0.5D, this.marsDungeonBoss.getY(), this.marsDungeonBoss.getZ() - 2.5D, 0.0F, 0.0F);
                boss.setYHeadRot(0.0F);
                boss.setYBodyRot(0.0F);
                for (var tnt : mars.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.TntProjectileEntity.class, boss.getBoundingBox().inflate(30.0D))) {
                    tnt.discard();
                }
                final io.github.reiko45.galacticraft.entity.TntProjectileEntity tnt = new io.github.reiko45.galacticraft.entity.TntProjectileEntity(mars, boss,
                        this.marsDungeonBoss.getX() + 1.5D, this.marsDungeonBoss.getY() + 4.0D, this.marsDungeonBoss.getZ() + 4.0D, net.minecraft.world.phys.Vec3.ZERO);
                tnt.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                mars.addFreshEntity(tnt);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_dungeon_boss"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars != null && this.marsDungeonBoss != null) {
                for (var boss : mars.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.CreeperBossEntity.class,
                        new net.minecraft.world.phys.AABB(this.marsDungeonBoss).inflate(20.0D))) {
                    boss.setHealth(boss.getMaxHealth() * 0.3F); // punched-back TNT has taken two of its heads
                }
                for (var tnt : mars.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.TntProjectileEntity.class,
                        new net.minecraft.world.phys.AABB(this.marsDungeonBoss).inflate(30.0D))) {
                    tnt.discard();
                }
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_dungeon_boss_one_head"));
        // It dies and fills the Tier 2 Treasure Chest
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null || this.marsDungeonBoss == null) {
                return;
            }
            for (var boss : mars.getEntitiesOfClass(io.github.reiko45.galacticraft.entity.CreeperBossEntity.class,
                    new net.minecraft.world.phys.AABB(this.marsDungeonBoss).inflate(20.0D))) {
                boss.setNoAi(false);
                boss.kill();
            }
        }));
        for (int i = 0; i < 7; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // 200 ticks of dying
        }
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars != null && this.marsDungeonTreasure != null) {
                tourPlayer(srv).teleportTo(mars, this.marsDungeonTreasure.getX() + 0.5D, this.marsDungeonTreasure.getY() + 0.5D,
                        this.marsDungeonTreasure.getZ() + 3.0D, 180.0F, 25.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_dungeon_treasure"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null || this.marsDungeonTreasure == null) {
                return;
            }
            if (mars.getBlockEntity(this.marsDungeonTreasure) instanceof io.github.reiko45.galacticraft.block.entity.TreasureChestBlockEntity chest) {
                boolean schematic = false;
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    schematic |= chest.getItem(slot).getItem() instanceof io.github.reiko45.galacticraft.item.SchematicItem;
                }
                GalacticraftCore.LOGGER.info("Screenshot tour: Tier 2 treasure chest locked {}, schematic inside {}", chest.isLocked(), schematic);
                chest.setLocked(false); // what the Tier 2 Dungeon Key does
                final ServerPlayer p = tourPlayer(srv);
                p.teleportTo(mars, this.marsDungeonTreasure.getX() + 0.5D, this.marsDungeonTreasure.getY(), this.marsDungeonTreasure.getZ() + 2.5D, 180.0F, 30.0F);
                p.openMenu(chest);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_treasure_chest_tier_2"));
        // Back with the slimelings: the grown one's panel, a meal, and its bag
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            if (mars == null || this.marsZoo == null) {
                return;
            }
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            p.teleportTo(mars, this.marsZoo.getX() + 2.5D, this.marsZoo.getY() + 1.0D, this.marsZoo.getZ() + 4.5D, 180.0F, 20.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // the zoo's chunks (and the slimeling, reloaded with a new entity id) arrive
        this.extras.add((minecraft, srv) -> {
            final SlimelingEntity slimeling = clientSlimeling(minecraft);
            if (slimeling != null) {
                minecraft.setScreen(new io.github.reiko45.galacticraft.client.screen.SlimelingScreen(slimeling));
            } else {
                GalacticraftCore.LOGGER.warn("Screenshot tour: the tour slimeling is not loaded");
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_slimeling"));
        this.extras.add((minecraft, srv) -> {
            final SlimelingEntity slimeling = clientSlimeling(minecraft);
            if (slimeling != null) {
                minecraft.setScreen(new io.github.reiko45.galacticraft.client.screen.SlimelingFeedScreen(slimeling));
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_slimeling_feed"));
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null);
            srv.execute(() -> {
                final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
                if (mars != null && this.tourSlimeling != null && mars.getEntity(this.tourSlimeling) instanceof SlimelingEntity slimeling) {
                    io.github.reiko45.galacticraft.menu.SlimelingMenu.open(tourPlayer(srv), slimeling);
                }
            });
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_slimeling_inventory"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            tourPlayer(srv).closeContainer();
            srv.setDifficulty(Difficulty.PEACEFUL, true);
            srv.overworld().setDayTime(6000L);
        }));
        marsMachineSteps();
    }

    // ------------------------------------------------------------------ Phase 7c

    private BlockPos marsBase;
    private BlockPos terraformerScene;
    private java.util.UUID cargoRocketId;
    private final java.util.Map<String, BlockPos> marsMachines = new java.util.LinkedHashMap<>();

    /**
     * Phase 7c on Mars: the gas machines, a Cryogenic Chamber and a Cargo Rocket pad with its loader, unloader and Launch
     * Controller; a Terraformer greening the regolith under its bubble; each GUI (both Launch Controller pages, the
     * Cargo Rocket's cargo and its NASA Workbench page); the view from inside the Cryogenic Chamber; a cargo launch.
     */
    private void marsMachineSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars == null) {
                return;
            }
            srv.overworld().setDayTime(5000L);
            this.marsBase = new BlockPos(-160, surface(mars, -160, -100) + 1, -100);
            buildMarsBase(mars, this.marsBase);
            this.terraformerScene = new BlockPos(-160, surface(mars, -160, -160) + 1, -160);
            buildTerraformerScene(mars, this.terraformerScene);
            tourPlayer(srv).teleportTo(mars, this.marsBase.getX() + 2.5D, this.marsBase.getY() + 3.0D, this.marsBase.getZ() + 14.5D, 180.0F, 15.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "mars_machines_and_cargo_rocket"));
        for (String name : new String[]{"electrolyzer", "methane_synthesizer", "gas_liquefier", "cargo_loader", "cargo_unloader", "launch_controller"}) {
            this.extras.add((minecraft, srv) -> srv.execute(() -> {
                final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
                final BlockPos pos = this.marsMachines.get(name);
                if (mars == null || pos == null) {
                    return;
                }
                final ServerPlayer p = tourPlayer(srv);
                p.teleportTo(mars, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 3.5D, 180.0F, 15.0F);
                if (mars.getBlockEntity(pos) instanceof io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity controller) {
                    controller.openMenu(p);
                } else if (mars.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                    p.openMenu(machine, pos);
                }
            }));
            this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_" + name));
        }
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof io.github.reiko45.galacticraft.client.screen.LaunchControllerScreen screen) {
                screen.showAdvanced(true);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_launch_controller_advanced"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            if (mars != null && this.cargoRocketId != null && mars.getEntity(this.cargoRocketId) instanceof io.github.reiko45.galacticraft.entity.CargoRocketEntity rocket) {
                CargoMenu.openCargoRocket(p, rocket);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_cargo_rocket"));
        // The Cargo Rocket page, unlocked with the Creeper Boss's schematic, every part in place
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            if (mars == null) {
                return;
            }
            GCPlayerData.get(p).unlockSchematic(io.github.reiko45.galacticraft.menu.Schematics.CARGO_ROCKET.id());
            // at the Overworld scene's NASA Workbench (the page closes away from one)
            final BlockPos bench = this.rocketScene.offset(-5, 0, 0);
            p.teleportTo(srv.overworld(), bench.getX() + 0.5, bench.getY(), bench.getZ() + 3.5, 180.0F, 15.0F);
            io.github.reiko45.galacticraft.menu.Schematics.open(p, io.github.reiko45.galacticraft.menu.Schematics.CARGO_ROCKET, srv.overworld(), bench);
            if (p.containerMenu instanceof io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu menu) {
                for (int i = 1; i < io.github.reiko45.galacticraft.menu.CargoRocketBenchMenu.MATRIX_SIZE; i++) {
                    final int index = menu.getSlot(i).getContainerSlot();
                    final Item part = switch (index) {
                        case 1 -> GCItems.NOSE_CONE.get();
                        case 2 -> GCItems.ADVANCED_WAFER.get();
                        case 9, 10, 12, 13 -> GCItems.ROCKET_FINS.get();
                        case 11 -> GCItems.TIER_1_ROCKET_ENGINE.get();
                        case 14, 15, 16 -> Items.CHEST;
                        default -> GCItems.HEAVY_DUTY_PLATE.get();
                    };
                    menu.getSlot(i).set(new ItemStack(part));
                }
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_cargo_rocket_bench"));
        // The Terraformer's bubble from outside, then its GUI
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            if (mars != null && this.terraformerScene != null) {
                p.teleportTo(mars, this.terraformerScene.getX() + 0.5D, this.terraformerScene.getY() + 9.0D, this.terraformerScene.getZ() + 26.5D, 180.0F, 18.0F);
            }
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> shoot(minecraft, "terraformer_bubble"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            if (mars != null && this.terraformerScene != null && mars.getBlockEntity(this.terraformerScene) instanceof MachineBlockEntity machine) {
                final ServerPlayer p = tourPlayer(srv);
                p.teleportTo(mars, this.terraformerScene.getX() + 0.5D, this.terraformerScene.getY(), this.terraformerScene.getZ() + 3.5D, 180.0F, 15.0F);
                p.openMenu(machine, this.terraformerScene);
            }
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_terraformer"));
        // Inside the Cryogenic Chamber
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            final BlockPos chamber = this.marsMachines.get("cryogenic_chamber");
            if (mars != null && chamber != null) {
                p.teleportTo(mars, chamber.getX() + 0.5D, chamber.getY(), chamber.getZ() + 2.5D, 180.0F, 0.0F);
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                io.github.reiko45.galacticraft.block.CryogenicChamberBlock.enter(p, chamber);
            }
        }));
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null); // the Leave Bed screen
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "cryogenic_chamber_inside"));
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            minecraft.setScreen(null);
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "cryogenic_chamber_sleeper"));
        // A cargo launch: the Launch Controller's rule fires at once
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerLevel mars = srv.getLevel(GCDimensions.MARS);
                final ServerPlayer p = tourPlayer(srv);
                p.stopSleepInBed(true, true);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                final BlockPos pos = this.marsMachines.get("launch_controller");
                if (mars != null && pos != null && mars.getBlockEntity(pos) instanceof io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity controller) {
                    controller.setLaunchSelection(io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity.AutoLaunch.INSTANT);
                    controller.setLaunchSchedulingEnabled(true);
                    p.teleportTo(mars, this.marsBase.getX() + 6.5D, this.marsBase.getY() + 2.0D, this.marsBase.getZ() + 13.5D, 180.0F, 0.0F);
                }
            });
        });
        this.extras.add((minecraft, srv) -> {
        }); // countdown
        this.extras.add((minecraft, srv) -> shoot(minecraft, "cargo_rocket_launch"));
        addAsteroidSteps();
    }

    private BlockPos asteroidScene;
    private BlockPos podLanding;

    /**
     * Phase 7d: a Tier 3 Rocket on its pad with the asteroid blocks and a player in Thermal Padding; the Tier 3 page; the
     * asteroids on the destination screen; the Entry Pod coming down and landed; the belt itself, its sky and a hollow
     * asteroid; and the thermometer warning on the HUD.
     */
    private void addAsteroidSteps() {
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            final ServerLevel level = srv.overworld();
            this.asteroidScene = this.rocketScene.offset(40, 0, 0);
            buildAsteroidScene(level, this.asteroidScene);
            final GCPlayerData data = GCPlayerData.get(p);
            for (GearSlot slot : GearData.THERMAL_SLOTS) {
                data.setGear(slot, ItemStack.EMPTY);
            }
            data.setGear(GearSlot.THERMAL_HELMET, new ItemStack(GCItems.THERMAL_PADDING_HELMET.get()));
            data.setGear(GearSlot.THERMAL_CHESTPLATE, new ItemStack(GCItems.THERMAL_PADDING_CHESTPLATE.get()));
            data.setGear(GearSlot.THERMAL_LEGGINGS, new ItemStack(GCItems.THERMAL_PADDING_LEGGINGS.get()));
            data.setGear(GearSlot.THERMAL_BOOTS, new ItemStack(GCItems.THERMAL_PADDING_BOOTS.get()));
            data.setGear(GearSlot.PARACHUTE, ItemStack.EMPTY);
            data.markGearChanged();
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                p.setItemSlot(slot, ItemStack.EMPTY);
            }
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GCItems.TITANIUM_PICKAXE.get()));
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            level.setDayTime(5000L);
            p.teleportTo(level, this.asteroidScene.getX() - 2.5D, this.asteroidScene.getY(), this.asteroidScene.getZ() + 4.5D, 180.0F, 0.0F);
        }));
        this.extras.add((minecraft, srv) -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "tier_3_rocket_and_thermal_padding"));
        // The Tier 3 Rocket page, unlocked with the Creeper Boss's schematic, every part in place
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                GCPlayerData.get(p).unlockSchematic(io.github.reiko45.galacticraft.menu.Schematics.ROCKET_T3.id());
                final BlockPos bench = this.rocketScene.offset(-5, 0, 0);
                p.teleportTo(srv.overworld(), bench.getX() + 0.5, bench.getY(), bench.getZ() + 3.5, 180.0F, 15.0F);
                io.github.reiko45.galacticraft.menu.Schematics.open(p, io.github.reiko45.galacticraft.menu.Schematics.ROCKET_T3, srv.overworld(), bench);
                if (p.containerMenu instanceof io.github.reiko45.galacticraft.menu.Tier3RocketBenchMenu menu) {
                    for (int i = 1; i < io.github.reiko45.galacticraft.menu.Tier3RocketBenchMenu.MATRIX_SIZE; i++) {
                        final int index = menu.getSlot(i).getContainerSlot();
                        final Item part = switch (index) {
                            case 1 -> GCItems.HEAVY_NOSE_CONE.get();
                            case 12, 16 -> GCItems.TIER_1_BOOSTER.get();
                            case 13, 14, 17, 18 -> GCItems.HEAVY_ROCKET_FINS.get();
                            case 15 -> GCItems.HEAVY_ROCKET_ENGINE.get();
                            case 19, 20 -> Items.CHEST;
                            case 21 -> null;
                            default -> GCItems.HEAVY_DUTY_PLATE_TIER_3.get();
                        };
                        if (part != null) {
                            menu.getSlot(i).set(new ItemStack(part));
                        }
                    }
                }
            });
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "gui_tier_3_rocket_bench"));
        // The asteroids on the destination screen, reachable from a Tier 3 Rocket
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            p.closeContainer();
            final GCPlayerData data = GCPlayerData.get(p);
            data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
            data.setGear(GearSlot.GEAR, new ItemStack(GCItems.OXYGEN_GEAR.get()));
            data.setGear(GearSlot.LEFT_TANK, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
            data.setGear(GearSlot.RIGHT_TANK, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
            data.markGearChanged();
            data.inSpace = true;
            data.spacePos = p.position();
            data.rocketTier = 3;
            data.rocketStorage = 2;
            data.rocketFuel = 200;
            data.rocketCargo = NonNullList.withSize(38, ItemStack.EMPTY);
            data.rocketCargo.set(0, new ItemStack(GCItems.THERMAL_CLOTH.get(), 12));
            data.launchedFromX = 0.5D;
            data.launchedFromZ = 0.5D;
            GCNetwork.sendToPlayer(p, SpaceTravel.selectionPayload(p));
        }));
        this.extras.add((minecraft, srv) -> {
            if (minecraft.screen instanceof CelestialSelectionScreen selection) {
                selection.select(GCDimensions.ASTEROIDS);
            }
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "celestial_asteroids"));
        // The Entry Pod: 40 ticks for the ground to load, then a fall from y 310 onto an asteroid
        this.extras.add((minecraft, srv) -> {
            minecraft.setScreen(null);
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            srv.execute(() -> SpaceTravel.choose(tourPlayer(srv), GCDimensions.ASTEROIDS));
        });
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> {
            logPod(srv, "descent");
            shoot(minecraft, "entry_pod_descent");
        });
        for (int i = 0; i < 9; i++) {
            this.extras.add((minecraft, srv) -> {
            }); // down onto the rock
        }
        this.extras.add((minecraft, srv) -> {
            logPod(srv, "landed");
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "entry_pod_landed"));
        // Out of the pod: the thermometer warns while the cold creeps in without padding
        this.extras.add((minecraft, srv) -> {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            srv.execute(() -> {
                final ServerPlayer p = tourPlayer(srv);
                if (p.getVehicle() != null) {
                    final Entity pod = p.getVehicle();
                    p.stopRiding();
                    this.podLanding = pod.blockPosition();
                    p.teleportTo(p.serverLevel(), pod.getX() + 2.0D, pod.getY(), pod.getZ(), -90.0F, 10.0F);
                }
                final GCPlayerData data = GCPlayerData.get(p);
                for (GearSlot slot : GearData.THERMAL_SLOTS) {
                    data.setGear(slot, ItemStack.EMPTY);
                }
                data.markGearChanged();
                data.thermalLevel = -14;
                p.setGameMode(GameType.SURVIVAL);
            });
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "thermal_hud_warning"));
        // The belt from among the asteroids, its sky with the sun on the horizon, and a hollow asteroid's inside
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            GCPlayerData.get(p).thermalLevel = 0;
            final ServerLevel asteroids = srv.getLevel(GCDimensions.ASTEROIDS);
            final io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator.Asteroid big = asteroid(asteroids, false);
            if (big == null) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no big asteroid");
                return;
            }
            final double distance = big.size() + 22.0D;
            final double x = big.x() - distance * 0.7D;
            final double z = big.z() + distance * 0.7D;
            final double y = big.y() + 8.0D;
            final float yaw = (float) Math.toDegrees(Math.atan2(-(big.x() - x), big.z() - z));
            final float pitch = (float) Math.toDegrees(Math.atan2(y - big.y(), distance));
            GalacticraftCore.LOGGER.info("Screenshot tour: big asteroid {}, camera at {} {} {}", big, x, y, z);
            p.teleportTo(asteroids, x, y, z, yaw, pitch);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "asteroid_belt"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 90.0F, -4.0F); // west, to the sun on the horizon
        }));
        this.extras.add((minecraft, srv) -> shoot(minecraft, "asteroid_sky"));
        this.extras.add((minecraft, srv) -> srv.execute(() -> {
            final ServerPlayer p = tourPlayer(srv);
            final ServerLevel asteroids = srv.getLevel(GCDimensions.ASTEROIDS);
            final io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator.Asteroid hollow = asteroid(asteroids, true);
            if (hollow == null) {
                GalacticraftCore.LOGGER.warn("Screenshot tour: no hollow asteroid");
                return;
            }
            GalacticraftCore.LOGGER.info("Screenshot tour: hollow asteroid {}", hollow);
            p.teleportTo(asteroids, hollow.x() + 0.5D, hollow.y() + 1.0D, hollow.z() + 0.5D, 45.0F, 25.0F);
        }));
        this.extras.add((minecraft, srv) -> {
        }); // chunks arrive
        this.extras.add((minecraft, srv) -> {
        });
        this.extras.add((minecraft, srv) -> shoot(minecraft, "hollow_asteroid"));
    }

    /** The nearest asteroid to (0, 0) that is (or isn't) hollow, big ones first for the outside view. */
    @org.jetbrains.annotations.Nullable
    private static io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator.Asteroid asteroid(@org.jetbrains.annotations.Nullable ServerLevel level,
                                                                                                 boolean hollow) {
        if (level == null || !(level.getChunkSource().getGenerator() instanceof io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator generator)) {
            return null;
        }
        io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator.Asteroid best = null;
        double bestDistance = Double.MAX_VALUE;
        for (io.github.reiko45.galacticraft.world.AsteroidsChunkGenerator.Asteroid asteroid
                : generator.asteroidsNear(level.getChunkSource().randomState(), 0, 0, 300)) {
            if (asteroid.hollow() != hollow || !hollow && asteroid.size() < 18) {
                continue;
            }
            final double distance = (double) asteroid.x() * asteroid.x() + (double) asteroid.z() * asteroid.z();
            if (distance < bestDistance) {
                bestDistance = distance;
                best = asteroid;
            }
        }
        return best;
    }

    private static void logPod(MinecraftServer server, String when) {
        final ServerPlayer p = tourPlayer(server);
        GalacticraftCore.LOGGER.info("Screenshot tour ({}): player at {} riding {}", when, p.position(), p.getVehicle());
    }

    /** A launch pad with a Tier 3 Rocket and, beside it, each asteroid block on a plinth. */
    private void buildAsteroidScene(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, 0, -5), origin.offset(6, 9, 8))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-12, -1, -5), origin.offset(6, -1, 8))) {
            level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
            level.setBlockAndUpdate(pos, GCBlocks.LANDING_PAD.get().defaultBlockState());
        }
        final io.github.reiko45.galacticraft.entity.Tier3RocketEntity rocket = io.github.reiko45.galacticraft.entity.Tier3RocketEntity.create(level,
                origin.getX() + 0.5D, origin.getY() + 0.4D, origin.getZ() + 0.5D, 3, 0);
        rocket.setYRot(180.0F);
        level.addFreshEntity(rocket);
        final Block[] blocks = {GCBlocks.ASTEROID_ROCK_0.get(), GCBlocks.ASTEROID_ROCK_1.get(), GCBlocks.ASTEROID_ROCK_2.get(),
                GCBlocks.ASTEROID_ALUMINUM_ORE.get(), GCBlocks.ILMENITE_ORE.get(), GCBlocks.ASTEROID_IRON_ORE.get(), GCBlocks.DENSE_ICE.get(),
                GCBlocks.DARK_DECORATION_BLOCK.get(), GCBlocks.TITANIUM_BLOCK.get()};
        for (int i = 0; i < blocks.length; i++) {
            level.setBlockAndUpdate(origin.offset(-11 + i, 0, -1), blocks[i].defaultBlockState());
        }
    }

    /**
     * Gas machines and a Cryogenic Chamber in a row, and a launch pad with a Cargo Rocket, a Cargo Loader and Unloader
     * and a Launch Controller (set up to send it to a second pad on the right).
     */
    private void buildMarsBase(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, 0, -4), origin.offset(14, 10, 14))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, -1, -4), origin.offset(14, -1, 14))) {
            level.setBlockAndUpdate(pos, GCBlocks.MARS_COBBLESTONE.get().defaultBlockState());
        }
        final ItemStack battery = GCItems.BATTERY.get().charged();
        final BlockPos electrolyzer = origin.offset(-7, 0, -1);
        final BlockPos synthesizer = origin.offset(-5, 0, -1);
        final BlockPos liquefier = origin.offset(-3, 0, -1);
        level.setBlockAndUpdate(electrolyzer, GCBlocks.ELECTROLYZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(synthesizer, GCBlocks.METHANE_SYNTHESIZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(liquefier, GCBlocks.GAS_LIQUEFIER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(electrolyzer) instanceof io.github.reiko45.galacticraft.block.entity.ElectrolyzerBlockEntity machine) {
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.tanks()[0].fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);
            machine.tanks()[1].fill(new FluidStack(GCFluids.OXYGEN.get(), 1500), IFluidHandler.FluidAction.EXECUTE);
            machine.tanks()[2].fill(new FluidStack(GCFluids.HYDROGEN.get(), 3000), IFluidHandler.FluidAction.EXECUTE);
            machine.inventory().setStackInSlot(1, new ItemStack(Items.WATER_BUCKET));
        }
        if (level.getBlockEntity(synthesizer) instanceof io.github.reiko45.galacticraft.block.entity.MethaneSynthesizerBlockEntity machine) {
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.tanks()[0].fill(new FluidStack(GCFluids.HYDROGEN.get(), 2500), IFluidHandler.FluidAction.EXECUTE);
            machine.tanks()[1].fill(new FluidStack(GCFluids.CARBON_DIOXIDE.get(), 800), IFluidHandler.FluidAction.EXECUTE);
            machine.tanks()[2].fill(new FluidStack(GCFluids.METHANE.get(), 1200), IFluidHandler.FluidAction.EXECUTE);
            machine.inventory().setStackInSlot(2, new ItemStack(GCItems.ATMOSPHERIC_VALVE.get()));
            machine.inventory().setStackInSlot(3, new ItemStack(GCItems.CARBON_FRAGMENTS.get(), 16));
        }
        if (level.getBlockEntity(liquefier) instanceof io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity machine) {
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.tanks()[0].fill(new FluidStack(GCFluids.METHANE.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
            machine.tanks()[1].fill(new FluidStack(GCFluids.FUEL.get(), 900), IFluidHandler.FluidAction.EXECUTE);
            machine.inventory().setStackInSlot(1, LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE));
        }
        this.marsMachines.put("electrolyzer", electrolyzer);
        this.marsMachines.put("methane_synthesizer", synthesizer);
        this.marsMachines.put("gas_liquefier", liquefier);
        final BlockPos chamber = origin.offset(0, 0, -1);
        final BlockState chamberState = GCBlocks.CRYOGENIC_CHAMBER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        level.setBlockAndUpdate(chamber, chamberState);
        chamberState.getBlock().setPlacedBy(level, chamber, chamberState, tourPlayer(level.getServer()), new ItemStack(GCBlocks.CRYOGENIC_CHAMBER.get()));
        this.marsMachines.put("cryogenic_chamber", chamber);
        // The cargo pad: loader on its west, unloader on its south, controller on its east
        final BlockPos pad = origin.offset(6, 0, 3);
        final BlockPos farPad = origin.offset(12, 0, 3);
        for (BlockPos centre : new BlockPos[]{pad, farPad}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(centre.offset(dx, 0, dz), GCBlocks.LANDING_PAD.get().defaultBlockState());
                }
            }
        }
        final BlockPos loader = pad.west(2);
        final BlockPos unloader = pad.south(2);
        final BlockPos controller = pad.north(2);
        level.setBlockAndUpdate(loader, GCBlocks.CARGO_LOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(unloader, GCBlocks.CARGO_UNLOADER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(controller, GCBlocks.LAUNCH_CONTROLLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        final BlockPos farController = farPad.north(2);
        level.setBlockAndUpdate(farController, GCBlocks.LAUNCH_CONTROLLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(loader) instanceof io.github.reiko45.galacticraft.block.entity.CargoLoaderBlockEntity machine) {
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.inventory().setStackInSlot(1, new ItemStack(GCItems.RAW_DESH.get(), 48));
            machine.inventory().setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 32));
            machine.inventory().setStackInSlot(3, new ItemStack(GCItems.CARBON_FRAGMENTS.get(), 20));
        }
        if (level.getBlockEntity(unloader) instanceof io.github.reiko45.galacticraft.block.entity.CargoUnloaderBlockEntity machine) {
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.toggleDisabled();
        }
        final ServerPlayer owner = tourPlayer(level.getServer());
        if (level.getBlockEntity(controller) instanceof io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity machine) {
            machine.setOwner(owner);
            machine.inventory().setStackInSlot(0, battery.copy());
            machine.energy().setStored(16_000.0F);
            machine.setFrequency(1);
            machine.setDestFrequency(2);
        }
        if (level.getBlockEntity(farController) instanceof io.github.reiko45.galacticraft.block.entity.LaunchControllerBlockEntity machine) {
            machine.setOwner(owner);
            machine.energy().setStored(16_000.0F);
            machine.setFrequency(2);
            machine.setDestFrequency(1);
        }
        this.marsMachines.put("cargo_loader", loader);
        this.marsMachines.put("cargo_unloader", unloader);
        this.marsMachines.put("launch_controller", controller);
        final io.github.reiko45.galacticraft.entity.CargoRocketEntity rocket = io.github.reiko45.galacticraft.entity.CargoRocketEntity.create(level,
                pad.getX() + 0.5D, pad.getY() + 0.4D + io.github.reiko45.galacticraft.entity.CargoRocketEntity.PAD_OFFSET, pad.getZ() + 0.5D, 2,
                io.github.reiko45.galacticraft.entity.CargoRocketEntity.FUEL_CAPACITY);
        rocket.setYRot(180.0F);
        rocket.cargo().setItem(0, new ItemStack(GCItems.DESH_INGOT.get(), 24));
        rocket.cargo().setItem(1, new ItemStack(Items.REDSTONE, 40));
        level.addFreshEntity(rocket);
        this.cargoRocketId = rocket.getUUID();
    }

    /** A Terraformer at full bubble with water, bone meal, seeds and saplings, the regolith around it already greening. */
    private void buildTerraformerScene(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, 0, -16), origin.offset(16, 12, 16))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7L);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-16, -1, -16), origin.offset(16, -1, 16))) {
            final double d = Math.sqrt(pos.distSqr(origin.below()));
            final boolean green = d < 9.0D && random.nextFloat() < 0.75F || d < 13.0D && random.nextFloat() < 0.25F;
            level.setBlockAndUpdate(pos, (green ? Blocks.GRASS_BLOCK : GCBlocks.MARS_FINE_REGOLITH.get()).defaultBlockState());
        }
        for (BlockPos sapling : new BlockPos[]{origin.offset(-5, 0, -3), origin.offset(4, 0, -6), origin.offset(6, 0, 4)}) {
            level.setBlockAndUpdate(sapling.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(sapling, Blocks.OAK_SAPLING.defaultBlockState());
            if (level.getBlockState(sapling).getBlock() instanceof net.minecraft.world.level.block.SaplingBlock tree) {
                tree.advanceTree(level, sapling, level.getBlockState(sapling).setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1), random);
            }
        }
        level.setBlockAndUpdate(origin.offset(-2, -1, 5), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(origin, GCBlocks.TERRAFORMER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(origin) instanceof io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity terraformer) {
            terraformer.inventory().setStackInSlot(1, GCItems.BATTERY.get().charged());
            terraformer.energy().setStored(16_000.0F);
            terraformer.waterTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1600), IFluidHandler.FluidAction.EXECUTE);
            terraformer.inventory().setStackInSlot(0, new ItemStack(Items.WATER_BUCKET));
            terraformer.inventory().setStackInSlot(2, new ItemStack(Items.BONE_MEAL, 64));
            terraformer.inventory().setStackInSlot(3, new ItemStack(Items.BONE_MEAL, 32));
            terraformer.inventory().setStackInSlot(6, new ItemStack(Items.OAK_SAPLING, 16));
            terraformer.inventory().setStackInSlot(7, new ItemStack(Items.BIRCH_SAPLING, 8));
            terraformer.inventory().setStackInSlot(10, new ItemStack(Items.WHEAT_SEEDS, 32));
            terraformer.setBubbleSize(io.github.reiko45.galacticraft.block.entity.TerraformerBlockEntity.MAX_SIZE);
        }
    }

    private SlimelingEntity clientSlimeling(Minecraft minecraft) {
        if (minecraft.level == null || this.tourSlimeling == null) {
            return null;
        }
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof SlimelingEntity slimeling && this.tourSlimeling.equals(slimeling.getUUID())) {
                return slimeling;
            }
        }
        return null;
    }

    /**
     * Slimeling eggs (one cracked), a Creeper Egg and a Tier 2 Treasure Chest behind three hatchlings and a grown, bred
     * orange slimeling, a sludgeling by its pool, and a baby Evolved Creeper.
     */
    private void buildMarsZoo(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, 0, -4), origin.offset(8, 8, 9))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -1, -4), origin.offset(8, -1, 9))) {
            level.setBlockAndUpdate(pos, GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState());
        }
        final BlockState[] row = {GCBlocks.RED_SLIMELING_EGG.get().defaultBlockState(), GCBlocks.BLUE_SLIMELING_EGG.get().defaultBlockState(),
                GCBlocks.YELLOW_SLIMELING_EGG.get().defaultBlockState().setValue(io.github.reiko45.galacticraft.block.SlimelingEggBlock.BROKEN, true),
                GCBlocks.CREEPER_EGG.get().defaultBlockState(), GCBlocks.TREASURE_CHEST_TIER_2.get().defaultBlockState()};
        for (int i = 0; i < row.length; i++) {
            level.setBlockAndUpdate(origin.offset(-4 + i * 2, 0, -2), row[i]);
        }
        level.setBlockAndUpdate(origin.offset(6, -1, 0), GCBlocks.MARS_STONE.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(6, 0, 0), io.github.reiko45.galacticraft.registry.GCFluids.BACTERIAL_SLUDGE_BLOCK.get().defaultBlockState());
        final float[][] colors = {{1.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 1.0F}, {1.0F, 1.0F, 0.0F}};
        for (int i = 0; i < colors.length; i++) {
            final SlimelingEntity hatchling = SlimelingEntity.hatch(level, colors[i][0], colors[i][1], colors[i][2]);
            zooMob(level, hatchling, origin.offset(-5 + i * 2, 0, 1));
        }
        final SlimelingEntity red = SlimelingEntity.hatch(level, 1.0F, 0.0F, 0.0F);
        final SlimelingEntity yellow = SlimelingEntity.hatch(level, 1.0F, 1.0F, 0.0F);
        final SlimelingEntity grown = (SlimelingEntity) red.getBreedOffspring(level, yellow);
        if (grown != null) {
            final ServerPlayer p = tourPlayer(level.getServer());
            grown.tame(p);
            grown.setOwnerName(p.getGameProfile().getName());
            grown.setSlimelingAge(SlimelingEntity.MAX_AGE * 3 / 4);
            grown.setCustomName(net.minecraft.network.chat.Component.literal("Marmalade"));
            grown.inventory().setItem(io.github.reiko45.galacticraft.entity.SlimelingInventory.BAG_SLOT, new ItemStack(GCItems.SLIMELING_CARGO.get()));
            grown.inventory().setItem(io.github.reiko45.galacticraft.entity.SlimelingInventory.CARGO_START, new ItemStack(GCItems.RAW_DESH.get(), 12));
            grown.inventory().setItem(io.github.reiko45.galacticraft.entity.SlimelingInventory.CARGO_START + 1, new ItemStack(Items.SLIME_BALL, 5));
            zooMob(level, grown, origin.offset(2, 0, 1));
            this.tourSlimeling = grown.getUUID();
        }
        final net.minecraft.world.entity.Entity sludgeling = GCEntities.SLUDGELING.get().create(level);
        if (sludgeling instanceof net.minecraft.world.entity.Mob mob) {
            zooMob(level, mob, origin.offset(5, 0, 1));
        }
        final io.github.reiko45.galacticraft.entity.monster.EvolvedCreeper baby = GCEntities.EVOLVED_CREEPER.get().create(level);
        if (baby != null) {
            baby.setBaby(true);
            zooMob(level, baby, origin.offset(-7, 0, 1));
        }
    }

    private static void zooMob(ServerLevel level, net.minecraft.world.entity.Mob mob, BlockPos pos) {
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        mob.setYHeadRot(0.0F);
        mob.setYBodyRot(0.0F);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static void logBalloons(MinecraftServer server, String when) {
        final ServerPlayer p = tourPlayer(server);
        if (p.getVehicle() instanceof io.github.reiko45.galacticraft.entity.LandingBalloonsEntity balloons) {
            GalacticraftCore.LOGGER.info("Screenshot tour ({}): balloons at y {} moving {}, {} bounces", when, balloons.getY(),
                    balloons.getDeltaMovement().y, balloons.groundHits());
        } else {
            GalacticraftCore.LOGGER.warn("Screenshot tour ({}): not riding landing balloons but {}", when, p.getVehicle());
        }
    }

    /** A row of every Mars block on a fine regolith floor, a vine strand, a sludge pool, and a Tier 2 Rocket on its pad. */
    private void buildMarsScene(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, 0, -6), origin.offset(7, 10, 6))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, -1, -6), origin.offset(7, -1, 6))) {
            level.setBlockAndUpdate(pos, GCBlocks.MARS_FINE_REGOLITH.get().defaultBlockState());
        }
        final Block[] row = {GCBlocks.MARS_COPPER_ORE.get(), GCBlocks.MARS_TIN_ORE.get(), GCBlocks.MARS_IRON_ORE.get(), GCBlocks.DESH_ORE.get(),
                GCBlocks.DESH_BLOCK.get(), GCBlocks.MARS_FINE_REGOLITH.get(), GCBlocks.MARS_REGOLITH.get(), GCBlocks.MARS_STONE.get(),
                GCBlocks.MARS_COBBLESTONE.get(), GCBlocks.MARS_DUNGEON_BRICK.get(), GCBlocks.MARS_COBBLESTONE_STAIRS.get(),
                GCBlocks.MARS_DUNGEON_BRICK_STAIRS.get(), GCBlocks.MARS_COBBLESTONE_SLAB.get(), GCBlocks.MARS_DUNGEON_BRICK_SLAB.get(),
                GCBlocks.MARS_COBBLESTONE_WALL.get()};
        for (int i = 0; i < row.length; i++) {
            final BlockPos pos = origin.offset(-7 + i, 0, 4);
            BlockState state = row[i].defaultBlockState();
            if (state.hasProperty(net.minecraft.world.level.block.StairBlock.FACING)) {
                state = state.setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH);
            }
            level.setBlockAndUpdate(pos, state);
        }
        // A cobblestone arch with cavernous vines hanging from it, over a pool of bacterial sludge
        for (int y = 0; y < 5; y++) {
            level.setBlockAndUpdate(origin.offset(-6, y, -2), GCBlocks.MARS_DUNGEON_BRICK.get().defaultBlockState());
            level.setBlockAndUpdate(origin.offset(-2, y, -2), GCBlocks.MARS_DUNGEON_BRICK.get().defaultBlockState());
        }
        for (int x = -6; x <= -2; x++) {
            level.setBlockAndUpdate(origin.offset(x, 5, -2), GCBlocks.MARS_DUNGEON_BRICK.get().defaultBlockState());
        }
        for (int x = -5; x <= -3; x++) {
            final int length = x == -4 ? 4 : 2;
            for (int i = 0; i < length; i++) {
                level.setBlockAndUpdate(origin.offset(x, 4 - i, -2), GCBlocks.CAVERNOUS_VINES.get().defaultBlockState()
                        .setValue(io.github.reiko45.galacticraft.block.CavernousVinesBlock.TYPE, i % 3));
            }
        }
        for (int x = -5; x <= -3; x++) {
            level.setBlockAndUpdate(origin.offset(x, -1, 0), GCBlocks.MARS_STONE.get().defaultBlockState());
            level.setBlockAndUpdate(origin.offset(x, -1, -1), GCBlocks.MARS_STONE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(origin.offset(-4, 0, 0), io.github.reiko45.galacticraft.registry.GCFluids.BACTERIAL_SLUDGE_BLOCK.get().defaultBlockState());
        // The Tier 2 Rocket with three chests' worth of cargo on a launch pad
        final BlockPos pad = origin.offset(3, 0, -2);
        for (BlockPos pos : BlockPos.betweenClosed(pad.offset(-1, 0, -1), pad.offset(1, 0, 1))) {
            level.setBlockAndUpdate(pos, GCBlocks.LANDING_PAD.get().defaultBlockState());
        }
        final io.github.reiko45.galacticraft.entity.Tier2RocketEntity rocket = io.github.reiko45.galacticraft.entity.Tier2RocketEntity.create(level,
                pad.getX() + 0.5D, pad.getY() + 0.2D, pad.getZ() + 0.5D, 3, 0);
        rocket.setYRot(180.0F);
        level.addFreshEntity(rocket);
    }

    private static void logFreefall(Minecraft minecraft, String when) {
        if (minecraft.player != null) {
            GalacticraftCore.LOGGER.info("Screenshot tour ({}): player at {} in freefall {}, motion {}", when, minecraft.player.position(),
                    io.github.reiko45.galacticraft.world.station.Freefall.inFreefall(minecraft.player), minecraft.player.getDeltaMovement());
        }
    }

    /**
     * Basic and advanced solar panels; an oil tank stack -> pull pipe -> pipe -> Refinery (facing the camera, oil in on
     * its west face, fuel out east) -> pull pipe -> dyed pipe -> fuel tank; and a source block of each liquid.
     */
    private void buildOilScene(ServerLevel level, BlockPos origin) {
        this.oilScene = origin;
        for (int x = -9; x <= 12; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 4; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (Block solar : new Block[]{GCBlocks.BASIC_SOLAR_PANEL.get(), GCBlocks.ADVANCED_SOLAR_PANEL.get()}) {
            final BlockPos pos = origin.offset(solar == GCBlocks.BASIC_SOLAR_PANEL.get() ? -7 : -3, 0, 0);
            final BlockState state = solar.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH);
            level.setBlockAndUpdate(pos, state);
            solar.setPlacedBy(level, pos, state, null, new ItemStack(solar));
            this.machines.add(pos);
        }

        final BlockPos refinery = origin.offset(4, 0, 0);
        level.setBlockAndUpdate(refinery, GCBlocks.REFINERY.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (level.getBlockEntity(refinery) instanceof RefineryBlockEntity machine) {
            machine.energy().setStored(16_000.0F);
            machine.oilTank().fill(new FluidStack(GCFluids.OIL.get(), 6_000), IFluidHandler.FluidAction.EXECUTE);
            machine.fuelTank().fill(new FluidStack(GCFluids.FUEL.get(), 3_000), IFluidHandler.FluidAction.EXECUTE);
            machine.inventory().setStackInSlot(0, GCItems.BATTERY.get().charged());
            machine.inventory().setStackInSlot(1, LiquidCanisterItem.full(false));
            machine.inventory().setStackInSlot(2, new ItemStack(GCItems.OIL_CANISTER.get()));
        }
        this.machines.add(refinery);

        // Oil supply: two stacked tanks, then a pull pipe and a normal pipe into the refinery's oil face.
        final BlockPos oilTank = origin.offset(1, 0, 0);
        level.setBlockAndUpdate(oilTank, GCBlocks.FLUID_TANK.get().defaultBlockState());
        level.setBlockAndUpdate(oilTank.above(), GCBlocks.FLUID_TANK.get().defaultBlockState());
        if (level.getBlockEntity(oilTank) instanceof FluidTankBlockEntity tank) {
            tank.stackHandler().fill(new FluidStack(GCFluids.OIL.get(), 24_000), IFluidHandler.FluidAction.EXECUTE);
        }
        // Fuel out: pull pipe, an orange pipe, and a tank.
        final BlockPos fuelTank = origin.offset(7, 0, 0);
        level.setBlockAndUpdate(fuelTank, GCBlocks.FLUID_TANK.get().defaultBlockState());
        final BlockState pipe = GCBlocks.FLUID_PIPE.get().defaultBlockState();
        pipe(level, origin.offset(2, 0, 0), pipe.setValue(FluidPipeBlock.PULL, true));
        pipe(level, origin.offset(3, 0, 0), pipe);
        pipe(level, origin.offset(5, 0, 0), pipe.setValue(FluidPipeBlock.PULL, true));
        pipe(level, origin.offset(6, 0, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.ORANGE));
        // A loose pipe run on top, to show the colours and vertical joints.
        pipe(level, origin.offset(3, 1, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIME));
        pipe(level, origin.offset(3, 2, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIME));
        pipe(level, origin.offset(4, 2, 0), pipe.setValue(FluidPipeBlock.COLOR, DyeColor.LIGHT_BLUE));

        level.setBlockAndUpdate(origin.offset(10, -1, 0), GCFluids.OIL_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(origin.offset(10, -1, 2), GCFluids.FUEL_BLOCK.get().defaultBlockState());
    }

    /**
     * Collector (with leaves), storage module, compressor, decompressor, detector and a running distributor with its
     * bubble, all facing the camera; their GUIs are added to the tour.
     */
    private void buildOxygenScene(ServerLevel level, BlockPos origin) {
        this.oxygenScene = origin;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-9, 0, -4), origin.offset(12, 6, 4))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        final BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-7, 0, -2), origin.offset(-5, 1, -1))) {
            level.setBlockAndUpdate(pos, leaves);
        }
        final Block[] row = {GCBlocks.OXYGEN_COLLECTOR.get(), GCBlocks.OXYGEN_STORAGE_MODULE.get(), GCBlocks.OXYGEN_COMPRESSOR.get(),
                GCBlocks.OXYGEN_DECOMPRESSOR.get(), GCBlocks.OXYGEN_SEALER.get(), GCBlocks.OXYGEN_DISTRIBUTOR.get()};
        for (int i = 0; i < row.length; i++) {
            final BlockPos pos = origin.offset(-6 + i * 2, 0, 0);
            level.setBlockAndUpdate(pos, row[i].defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
            if (level.getBlockEntity(pos) instanceof OxygenMachineBlockEntity machine) {
                machine.energy().setStored(machine.energy().getCapacityGJ());
                machine.setOxygenStored(machine.oxygenTank().getCapacity() / 2);
                if (machine instanceof OxygenCompressorBlockEntity) {
                    final ItemStack tank = new ItemStack(GCItems.MEDIUM_OXYGEN_TANK.get());
                    OxygenTankItem.setOxygen(tank, 700);
                    machine.inventory().setStackInSlot(0, tank);
                } else if (machine instanceof OxygenDecompressorBlockEntity) {
                    machine.inventory().setStackInSlot(0, GCItems.LIGHT_OXYGEN_TANK.get().full());
                } else if (machine instanceof OxygenSealerBlockEntity) {
                    machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                    machine.inventory().setStackInSlot(2, new ItemStack(GCItems.AMBIENT_THERMAL_CONTROLLER.get()));
                } else if (machine instanceof OxygenDistributorBlockEntity distributor) {
                    distributor.setBubbleSize(3.0F);
                    machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
                }
            }
            this.machines.add(pos);
        }
        level.setBlockAndUpdate(origin.offset(6, 0, 0), GCBlocks.OXYGEN_DETECTOR.get().defaultBlockState());
    }

    /** Teleport to the Moon (survival, wearing gear) beside a sealed glass room and an unsealed torch. */
    private static void buildMoonScene(MinecraftServer server) {
        final ServerLevel moon = server.getLevel(GCDimensions.MOON);
        final ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (moon == null) {
            GalacticraftCore.LOGGER.warn("Screenshot tour: no Moon dimension");
            return;
        }
        moon.getChunk(0, 0);
        final int y = moon.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 2, 2);
        final BlockPos origin = new BlockPos(0, y, 0);
        moon.setDayTime(6000);
        // A 5x4x5 glass box, hollow, on a floor of moon rock
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, -6), origin.offset(6, 6, 6))) {
            moon.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -6), origin.offset(6, -1, 6))) {
            moon.setBlockAndUpdate(pos, GCBlocks.MOON_ROCK.get().defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, 0, -4), origin.offset(4, 4, 0))) {
            final boolean wall = pos.getX() == origin.getX() || pos.getX() == origin.getX() + 4 || pos.getZ() == origin.getZ() - 4
                    || pos.getZ() == origin.getZ() || pos.getY() == origin.getY() + 4;
            moon.setBlockAndUpdate(pos, wall ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        final BlockPos sealer = origin.offset(2, 0, -2);
        moon.setBlockAndUpdate(sealer, GCBlocks.OXYGEN_SEALER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        if (moon.getBlockEntity(sealer) instanceof OxygenSealerBlockEntity machine) {
            machine.energy().setStored(machine.energy().getCapacityGJ());
            machine.inventory().setStackInSlot(1, new ItemStack(GCItems.INFINITE_OXYGEN.get()));
        }
        moon.setBlockAndUpdate(origin.offset(1, 0, -3), GCBlocks.UNLIT_TORCH.get().defaultBlockState());
        moon.setBlockAndUpdate(origin.offset(-2, 0, -2), GCBlocks.UNLIT_TORCH.get().defaultBlockState());
        player.setGameMode(GameType.SURVIVAL);
        final GCPlayerData data = GCPlayerData.get(player);
        data.setGear(GearSlot.MASK, new ItemStack(GCItems.OXYGEN_MASK.get()));
        player.teleportTo(moon, origin.getX() + 1.0, origin.getY(), origin.getZ() + 5.5, 200.0F, 20.0F);
    }

    private static void pipe(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, ((FluidPipeBlock) state.getBlock()).withConnections(level, pos, state));
    }

    private static void fill(ServerLevel level, BlockPos pos, java.util.function.Consumer<MachineBlockEntity> filler) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            filler.accept(machine);
        }
    }
}
