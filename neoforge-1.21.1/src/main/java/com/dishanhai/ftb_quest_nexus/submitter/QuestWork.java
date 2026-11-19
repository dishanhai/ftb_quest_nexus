package com.dishanhai.ftb_quest_nexus.submitter;

// Author: dishanhai. Forked from gt_shanhai's FTBQ AE submitter; see SOURCE.md.
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqRewardBuffer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.*;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** FTBQ matching and reward preparation, independent of the block's UI/lifecycle. */
final class QuestWork {
    private QuestWork() {}

    static Submission match(MEStorage storage, TeamData data, IActionSource source, ItemKeys keys,
                            SubmitterQuestIndex index, int start) {
        var quests = index.matchQuests();
        int size = quests.size();
        if (size == 0) return null;
        int position = Math.floorMod(start, size);
        for (int i = 0; i < size; i++, position = (position + 1) % size) {
            var group = quests.get(position);
            if (!data.canStartTasks(group.quest()) || !data.areDependenciesComplete(group.quest())) continue;
            Submission submission = firstSubmission(storage, data, source, keys, group.tasks(), 0);
            if (submission != null) return new Submission(submission.task(), submission.amount(), position + 1);
        }
        return null;
    }

    static Submission firstSubmission(MEStorage storage, TeamData data, IActionSource source, ItemKeys keys,
                                      List<Task> candidates, int start) {
        int size = candidates.size();
        if (size == 0) return null;
        int position = Math.floorMod(start, size);
        for (int i = 0; i < size; i++, position = (position + 1) % size) {
            Task task = candidates.get(position);
            long amount = extract(storage, data, task, source, keys);
            if (amount > 0) return new Submission(task, amount, position + 1);
        }
        return null;
    }

    record Submission(Task task, long amount, int nextIndex) {}

    static long extract(MEStorage storage, TeamData data, Task task, IActionSource source, ItemKeys keys) {
        if (!ConsumableTask.supported(task) || !ConsumableTask.canSubmit(data, task)) return 0;
        long remaining = task.getMaxProgress() - data.getProgress(task);
        if (remaining <= 0) return 0;
        if (task instanceof ConsumableTask.ExactStorageTarget target) {
            return storage.extract(target.storageKey(), remaining, Actionable.MODULATE, source);
        }
        if (task instanceof FluidTask fluid) {
            FluidStack target = new FluidStack(fluid.getFluid(), 1);
            target.applyComponents(fluid.getFluidDataComponentPatch());
            AEFluidKey key = AEFluidKey.of(target);
            return key == null ? 0 : storage.extract(key, remaining, Actionable.MODULATE, source);
        }
        return extractItems(storage, (ItemTask) task, source, remaining, keys);
    }

    private static long extractItems(MEStorage storage, ItemTask task, IActionSource source,
                                     long remaining, ItemKeys keys) {
        ItemStack target = task.getItemStack();
        boolean filter = ItemMatchingSystem.INSTANCE.isItemFilter(target);
        AEItemKey exact = !target.isEmpty() && !filter && task.test(target) ? AEItemKey.of(target) : null;
        long extracted = exact == null ? 0 : storage.extract(exact, remaining, Actionable.MODULATE, source);
        if (extracted >= remaining) return extracted;
        boolean sameItem = !filter && !target.isEmpty() && task.getClass() == ItemTask.class;
        for (AEItemKey key : keys.candidates(storage, sameItem ? target.getItem() : null)) {
            if (key.equals(exact)) continue;
            if (filter && key.matches(target)) continue;
            if (!task.test(key.toStack())) continue;
            extracted += storage.extract(key, remaining - extracted, Actionable.MODULATE, source);
            if (extracted >= remaining) break;
        }
        return extracted;
    }

    /** Snapshot fallback keys once per work pass, only when exact extraction cannot finish. */
    static final class ItemKeys {
        private List<AEItemKey> all;
        private Map<Item, List<AEItemKey>> byItem;

        List<AEItemKey> candidates(MEStorage storage, Item item) {
            if (all == null) {
                all = new ArrayList<>();
                byItem = new HashMap<>();
                for (var entry : storage.getAvailableStacks()) {
                    if (entry.getLongValue() > 0 && entry.getKey() instanceof AEItemKey key) {
                        all.add(key);
                        byItem.computeIfAbsent(key.getItem(), ignored -> new ArrayList<>()).add(key);
                    }
                }
            }
            return item == null ? all : byItem.getOrDefault(item, List.of());
        }
    }

    static long claim(MEStorage storage, TeamData data, UUID playerId, Reward reward,
                      ServerLevel level, BlockEntity blockEntity, FtbqRewardBuffer<AEItemKey> pending,
                      IActionSource source, Runnable changed) {
        if (!canClaimItem(reward)) return 0;
        List<ItemStack> stacks = new ArrayList<>();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (!collectItems(reward, stacks, playerId, player, level, blockEntity)) return 0;
        FtbqRewardBuffer<AEItemKey> prepared = new FtbqRewardBuffer<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) return 0;
            prepared.add(key, stack.getCount());
        }
        // FTBQ 2101 claimReward also delivers items. Mark only to avoid a second copy.
        if (!data.markRewardAsClaimed(playerId, reward, System.currentTimeMillis())) return 0;
        prepared.snapshot().forEach(pending::add);
        changed.run();
        return pending.flush((key, amount) -> storage.insert(key, amount, Actionable.MODULATE, source), changed);
    }

    static void claimExperience(TeamData data, UUID playerId, Reward reward) {
        if (playerId == null || data.getClaimType(playerId, reward) != RewardClaimType.CAN_CLAIM) return;
        for (ServerPlayer player : data.getOnlineMembers()) {
            if (playerId.equals(player.getUUID())) {
                data.claimReward(player, reward, false);
                return;
            }
        }
    }

    static boolean canClaimItem(Reward reward) {
        if (!SubmitterQuestIndex.supportsItemRewardType(reward)) return false;
        return reward.getClass() == ItemReward.class || isItemRewardTree(reward, new HashSet<>());
    }

    private static boolean isItemRewardTree(Reward reward, Set<Reward> visiting) {
        if (!SubmitterQuestIndex.supportsItemRewardType(reward)) return false;
        if (reward.getClass() == ItemReward.class) return true;
        if (!visiting.add(reward)) return false;
        try {
            RandomReward random = (RandomReward) reward;
            var table = random.getTable();
            if (table == null) return false;
            for (WeightedReward entry : table.getWeightedRewards()) {
                if (!isItemRewardTree(entry.getReward(), visiting)) return false;
            }
            return true;
        } finally {
            visiting.remove(reward);
        }
    }

    private static boolean collectItems(Reward reward, List<ItemStack> stacks, UUID playerId,
                                        ServerPlayer player, ServerLevel level, BlockEntity blockEntity) {
        if (reward instanceof RandomReward random) {
            var table = random.getTable();
            var selectedRewards = random instanceof AllTableReward ? table.getWeightedRewards()
                    : table.generateWeightedRandomRewards(level.getRandom(), 1, random instanceof LootReward);
            for (WeightedReward selected : selectedRewards) {
                if (!collectItems(selected.getReward(), stacks, playerId, player, level, blockEntity)) return false;
            }
            // Empty loot is a consumed roll, never permission to roll repeatedly.
            return true;
        }
        return reward.automatedClaimPre(blockEntity, stacks, level.getRandom(), playerId, player);
    }
}
