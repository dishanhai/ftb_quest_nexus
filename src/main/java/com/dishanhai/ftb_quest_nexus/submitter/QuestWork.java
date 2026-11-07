package com.dishanhai.ftb_quest_nexus.submitter;

// Author: dishanhai. Forked from gt_shanhai's FTBQ AE submitter; see SOURCE.md.
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqRewardBuffer;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.*;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
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

    static long extract(MEStorage storage, TeamData data, ItemTask task, IActionSource source) {
        if (data.isLocked() || !data.canStartTasks(task.getQuest()) || task.isOnlyFromCrafting()
                || !task.consumesResources()) return 0;
        long remaining = task.getMaxProgress() - data.getProgress(task);
        if (remaining <= 0) return 0;
        ItemStack target = task.getItemStack();
        boolean filter = ItemMatchingSystem.INSTANCE.isItemFilter(target);
        AEItemKey exact = !target.isEmpty() && !filter && task.test(target) ? AEItemKey.of(target) : null;
        long extracted = exact == null ? 0 : storage.extract(exact, remaining, Actionable.MODULATE, source);
        if (extracted >= remaining) return extracted;
        // Inventory mutation can invalidate its iterator, so iterate a key snapshot.
        List<AEItemKey> keys = new ArrayList<>();
        for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEItemKey key) keys.add(key);
        }
        for (AEItemKey key : keys) {
            if (filter && key.matches(target)) continue;
            if (!task.test(key.toStack())) continue;
            extracted += storage.extract(key, remaining - extracted, Actionable.MODULATE, source);
            if (extracted >= remaining) break;
        }
        return extracted;
    }

    static long claim(MEStorage storage, TeamData data, UUID playerId, Reward reward,
                      ServerLevel level, BlockEntity blockEntity, FtbqRewardBuffer<AEItemKey> pending,
                      IActionSource source, Runnable changed) {
        if (!canClaim(reward)) return 0;
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
        // FTBQ 2001.4.22 claimReward also delivers items. Mark only to avoid a second copy.
        if (!data.markRewardAsClaimed(playerId, reward, System.currentTimeMillis())) return 0;
        prepared.snapshot().forEach(pending::add);
        changed.run();
        return pending.flush((key, amount) -> storage.insert(key, amount, Actionable.MODULATE, source), changed);
    }

    static boolean canClaim(Reward reward) {
        return isItemRewardTree(reward, new HashSet<>());
    }

    private static boolean isItemRewardTree(Reward reward, Set<Reward> visiting) {
        if (reward == null || reward instanceof ChoiceReward || !visiting.add(reward)) return false;
        try {
            if (reward.getClass() == ItemReward.class) return true;
            if (reward.getClass() != RandomReward.class && reward.getClass() != LootReward.class) return false;
            RandomReward random = (RandomReward) reward;
            if (random.getTable() == null) return false;
            for (WeightedReward entry : random.getTable().getWeightedRewards()) {
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
            for (WeightedReward selected : random.getTable().generateWeightedRandomRewards(
                    level.getRandom(), 1, random instanceof LootReward)) {
                if (!collectItems(selected.getReward(), stacks, playerId, player, level, blockEntity)) return false;
            }
            // Empty loot is a consumed roll, never permission to roll repeatedly.
            return true;
        }
        return reward.automatedClaimPre(blockEntity, stacks, level.getRandom(), playerId, player);
    }
}
