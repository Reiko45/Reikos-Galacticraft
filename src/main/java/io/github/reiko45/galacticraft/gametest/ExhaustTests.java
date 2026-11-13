package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.entity.GasLiquefierBlockEntity;
import io.github.reiko45.galacticraft.entity.RocketEffects;
import io.github.reiko45.galacticraft.entity.Tier1RocketEntity;
import io.github.reiko45.galacticraft.item.LiquidCanisterItem;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10 part 4: 1.12.2's exhaust (launch flames, their burns) and the Gas Liquefier's mist. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExhaustTests {
    private static final String AIR = "air_room";

    private ExhaustTests() {
    }

    private static Pig pig(GameTestHelper helper, Vec3 at) {
        final Pig pig = EntityType.PIG.create(helper.getLevel());
        pig.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static Tier1RocketEntity rocket(GameTestHelper helper, int fuel) {
        final BlockPos abs = helper.absolutePos(new BlockPos(6, 1, 6));
        final Tier1RocketEntity rocket = Tier1RocketEntity.create(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 0.4D, abs.getZ() + 0.5D, 0, fuel);
        rocket.setNoGravity(true);
        helper.getLevel().addFreshEntity(rocket);
        return rocket;
    }

    /**
     * 1.12.2 ParticleLaunchFlame: the flames round a rocket counting down set a pig beside it alight; not its crew, nor a
     * pig six blocks off.
     */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void countdownFlamesBurnOnlookers(GameTestHelper helper) {
        final Tier1RocketEntity rocket = rocket(helper, Tier1RocketEntity.FUEL_CAPACITY);
        final ServerPlayer crew = helper.makeMockServerPlayerInLevel();
        crew.startRiding(rocket, true);
        rocket.ignite();
        helper.assertValueEqual(rocket.phase(), Tier1RocketEntity.Phase.IGNITED, "counting down");
        final Pig near = pig(helper, rocket.position().add(2.5D, -0.4D, 0.0D));
        final Pig far = pig(helper, rocket.position().add(6.0D, -0.4D, 0.0D));
        helper.succeedWhen(() -> {
            helper.assertTrue(near.isOnFire(), "the pig beside the rocket burns");
            helper.assertFalse(far.isOnFire(), "the pig six blocks off doesn't");
            helper.assertFalse(crew.isOnFire(), "the crew don't");
            crew.stopRiding();
            rocket.discard();
            near.discard();
            far.discard();
        });
    }

    /** 1.12.2 hasValidFuel: an empty rocket makes no flames, so it burns nothing. */
    @GameTest(template = AIR, timeoutTicks = 100)
    public static void emptyRocketBurnsNothing(GameTestHelper helper) {
        final Tier1RocketEntity rocket = rocket(helper, 0);
        rocket.ignite();
        final Pig near = pig(helper, rocket.position().add(2.0D, -0.4D, 0.0D));
        helper.runAfterDelay(12, () -> {
            helper.assertFalse(near.isOnFire(), "no fuel, no flames");
            rocket.discard();
            near.discard();
            helper.succeed();
        });
    }

    /**
     * 1.12.2 Vector3#rotate(angle, axis), which spreads the Tier 3 Rocket's four jets round its exhaust: a quarter turn
     * about straight down takes +X to -Z; nothing moves along the axis.
     */
    @GameTest(template = AIR)
    public static void jetsTurnAboutTheExhaust(GameTestHelper helper) {
        final double[] quarter = RocketEffects.rotate(new double[] {1.0D, 0.0D, 0.0D}, 90.0F, new double[] {0.0D, -3.2D, 0.0D});
        helper.assertTrue(Math.abs(quarter[0]) < 1.0E-6D && Math.abs(quarter[1]) < 1.0E-6D && Math.abs(quarter[2] + 1.0D) < 1.0E-6D,
                "+X to -Z: " + quarter[0] + ", " + quarter[1] + ", " + quarter[2]);
        final double[] along = RocketEffects.rotate(new double[] {0.0D, 2.0D, 0.0D}, 37.0F, new double[] {0.0D, -1.0D, 0.0D});
        helper.assertTrue(Math.abs(along[1] - 2.0D) < 1.0E-6D && Math.abs(along[0]) < 1.0E-6D, "along the axis: unchanged");
        final double[] none = RocketEffects.rotate(new double[] {0.32D, 0.0D, 0.0D}, 45.0F, new double[] {0.0D, 0.0D, 0.0D});
        helper.assertValueEqual(none[0], 0.32D, "no axis, no turn");
        helper.succeed();
    }

    /**
     * 1.12.2 BlockMachineMarsT2#randomDisplayTick showed mist while the liquefier worked (processTicks, synced): the
     * client is told when it starts.
     */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void liquefierTellsClientsItMists(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, GCBlocks.GAS_LIQUEFIER.get());
        final GasLiquefierBlockEntity machine = (GasLiquefierBlockEntity) helper.getBlockEntity(pos);
        helper.assertFalse(machine.misting(), "idle");
        helper.assertFalse(machine.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("Misting"), "idle on the client");
        machine.energy().setStored(16_000.0F);
        machine.inventory().setStackInSlot(1, LiquidCanisterItem.full(LiquidCanisterItem.Kind.METHANE));
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.misting(), "liquefying");
            helper.assertTrue(machine.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("Misting"), "sent to clients");
        });
    }
}
