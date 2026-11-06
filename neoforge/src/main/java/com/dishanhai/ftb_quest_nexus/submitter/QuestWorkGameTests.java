package com.dishanhai.ftb_quest_nexus.submitter;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqRewardBuffer;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Author: dishanhai. Executes the real FTBQ/AE task and reward APIs. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QuestWorkGameTests {
    private QuestWorkGameTests() {}

    @GameTest(template = "empty")
    public static void extractionConsumesOnlyRemainingRequirement(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        MemoryStorage storage = new MemoryStorage(100);
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        storage.items.add(iron, 64);
        long extracted = QuestWork.extract(storage, fixture.data, fixture.task, IActionSource.empty());
        helper.assertTrue(extracted == 10, "Extraction exceeded the task requirement");
        helper.assertTrue(storage.items.get(iron) == 54, "AE inventory was not reduced exactly once");
        helper.assertTrue(fixture.data.getProgress(fixture.task) == 0, "Extraction must not also credit task progress");
        fixture.data.addProgress(fixture.task, extracted);
        helper.assertTrue(fixture.data.getProgress(fixture.task) == 10, "Real FTBQ progress API did not accept extraction");
        helper.assertTrue(QuestWork.extract(storage, fixture.data, fixture.task, IActionSource.empty()) == 0,
                "Completed target was consumed again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedAndNonConsumingTasksNeverExtract(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        MemoryStorage storage = new MemoryStorage(100);
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        storage.items.add(iron, 64);
        fixture.task.setConsumeItems(Tristate.FALSE);
        helper.assertTrue(QuestWork.extract(storage, fixture.data, fixture.task, IActionSource.empty()) == 0,
                "Non-consuming task extracted resources");
        fixture.task.setConsumeItems(Tristate.TRUE);
        fixture.data.setLocked(true);
        helper.assertTrue(QuestWork.extract(storage, fixture.data, fixture.task, IActionSource.empty()) == 0,
                "Locked team extracted resources");
        helper.assertTrue(storage.items.get(iron) == 64, "Refused tasks changed AE inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullAeRetainsRewardAndClaimCannotDuplicate(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        ItemReward reward = new ItemReward(fixture.file.newID(), fixture.quest, new ItemStack(Items.GOLD_INGOT), 8);
        fixture.quest.addReward(reward);
        UUID player = UUID.randomUUID();
        MemoryStorage storage = new MemoryStorage(0);
        FtbqRewardBuffer<AEItemKey> pending = new FtbqRewardBuffer<>();
        SubmitterBlockEntity entity = NexusContent.BLOCK_ENTITY.get().create(BlockPos.ZERO,
                NexusContent.SUBMITTER.get().defaultBlockState());
        int[] dirty = {0};
        long inserted = QuestWork.claim(storage, fixture.data, player, reward, helper.getLevel(), entity,
                pending, IActionSource.empty(), () -> dirty[0]++);
        AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
        helper.assertTrue(inserted == 0 && pending.snapshot().getOrDefault(gold, 0L) == 8,
                "Full storage lost the generated reward");
        helper.assertTrue(fixture.data.isRewardClaimed(player, reward), "FTBQ did not record the reward claim");
        helper.assertTrue(dirty[0] > 0, "Prepared reward was not marked dirty");
        QuestWork.claim(storage, fixture.data, player, reward, helper.getLevel(), entity,
                pending, IActionSource.empty(), () -> dirty[0]++);
        helper.assertTrue(pending.snapshot().get(gold) == 8, "Repeated claim duplicated the reward");
        storage.capacity = 3;
        helper.assertTrue(pending.flush((key, amount) -> storage.insert(key, amount, Actionable.MODULATE,
                IActionSource.empty()), () -> {}) == 3, "Partial capacity was not used");
        helper.assertTrue(pending.snapshot().get(gold) == 5 && storage.items.get(gold) == 3,
                "Partial insertion did not preserve the exact remainder");
        storage.capacity = 8;
        pending.flush((key, amount) -> storage.insert(key, amount, Actionable.MODULATE, IActionSource.empty()), () -> {});
        helper.assertTrue(pending.isEmpty() && storage.items.get(gold) == 8, "Retry lost or duplicated reward items");
        helper.succeed();
    }

    private static Fixture fixture(GameTestHelper helper) {
        // A local quest file keeps test objects out of the world's actual quest catalog.
        ServerQuestFile file = new ServerQuestFile(helper.getLevel().getServer());
        Chapter chapter = new Chapter(file.newID(), file, file.getDefaultChapterGroup());
        Quest quest = new Quest(file.newID(), chapter);
        ItemTask task = new ItemTask(file.newID(), quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10);
        task.setConsumeItems(Tristate.TRUE);
        quest.addTask(task);
        return new Fixture(file, quest, task, new TeamData(UUID.randomUUID(), file));
    }

    private record Fixture(ServerQuestFile file, Quest quest, ItemTask task, TeamData data) {}

    private static final class MemoryStorage implements MEStorage {
        private final KeyCounter items = new KeyCounter();
        private long capacity;
        MemoryStorage(long capacity) { this.capacity = capacity; }
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
