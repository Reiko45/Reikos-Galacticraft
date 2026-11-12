package io.github.reiko45.galacticraft.gametest;

import com.mojang.authlib.GameProfile;
import io.github.reiko45.galacticraft.GalacticraftCore;
import io.github.reiko45.galacticraft.entity.FlagEntity;
import io.github.reiko45.galacticraft.item.FlagItem;
import io.github.reiko45.galacticraft.menu.Schematics;
import io.github.reiko45.galacticraft.player.GCPlayerData;
import io.github.reiko45.galacticraft.race.FlagData;
import io.github.reiko45.galacticraft.race.SpaceRace;
import io.github.reiko45.galacticraft.race.SpaceRaces;
import io.github.reiko45.galacticraft.registry.GCItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 6b: Space Race teams (1.12.2 SpaceRaceManager and its PacketSimple cases) and the flag. */
@GameTestHolder(GalacticraftCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceRaceTests {
    private SpaceRaceTests() {
    }

    /**
     * A logged-in player with a name of its own (vanilla's mock players are all "test-mock-player", and invitations go
     * by name), in survival, standing at {@code pos}.
     */
    private static ServerPlayer player(GameTestHelper helper, String name, BlockPos pos) {
        final GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("gc-race-" + name + "-" + System.nanoTime()).getBytes()), name);
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        final ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        final BlockPos at = helper.absolutePos(pos);
        player.teleportTo(helper.getLevel(), at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    private static void logOut(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer player : players) {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static String unique(String name) {
        return name + Long.toString(System.nanoTime() % 100000L, 36);
    }

    private static FlagData striped(int rgb) {
        final FlagData flag = new FlagData();
        for (int x = 0; x < flag.width(); x += 2) {
            for (int y = 0; y < flag.height(); y++) {
                flag.setColorAt(x, y, rgb);
            }
        }
        return flag;
    }

    /**
     * Opening the manager gives a player a team of their own; only its owner can rename it (trimmed, at most 25
     * characters, blank meaning "unnamed"), redraw its flag or change its colour.
     */
    @GameTest(template = "empty")
    public static void onlyTheOwnerChangesTheTeam(GameTestHelper helper) {
        final SpaceRaces races = SpaceRaces.get(helper.getLevel().getServer());
        final ServerPlayer owner = player(helper, unique("Ann"), new BlockPos(0, 1, 0));
        final ServerPlayer other = player(helper, unique("Ben"), new BlockPos(0, 1, 0));
        final SpaceRace race = races.ensure(owner);
        helper.assertTrue(races.ensure(owner) == race, "one team per player");
        helper.assertTrue(race.isOwner(owner.getUUID()), "its maker owns it");
        helper.assertValueEqual(race.name(), SpaceRace.DEFAULT_NAME, "new teams have no name");
        races.update(owner, "  Lunar Pioneers  ", striped(0x123456), 0xE07020);
        helper.assertValueEqual(race.name(), "Lunar Pioneers", "renamed, trimmed");
        helper.assertValueEqual(race.color(), 0xE07020, "team colour");
        helper.assertValueEqual(race.flag().colorAt(0, 0), 0x123456, "the new flag");
        helper.assertValueEqual(race.flag().colorAt(1, 0), FlagData.WHITE, "the rest still white");
        races.update(owner, "A name far longer than twenty-five letters", race.flag(), race.color());
        helper.assertValueEqual(race.name().length(), SpaceRaces.MAX_NAME_LENGTH, "names stop at 25 characters");
        races.update(owner, "   ", race.flag(), race.color());
        helper.assertValueEqual(race.name(), SpaceRace.DEFAULT_NAME, "a blank name is no name");
        races.update(owner, "Lunar Pioneers", race.flag(), race.color());
        // Ben joins; his changes don't count
        races.invite(owner, other.getGameProfile().getName());
        races.join(other, race.id());
        races.update(other, "Hijacked", striped(0xFF0000), 0x00FF00);
        helper.assertValueEqual(race.name(), "Lunar Pioneers", "members can't rename it");
        helper.assertValueEqual(race.color(), 0xE07020, "or recolour it");
        helper.assertValueEqual(race.flag().colorAt(0, 0), 0x123456, "or redraw the flag");
        logOut(helper, owner, other);
        helper.succeed();
    }

    /**
     * The owner invites a player within 25 blocks of the same world; with the invitation they join (leaving their own
     * team, which goes once empty); without one they can't. The owner can remove members, who get a team of their own.
     */
    @GameTest(template = "empty")
    public static void invitationsJoinAndRemoval(GameTestHelper helper) {
        final SpaceRaces races = SpaceRaces.get(helper.getLevel().getServer());
        final ServerPlayer owner = player(helper, unique("Cat"), new BlockPos(0, 1, 0));
        final ServerPlayer near = player(helper, unique("Dan"), new BlockPos(0, 1, 0));
        final ServerPlayer far = player(helper, unique("Eve"), new BlockPos(0, 1, 0));
        far.teleportTo(helper.getLevel(), owner.getX() + 30.0D, owner.getY(), owner.getZ(), 0.0F, 0.0F);
        final SpaceRace race = races.ensure(owner);
        final SpaceRace old = races.ensure(near);
        helper.assertFalse(races.join(near, race.id()), "no joining without an invitation");
        helper.assertFalse(races.invite(owner, far.getGameProfile().getName()), "30 blocks is too far");
        helper.assertFalse(races.invite(owner, "nobody-online"), "only online players");
        helper.assertTrue(races.invite(owner, near.getGameProfile().getName()), "invited");
        helper.assertValueEqual(GCPlayerData.get(near).spaceRaceInvite, race.id(), "the invitation is noted");
        helper.assertTrue(races.join(near, race.id()), "joined");
        helper.assertTrue(race.has(near.getUUID()), "now a member");
        helper.assertValueEqual(race.members().size(), 2, "two members");
        helper.assertTrue(races.byId(old.id()) == null, "the empty old team is gone");
        helper.assertValueEqual(GCPlayerData.get(near).spaceRaceInvite, 0, "the invitation is used up");
        helper.assertFalse(races.join(near, race.id()), "and can't be used twice");
        helper.assertFalse(races.invite(near, far.getGameProfile().getName()), "members can't invite");
        helper.assertFalse(races.remove(near, owner.getGameProfile().getName()), "or remove anyone");
        helper.assertFalse(races.remove(owner, owner.getGameProfile().getName()), "the owner can't remove themselves");
        helper.assertTrue(races.remove(owner, near.getGameProfile().getName()), "removed");
        helper.assertFalse(race.has(near.getUUID()), "no longer a member");
        final SpaceRace own = races.of(near.getUUID());
        helper.assertTrue(own != null && own != race && own.isOwner(near.getUUID()), "a team of their own");
        logOut(helper, owner, near, far);
        helper.succeed();
    }

    /**
     * A schematic one member unlocks is unlocked for the whole team: at once for those online, at their next login for
     * the others.
     */
    @GameTest(template = "empty")
    public static void teamsShareSchematics(GameTestHelper helper) {
        final SpaceRaces races = SpaceRaces.get(helper.getLevel().getServer());
        final ServerPlayer owner = player(helper, unique("Fay"), new BlockPos(0, 1, 0));
        final ServerPlayer mate = player(helper, unique("Gus"), new BlockPos(0, 1, 0));
        final ServerPlayer loner = player(helper, unique("Hal"), new BlockPos(0, 1, 0));
        final SpaceRace race = races.ensure(owner);
        races.invite(owner, mate.getGameProfile().getName());
        races.join(mate, race.id());
        races.ensure(loner);
        helper.assertTrue(Schematics.unlock(owner, new ItemStack(GCItems.BUGGY_SCHEMATIC.get())), "unlocked");
        helper.assertTrue(GCPlayerData.get(mate).hasSchematic(Schematics.MOON_BUGGY.id()), "the teammate has it too");
        helper.assertFalse(GCPlayerData.get(loner).hasSchematic(Schematics.MOON_BUGGY.id()), "other teams don't");
        logOut(helper, mate);
        helper.assertTrue(Schematics.unlock(owner, new ItemStack(GCItems.TIER_2_ROCKET_SCHEMATIC.get())), "unlocked while the mate is away");
        helper.assertFalse(GCPlayerData.get(mate).hasSchematic(Schematics.ROCKET_T2.id()), "not yet");
        races.onLogin(mate);
        helper.assertTrue(GCPlayerData.get(mate).hasSchematic(Schematics.ROCKET_T2.id()), "waiting at the next login");
        helper.assertTrue(race.takePendingSchematics(mate.getUUID()).isEmpty(), "and handed over only once");
        logOut(helper, owner, loner);
        helper.succeed();
    }

    /** A team's clock runs while a member is online, and the first visit to each world is noted with the time. */
    @GameTest(template = "empty")
    public static void teamClockAndWorldsReached(GameTestHelper helper) {
        final SpaceRaces races = SpaceRaces.get(helper.getLevel().getServer());
        final ServerPlayer owner = player(helper, unique("Ida"), new BlockPos(0, 1, 0));
        final SpaceRace race = races.ensure(owner);
        final int before = race.ticksSpent();
        races.tick(helper.getLevel().getServer());
        helper.assertTrue(race.ticksSpent() > before, "the clock runs while a member is online");
        helper.assertTrue(race.worldsReached().containsKey(Level.OVERWORLD.location()), "the Overworld is reached");
        logOut(helper, owner);
        final int offline = race.ticksSpent();
        races.tick(helper.getLevel().getServer());
        helper.assertValueEqual(race.ticksSpent(), offline, "and stops while nobody is on");
        helper.succeed();
    }

    /** Teams survive a save: name, flag, colour, members, clock, worlds reached and schematics waiting for members. */
    @GameTest(template = "empty")
    public static void teamsSaveAndLoad(GameTestHelper helper) {
        final SpaceRace race = new SpaceRace(42);
        race.setName("Vostok Crew");
        race.setFlag(striped(0xC81E1E));
        race.setColor(0x1E8C3C);
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        race.members().add(new SpaceRace.Member(a, "Valentina"));
        race.members().add(new SpaceRace.Member(b, "Yuri"));
        race.tick();
        race.tick();
        race.reached(ResourceLocation.fromNamespaceAndPath(GalacticraftCore.MOD_ID, "moon"));
        race.addPendingSchematic(b, "rocket_t2");
        final SpaceRace copy = SpaceRace.load(race.save().copy());
        helper.assertValueEqual(copy.id(), 42, "id");
        helper.assertValueEqual(copy.name(), "Vostok Crew", "name");
        helper.assertTrue(copy.flag().equals(race.flag()), "flag");
        helper.assertValueEqual(copy.color(), 0x1E8C3C, "colour");
        helper.assertValueEqual(copy.members(), race.members(), "members, owner first");
        helper.assertValueEqual(copy.ticksSpent(), 2, "clock");
        helper.assertValueEqual(copy.worldsReached().get(ResourceLocation.fromNamespaceAndPath(GalacticraftCore.MOD_ID, "moon")), 2, "worlds reached");
        helper.assertValueEqual(copy.takePendingSchematics(b), List.of("rocket_t2"), "waiting schematics");
        final CompoundTag tag = new CompoundTag();
        race.flag().save(tag);
        helper.assertTrue(FlagData.load(tag).equals(race.flag()), "flags save on their own too");
        helper.succeed();
    }

    /**
     * The flag goes in once it has been held up for a second ((t^2 + 2t) / 3 reaches 1 at t = 1); one per block; a
     * survival player uses it up.
     */
    @GameTest(template = "air_room", skyAccess = true)
    public static void flagsArePlanted(GameTestHelper helper) {
        helper.assertTrue(FlagItem.charge(19) < 1.0F, "19 ticks is not enough");
        helper.assertValueEqual(FlagItem.charge(20), 1.0F, "20 ticks is");
        final BlockPos ground = new BlockPos(6, 1, 6);
        helper.setBlock(ground, Blocks.STONE);
        final ServerPlayer player = player(helper, unique("Jo"), new BlockPos(6, 2, 4));
        final ItemStack stack = new ItemStack(GCItems.FLAG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos at = helper.absolutePos(ground);
        helper.assertTrue(FlagItem.plant(helper.getLevel(), player, stack, at), "planted");
        helper.assertTrue(stack.isEmpty(), "used up");
        final List<FlagEntity> flags = helper.getLevel().getEntitiesOfClass(FlagEntity.class, new AABB(at).expandTowards(0, 3, 0));
        helper.assertValueEqual(flags.size(), 1, "one flag");
        final FlagEntity flag = flags.get(0);
        helper.assertTrue(flag.owner().equals(player.getUUID()), "flying the planter's flag");
        helper.assertValueEqual(flag.getY(), at.getY() + 1.0D, "standing on the block");
        final ItemStack second = new ItemStack(GCItems.FLAG.get());
        helper.assertFalse(FlagItem.plant(helper.getLevel(), player, second, at), "one flag per block");
        helper.assertValueEqual(second.getCount(), 1, "the second flag is kept");
        flag.discard();
        logOut(helper, player);
        helper.succeed();
    }

    /**
     * Hits do ten times their damage and break it past 40, dropping the flag; a creative player knocks it down at once
     * and gets nothing. Right-clicking turns it 3 degrees. Over air it falls.
     */
    @GameTest(template = "air_room", skyAccess = true)
    public static void flagsBreakTurnAndFall(GameTestHelper helper) {
        final BlockPos ground = new BlockPos(6, 1, 6);
        helper.setBlock(ground, Blocks.STONE);
        final ServerPlayer player = player(helper, unique("Kim"), new BlockPos(6, 2, 4));
        final BlockPos at = helper.absolutePos(ground);
        final FlagEntity flag = FlagEntity.create(helper.getLevel(), at.getX() + 0.5D, at.getY() + 1.0D, at.getZ() + 0.5D, 90, player);
        helper.getLevel().addFreshEntity(flag);
        flag.interact(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(flag.facingAngle(), 93, "turned 3 degrees");
        final var hit = helper.getLevel().damageSources().playerAttack(player);
        flag.hurt(hit, 2.0F);
        flag.hurt(hit, 2.0F);
        helper.assertValueEqual(flag.damage(), 40.0F, "ten times the damage");
        helper.assertFalse(flag.isRemoved(), "40 is not enough");
        flag.hurt(hit, 0.5F);
        helper.assertTrue(flag.isRemoved(), "broken past 40");
        final AABB around = new AABB(at).inflate(2.0D, 3.0D, 2.0D);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(GCItems.FLAG.get())).size() == 1,
                "and dropped");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
        final FlagEntity second = FlagEntity.create(helper.getLevel(), at.getX() + 0.5D, at.getY() + 1.0D, at.getZ() + 0.5D, 0, player);
        helper.getLevel().addFreshEntity(second);
        player.setGameMode(GameType.CREATIVE);
        second.hurt(hit, 0.1F);
        helper.assertTrue(second.isRemoved(), "a creative player knocks it down at once");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "with no drop");
        // One over air, two blocks up: it comes down onto the stone
        final FlagEntity falling = FlagEntity.create(helper.getLevel(), at.getX() + 0.5D, at.getY() + 3.0D, at.getZ() + 0.5D, 0, player);
        helper.getLevel().addFreshEntity(falling);
        logOut(helper, player);
        helper.succeedWhen(() -> {
            helper.assertTrue(falling.onGround() && Math.abs(falling.getY() - (at.getY() + 1.0D)) < 1.0E-3, "landed on the stone");
            falling.discard();
        });
    }
}
