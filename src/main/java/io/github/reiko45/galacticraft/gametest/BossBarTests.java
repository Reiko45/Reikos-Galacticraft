package io.github.reiko45.galacticraft.gametest;

import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.DungeonBossEntity;
import io.github.reiko45.galacticraft.registry.GCEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The dungeon bosses' health bars (1.12.2 EntityBossBase's BossInfoServer): shown near the boss, and gone after it. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossBarTests {
    private static final String AIR = "air_room";

    private BossBarTests() {
    }

    /**
     * A player near a boss is shown its bar, but not from 100 blocks above it; the bar goes when the boss goes (despawned,
     * as when everyone leaves its room), and when the player leaves for another world.
     */
    @GameTest(template = AIR, timeoutTicks = 200)
    public static void bossBarsGoWithTheBossOrThePlayer(GameTestHelper helper) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final Vec3 at = helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        player.teleportTo(at.x, at.y, at.z);
        final DungeonBossEntity first = boss(helper);
        final DungeonBossEntity[] second = {null};
        final int[] stage = {0};
        helper.succeedWhen(() -> {
            if (stage[0] == 0) {
                helper.assertTrue(first.bossBarViewers().contains(player), "shown the bar");
                player.teleportTo(at.x, at.y + 100.0D, at.z);
                first.aiStep();
                helper.assertFalse(first.bossBarViewers().contains(player), "no bar 100 blocks above the boss");
                player.teleportTo(at.x, at.y, at.z);
                first.aiStep();
                helper.assertTrue(first.bossBarViewers().contains(player), "the bar back on coming down");
                first.discard();
                helper.assertTrue(first.bossBarViewers().isEmpty(), "the despawned boss's bar is gone");
                second[0] = boss(helper);
                stage[0] = 1;
                throw new GameTestAssertException("a second boss");
            }
            if (stage[0] == 1) {
                helper.assertTrue(second[0].bossBarViewers().contains(player), "shown the second boss's bar");
                // (the GameTest server has no Moon; any other world will do)
                final ServerLevel other = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
                helper.assertTrue(other != null, "another world");
                player.teleportTo(other, 0.5D, 120.0D, 0.5D, 0.0F, 0.0F);
                stage[0] = 2;
                throw new GameTestAssertException("on to another world");
            }
            helper.assertFalse(second[0].bossBarViewers().contains(player), "the bar went when the player left for another world");
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    private static DungeonBossEntity boss(GameTestHelper helper) {
        final DungeonBossEntity boss = helper.spawn(GCEntities.CREEPER_BOSS.get(), 7, 1, 4);
        boss.setNoAi(true);
        boss.setPersistenceRequired();
        return boss;
    }
}
