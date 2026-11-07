package com.dishanhai.ftb_quest_nexus.api.ftbq;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.reward.AllTableReward;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.LootReward;
import dev.ftb.mods.ftbquests.quest.reward.RandomReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.XPLevelsReward;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import dev.ftb.mods.ftbquests.quest.task.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Author: dishanhai. Ordered FTBQ definitions shared by all submitters on the server. */
public final class SubmitterQuestIndex {
    private static SubmitterQuestIndex cached;
    private static long nextRevision;

    private final ServerQuestFile file;
    private final long revision;
    private final Map<Long, RewardQuest> rewardQuests = new HashMap<>();
    private List<MatchQuest> matchQuests;
    private Set<Long> allRewardQuestIds;

    private SubmitterQuestIndex(ServerQuestFile file) {
        this.file = file;
        revision = ++nextRevision;
    }

    public static synchronized SubmitterQuestIndex forFile(ServerQuestFile file) {
        if (cached == null || cached.file != file) {
            cached = new SubmitterQuestIndex(file);
        }
        return cached;
    }

    public static synchronized void invalidate(BaseQuestFile file) {
        if (cached != null && cached.file == file) clear();
    }

    public static synchronized void clear() {
        cached = null;
    }

    public static boolean isExperienceReward(Reward reward) {
        return reward != null && (reward.getClass() == XPReward.class || reward.getClass() == XPLevelsReward.class);
    }

    public static boolean supportsQueueTask(Task task) {
        if (ConsumableTask.supported(task)) return true;
        Quest quest = task == null ? null : task.getQuest();
        return quest != null && quest.getTasks().contains(task)
                && quest.getRewards().stream().anyMatch(SubmitterQuestIndex::isExperienceReward);
    }

    public static boolean supportsItemRewardType(Reward reward) {
        if (reward == null) return false;
        Class<?> type = reward.getClass();
        // Table contents are checked against their live definitions when claiming.
        return type == ItemReward.class || type == RandomReward.class || type == LootReward.class
                || type == AllTableReward.class;
    }

    public long revision() { return revision; }

    public List<MatchQuest> matchQuests() {
        if (matchQuests == null) {
            List<MatchQuest> matches = new ArrayList<>();
            forEachLiveQuest(quest -> {
                List<Task> tasks = quest.getTasks().stream().filter(ConsumableTask::supported).toList();
                if (!tasks.isEmpty()) matches.add(new MatchQuest(quest, tasks));
            });
            matchQuests = List.copyOf(matches);
        }
        return matchQuests;
    }

    public Set<Long> rewardQuestIds() {
        if (allRewardQuestIds == null) {
            Set<Long> ids = new LinkedHashSet<>();
            forEachLiveQuest(quest -> {
                RewardQuest rewards = rewardQuest(quest);
                if (!rewards.rewards().isEmpty() || !rewards.choices().isEmpty()) ids.add(quest.id);
            });
            allRewardQuestIds = Collections.unmodifiableSet(ids);
        }
        return allRewardQuestIds;
    }

    public RewardQuest rewardQuest(long id) {
        RewardQuest known = rewardQuests.get(id);
        if (known != null) return known;
        Quest quest = file.getQuest(id);
        if (quest != null) return rewardQuest(quest);
        // A matching pass can see a new chapter entry before FTBQ refreshes its ID map.
        if (matchQuests != null) {
            for (MatchQuest match : matchQuests) {
                if (match.quest().id == id) return rewardQuest(match.quest());
            }
        }
        return null;
    }

    private RewardQuest rewardQuest(Quest quest) {
        return rewardQuests.computeIfAbsent(quest.id, ignored -> new RewardQuest(quest,
                quest.getRewards().stream().filter(reward -> supportsItemRewardType(reward)
                        || isExperienceReward(reward)).toList(),
                quest.getRewards().stream().filter(ChoiceReward.class::isInstance).map(ChoiceReward.class::cast).toList()));
    }

    private void forEachLiveQuest(Consumer<Quest> visitor) {
        // NeoForge's flattened quest list can still reflect the old layout after an editor move.
        file.forAllChapters(chapter -> chapter.getQuests().forEach(visitor));
    }

    public record MatchQuest(Quest quest, List<Task> tasks) {}
    public record RewardQuest(Quest quest, List<Reward> rewards, List<ChoiceReward> choices) {}
}
