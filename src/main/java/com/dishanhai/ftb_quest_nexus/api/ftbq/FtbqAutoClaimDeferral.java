package com.dishanhai.ftb_quest_nexus.api.ftbq;

// Author: dishanhai. Reserve supported rewards for the submitting machine during progress events.
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class FtbqAutoClaimDeferral {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private FtbqAutoClaimDeferral() {}

    public static List<Quest> run(TeamData data, Predicate<Reward> rewards, Runnable action) {
        Context previous = CURRENT.get();
        Context context = new Context(data, rewards, new LinkedHashSet<>());
        CURRENT.set(context);
        try {
            action.run();
            return List.copyOf(context.quests);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static boolean shouldDefer(TeamData data, Reward reward) {
        Context context = CURRENT.get();
        if (context == null || context.data != data || !context.rewards.test(reward)) return false;
        // Per-player auto-claims can reset a repeatable quest too. Replay them only after AE claims.
        context.quests.add(reward.getQuest());
        return true;
    }

    private record Context(TeamData data, Predicate<Reward> rewards, Set<Quest> quests) {}
}
