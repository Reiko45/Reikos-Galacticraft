package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.block.LaserTurretBlock;
import io.github.reiko45.galacticraft.block.entity.LaserTurretBlockEntity;
import io.github.reiko45.galacticraft.block.entity.SolarPartBlockEntity;
import io.github.reiko45.galacticraft.entity.MeteorEntity;
import io.github.reiko45.galacticraft.menu.LaserTurretMenu;
import io.github.reiko45.galacticraft.registry.GCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Phase 7g part 4: the Laser Turret. Turrets left in whitelist mode would fire at the next test's mobs and players, so
 * the one that really fires is in blacklist mode with mooshrooms only (no other test uses them), and the others are
 * asked who they would pick without being powered.
 */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaserTurretTests {
    private static final String AIR = "air_room";

    private LaserTurretTests() {
    }

    private static LaserTurretBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, GCBlocks.LASER_TURRET.get());
        LaserTurretBlock.placePart(helper.getLevel(), helper.absolutePos(pos));
        return helper.getBlockEntity(pos);
    }

    private static <T extends Mob> T still(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        final T mob = helper.spawn(type, pos);
        mob.setNoAi(true);
        return mob;
    }

    /** Charged up from 16,000 gJ, it fires at the listed mooshroom (1.5 damage) and not the cow, for 1,000 gJ. */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void turretShootsListedMob(GameTestHelper helper) {
        final LaserTurretBlockEntity turret = place(helper, new BlockPos(2, 1, 2));
        turret.setTargetMeteors(false);
        turret.setBlacklistMode(true);
        turret.addEntity(EntityType.getKey(EntityType.MOOSHROOM));
        turret.energy().setStored(LaserTurretBlockEntity.CAPACITY);
        final Mob cow = still(helper, EntityType.COW, new BlockPos(3, 1, 9));
        final Mob mooshroom = still(helper, EntityType.MOOSHROOM, new BlockPos(9, 1, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(mooshroom.getHealth() <= mooshroom.getMaxHealth() - LaserTurretBlockEntity.DAMAGE, "the mooshroom was hit");
            helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "the cow wasn't");
            helper.assertTrue(turret.energy().getStoredGJ() <= LaserTurretBlockEntity.CAPACITY - LaserTurretBlockEntity.SHOT_ENERGY, "a shot costs 1,000 gJ");
            helper.assertTrue(turret.chargeLevel() < LaserTurretBlockEntity.FULL_CHARGE, "charging again");
        });
    }

    /**
     * Whitelist mode fires at everything not listed; the owner (listed when placing) and the owner's pets are spared;
     * blacklist mode fires only at what is listed, the owner too once "never fire at your team" is off (as in 1.12.2).
     */
    @GameTest(template = AIR)
    public static void turretListsOwnerAndPets(GameTestHelper helper) {
        final LaserTurretBlockEntity turret = place(helper, new BlockPos(2, 1, 2));
        final ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        final ServerPlayer visitor = helper.makeMockServerPlayerInLevel();
        turret.setOwner(owner);
        helper.assertTrue(turret.players().contains(owner.getGameProfile().getName()), "the owner goes on the list");
        final Mob sheep = still(helper, EntityType.SHEEP, new BlockPos(6, 1, 6));
        final Wolf wolf = still(helper, EntityType.WOLF, new BlockPos(7, 1, 6));
        wolf.tame(owner);
        helper.assertFalse(turret.shouldTarget(owner), "never the owner");
        helper.assertTrue(turret.shouldTarget(sheep), "whitelist mode: anything not listed");
        helper.assertFalse(turret.shouldTarget(wolf), "never the owner's pets");
        turret.addEntity(EntityType.getKey(EntityType.SHEEP));
        helper.assertFalse(turret.shouldTarget(sheep), "listed sheep are spared");
        turret.setBlacklistMode(true);
        helper.assertTrue(turret.shouldTarget(sheep), "blacklist mode: listed sheep are targets");
        helper.assertFalse(turret.shouldTarget(owner), "still never the owner");
        turret.setAlwaysIgnoreSpaceRace(false);
        helper.assertTrue(turret.shouldTarget(owner), "1.12.2: the listed owner, with the team rule off");
        helper.assertFalse(turret.shouldTarget(wolf), "pets stay safe");
        // only the owner may press its buttons
        helper.assertFalse(new LaserTurretMenu(0, visitor.getInventory(), turret).clickMenuButton(visitor, LaserTurretMenu.BUTTON_ENABLE),
                "a visitor can't switch it off");
        helper.assertTrue(new LaserTurretMenu(0, owner.getInventory(), turret).clickMenuButton(owner, LaserTurretMenu.BUTTON_ENABLE)
                && turret.isDisabled(), "the owner can");
        helper.succeed();
    }

    /** The priorities pick between the near healthy goat and the far hurt one; a wall hides the nearer. */
    @GameTest(template = AIR)
    public static void turretPrioritiesAndSight(GameTestHelper helper) {
        final LaserTurretBlockEntity turret = place(helper, new BlockPos(2, 1, 2));
        turret.setTargetMeteors(false);
        turret.setBlacklistMode(true);
        turret.addEntity(EntityType.getKey(EntityType.GOAT));
        final Mob near = still(helper, EntityType.GOAT, new BlockPos(2, 1, 6));
        final Mob far = still(helper, EntityType.GOAT, new BlockPos(2, 1, 11));
        far.setHealth(2.0F);
        helper.assertTrue(turret.findTarget() == near, "closest first");
        turret.setPriorities(3, 1, 2);
        helper.assertTrue(turret.findTarget() == far, "lowest health first");
        turret.setPriorities(2, 3, 1);
        helper.assertTrue(turret.findTarget() == near, "highest health first");
        turret.setPriorities(1, 2, 3);
        for (int x = 0; x <= 4; x++) {
            for (int y = 1; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
            }
        }
        helper.assertTrue(turret.findTarget() == null, "both behind the wall");
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.AIR);
        helper.assertTrue(turret.findTarget() == near, "seen through the gap");
        helper.succeed();
    }

    /** A meteor 40 blocks up and 30 across is picked before any mob (once it is listed, in blacklist mode), only while Target Meteors is on. */
    @GameTest(template = AIR)
    public static void turretTargetsMeteors(GameTestHelper helper) {
        final LaserTurretBlockEntity turret = place(helper, new BlockPos(2, 1, 2));
        turret.setBlacklistMode(true);
        turret.addEntity(EntityType.getKey(EntityType.SHEEP));
        still(helper, EntityType.SHEEP, new BlockPos(2, 1, 6));
        final Vec3 at = helper.absoluteVec(new Vec3(32.5D, 41.0D, 2.5D));
        final MeteorEntity meteor = MeteorEntity.create(helper.getLevel(), at.x, at.y, at.z, 0.0D, 0.0D, 0.0D, 1);
        helper.getLevel().addFreshEntity(meteor);
        helper.assertFalse(turret.findTarget() == meteor, "1.12.2: blacklist mode fires only at what is listed, meteors too");
        turret.addEntity(EntityType.getKey(meteor.getType()));
        helper.assertTrue(turret.findTarget() == meteor, "the meteor first");
        turret.setTargetMeteors(false);
        helper.assertFalse(turret.findTarget() == meteor, "not with meteors off");
        meteor.discard();
        helper.succeed();
    }

    /** It stands on an invisible part block; breaking either breaks both. */
    @GameTest(template = AIR)
    public static void turretPartBlock(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 1, 2);
        place(helper, pos);
        helper.assertBlockPresent(GCBlocks.LASER_TURRET_PART.get(), pos.above());
        final SolarPartBlockEntity part = helper.getBlockEntity(pos.above());
        helper.assertTrue(helper.absolutePos(pos).equals(part.mainPos()), "the part knows its turret");
        helper.destroyBlock(pos.above());
        helper.assertBlockNotPresent(GCBlocks.LASER_TURRET.get(), pos);
        place(helper, pos);
        helper.destroyBlock(pos);
        helper.assertBlockNotPresent(GCBlocks.LASER_TURRET_PART.get(), pos.above());
        helper.succeed();
    }
}
