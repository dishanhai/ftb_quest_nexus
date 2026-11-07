package com.dishanhai.ftb_quest_nexus.submitter;

// Author: dishanhai. Real block/FTBQ/AE regression paths; only network power is bypassed.
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.IStorageProvider;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqAutoClaimDeferral;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SubmitterRegressionGameTests {
    private SubmitterRegressionGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void foreignTeamCannotRebind(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Initial binding failed");
            CompoundTag before = userData(f);
            ServerPlayer outsider = player(helper);
            outsider.setPos(f.owner.position());
            helper.assertTrue(!f.entity.canManage(outsider), "Fixture did not use separate teams");
            helper.assertTrue(!f.entity.applyAction(outsider, 0), "Foreign player rebound the submitter");
            helper.assertTrue(before.equals(userData(f)), "Rejected binding changed user data");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void sameTeamCanRebindButPendingRewardsPreventIt(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Initial binding failed");
            CompoundTag departed = userData(f);
            departed.putString("rewardPlayerIdText", UUID.randomUUID().toString());
            f.entity.loadUserData(departed);
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Remaining team member could not rebind");
            helper.assertTrue(f.owner.getUUID().toString().equals(f.entity.getRewardPlayerIdText()),
                    "Reward player was not replaced");
            helper.assertTrue(f.entity.getQueueSize() == 1, "Same-team rebind discarded queued targets");
            CompoundTag user = userData(f);
            net.minecraft.nbt.ListTag pending = new net.minecraft.nbt.ListTag();
            CompoundTag entry = new CompoundTag();
            entry.put("key", AEItemKey.of(Items.GOLD_INGOT).toTag());
            entry.putLong("amount", 3);
            pending.add(entry);
            user.put("FtbqPendingRewards", pending);
            f.entity.loadUserData(user);
            f.entity.applyAction(f.owner, 0);
            helper.assertTrue(user.equals(userData(f)), "Rebinding altered pending rewards");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void registrationIncludesEveryEligibleItemTarget(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemTask second = itemTask(f.file, f.quest, Items.GOLD_INGOT);
            ItemTask nonConsuming = itemTask(f.file, f.quest, Items.DIAMOND);
            nonConsuming.setConsumeItems(Tristate.FALSE);
            ItemTask crafting = new ItemTask(f.file.newID(), f.quest) {
                @Override public boolean isOnlyFromCrafting() { return true; }
            };
            crafting.setConsumeItems(Tristate.TRUE);
            f.quest.addTask(crafting);
            f.file.refreshIDMap();
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Binding failed");
            UUID id = UUID.fromString(f.entity.getSubmitterIdText());
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, id, f.task.id), "Registration rejected");
            helper.assertTrue(f.entity.getQueueSize() == 2, "Registration omitted a sibling item target");
            String queued = userData(f).getString("queuedTaskIdsText");
            helper.assertTrue(queued.contains(Long.toUnsignedString(second.id, 16)), "Second target missing");
            SubmitterBlockEntity.queueTask(f.owner, id, f.task.id);
            helper.assertTrue(f.entity.getQueueSize() == 2, "Repeated registration duplicated targets");
            ServerPlayer outsider = player(helper);
            helper.assertTrue(!SubmitterBlockEntity.queueTask(outsider, id, f.task.id),
                    "Foreign player registered targets");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void automaticPersonalRewardReturnsToAeWithoutTakingTeammateReward(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemReward reward = automaticReward(f, false);
            runWork(f);
            AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
            helper.assertTrue(f.storage.items.get(gold) == 8, "Automatic reward bypassed AE: iron="
                    + f.storage.items.get(AEItemKey.of(Items.IRON_INGOT)) + ", progress=" + f.data.getProgress(f.task)
                    + ", claimed=" + f.data.isRewardClaimed(f.owner.getUUID(), reward)
                    + ", status=" + f.entity.getStatusComponent().getString());
            helper.assertTrue(!f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Bound player also received the reward");
            helper.assertTrue(f.teammate.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Submitter suppressed another player's personal reward");
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), reward), "Reward claim was not recorded");
            runWork(f);
            helper.assertTrue(f.storage.items.get(gold) == 8, "Automatic reward duplicated on retry");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void automaticTeamRewardReturnsToAeEvenWithAnotherPlayerContext(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            automaticReward(f, true);
            f.file.withPlayerContext(f.teammate, () -> runWork(f));
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Team reward was claimed by another player before AE");
            helper.assertTrue(!f.teammate.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Team reward also reached a player");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fullAeRetainsAutomaticRewardAndRetriesExactlyOnce(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            automaticReward(f, false);
            f.storage.capacity = 0;
            runWork(f);
            helper.assertTrue(userData(f).getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 8,
                    "Full AE lost the deferred reward");
            helper.assertTrue(!f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Full AE sent the reward to the player");
            f.storage.capacity = 100;
            runWork(f);
            helper.assertTrue(userData(f).getList("FtbqPendingRewards", 10).isEmpty(), "Retry did not flush");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Retry lost or duplicated the automatic reward");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void unsupportedAutomaticRewardsKeepVanillaDelivery(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            XPReward xp = new XPReward(f.file.newID(), f.quest, 12);
            configureAutomaticReward(f, xp, false);
            automaticReward(f, false);
            int before = f.owner.totalExperience;
            runWork(f);
            helper.assertTrue(f.owner.totalExperience == before + 12, "XP auto-claim was suppressed");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8, "Item reward missed AE");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void claimScopeRestoresAfterNestedFailure(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemReward reward = automaticReward(f, false);
            try {
                FtbqAutoClaimDeferral.run(f.data, QuestWork::canClaim, () -> {
                    helper.assertTrue(FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                            "Owner's reward was not reserved");
                    helper.assertTrue(!FtbqAutoClaimDeferral.shouldDefer(
                                    new TeamData(UUID.randomUUID(), f.file), reward),
                            "Scope intercepted another team's rewards");
                    try {
                        FtbqAutoClaimDeferral.run(f.data, r -> false, () -> {
                            throw new IllegalStateException("nested regression probe");
                        });
                    } catch (IllegalStateException expected) {
                        helper.assertTrue(FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                                "Nested failure did not restore outer scope");
                    }
                    throw new IllegalStateException("outer regression probe");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(!FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                        "Failed submission leaked its reward scope");
            }
            f.data.addProgress(f.task, 10);
            helper.assertTrue(f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Later ordinary completion no longer uses vanilla rewards");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void repeatablePersonalRewardCannotResetBeforeAeClaims(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            CompoundTag config = new CompoundTag();
            f.quest.writeData(config);
            config.putBoolean("can_repeat", true);
            f.quest.readData(config);
            automaticReward(f, false);
            runWork(f);
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Teammate auto-claim reset the repeatable quest before AE delivery");
        });
    }

    private static ItemReward automaticReward(Fixture f, boolean team) {
        ItemReward reward = new ItemReward(f.file.newID(), f.quest, new ItemStack(Items.GOLD_INGOT), 8);
        configureAutomaticReward(f, reward, team);
        return reward;
    }

    private static void configureAutomaticReward(Fixture f, Reward reward, boolean team) {
        CompoundTag config = new CompoundTag();
        reward.writeData(config);
        config.putString("auto", "enabled");
        config.putBoolean("team_reward", team);
        reward.readData(config);
        f.quest.addReward(reward);
        f.file.refreshIDMap();
    }

    private static void runWork(Fixture f) {
        helperReflect(() -> {
            // Exercise runQueuedWork directly: channel/power tests belong to AE, not this regression.
            var online = SubmitterBlockEntity.class.getDeclaredField("online");
            online.setAccessible(true);
            online.setBoolean(f.entity, true);
            var work = SubmitterBlockEntity.class.getDeclaredMethod("runQueuedWork");
            work.setAccessible(true);
            work.invoke(f.entity);
        });
    }

    private static CompoundTag userData(Fixture f) {
        CompoundTag tag = new CompoundTag();
        f.entity.saveUserData(tag);
        return tag;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "NexusRegression"));
        TeamManagerImpl.INSTANCE.playerLoggedIn(null, id, "NexusRegression");
        return player;
    }

    private static ItemTask itemTask(ServerQuestFile file, Quest quest, net.minecraft.world.item.Item item) {
        ItemTask task = new ItemTask(file.newID(), quest).setStackAndCount(new ItemStack(item), 10);
        task.setConsumeItems(Tristate.TRUE);
        quest.addTask(task);
        return task;
    }

    private static void withSubmitter(GameTestHelper helper, Consumer<Fixture> test) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        helper.runAfterDelay(20, () -> {
            ServerQuestFile previous = ServerQuestFile.INSTANCE;
            SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
            ServerPlayer owner = player(helper);
            ServerPlayer teammate = player(helper);
            owner.setPos(helper.absolutePos(pos).getX() + .5, helper.absolutePos(pos).getY() + .5,
                    helper.absolutePos(pos).getZ() + .5);
            ServerQuestFile file = new ServerQuestFile(helper.getLevel().getServer());
            Chapter chapter = new Chapter(file.newID(), file, file.getDefaultChapterGroup());
            file.getDefaultChapterGroup().addChapter(chapter);
            Quest quest = new Quest(file.newID(), chapter);
            chapter.addQuest(quest);
            ItemTask task = itemTask(file, quest, Items.IRON_INGOT);
            UUID team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId();
            TeamData data = new TeamData(team, file) {
                @Override public Collection<ServerPlayer> getOnlineMembers() { return List.of(owner, teammate); }
            };
            file.addData(data, false);
            file.refreshIDMap();
            MemoryStorage storage = new MemoryStorage();
            storage.items.add(AEItemKey.of(Items.IRON_INGOT), 64);
            IStorageProvider provider = mounts -> mounts.mount(storage);
            var storageService = entity.getMainNode().getGrid().getStorageService();
            storageService.addGlobalStorageProvider(provider);
            Fixture f = new Fixture(file, quest, task, data, entity, owner, teammate, storage);
            try {
                ServerQuestFile.INSTANCE = file;
                helper.assertTrue(entity.applyAction(owner, 0), "Fixture binding failed");
                helper.assertTrue(SubmitterBlockEntity.queueTask(owner,
                        UUID.fromString(entity.getSubmitterIdText()), task.id), "Fixture registration failed");
                test.accept(f);
                helper.succeed();
            } finally {
                ServerQuestFile.INSTANCE = previous;
                storageService.removeGlobalStorageProvider(provider);
            }
        });
    }

    private record Fixture(ServerQuestFile file, Quest quest, ItemTask task, TeamData data,
                           SubmitterBlockEntity entity, ServerPlayer owner, ServerPlayer teammate,
                           MemoryStorage storage) {}

    @FunctionalInterface private interface ReflectAction { void run() throws ReflectiveOperationException; }
    private static void helperReflect(ReflectAction action) {
        try { action.run(); }
        catch (InvocationTargetException e) { throw new AssertionError(e.getCause()); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static final class MemoryStorage implements MEStorage {
        private final KeyCounter items = new KeyCounter();
        private long capacity = Long.MAX_VALUE;
        @Override public Component getDescription() { return Component.empty(); }
        @Override public void getAvailableStacks(KeyCounter target) { target.addAll(items); }
        @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
            long extracted = Math.min(amount, items.get(key));
            if (action == Actionable.MODULATE) items.remove(key, extracted);
            return extracted;
        }
        @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
            long stored = 0;
            for (var entry : items) stored += entry.getLongValue();
            long inserted = Math.min(amount, Math.max(0, capacity - stored));
            if (action == Actionable.MODULATE) items.add(key, inserted);
            return inserted;
        }
    }
}
