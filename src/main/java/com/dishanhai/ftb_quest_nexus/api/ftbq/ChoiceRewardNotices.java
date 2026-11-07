package com.dishanhai.ftb_quest_nexus.api.ftbq;

import com.mojang.brigadier.CommandDispatcher;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.RewardClaimType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Author: dishanhai. Private notifications and server-authoritative choice batches. */
public final class ChoiceRewardNotices {
    public static final int MAX_BATCH = 256;
    private static final String COMMAND = "ftbqn_choice_rewards";
    private static final String CHAT_KEY = "chat.ftb_quest_nexus.choice_rewards.";
    private static final Map<ServerPlayer, PlayerNotice> PLAYERS = new WeakHashMap<>();

    private ChoiceRewardNotices() {}

    public static boolean canChoose(TeamData data, UUID playerId, ChoiceReward reward) {
        var table = reward.getTable();
        return !data.isLocked() && !data.isRewardBlocked(reward) && table != null
                && !table.getWeightedRewards().isEmpty() && data.getClaimType(playerId, reward) == RewardClaimType.CAN_CLAIM;
    }

    public static void notifyReady(ServerPlayer player, TeamData data, Map<Long, ChoiceReward> choices,
                                   Function<ServerPlayer, Map<Long, ChoiceReward>> available) {
        if (choices.isEmpty()) return;
        PlayerNotice notice = PLAYERS.computeIfAbsent(player, ignored -> new PlayerNotice());
        if (choices.values().stream().allMatch(reward -> Stamp.of(data, reward).equals(notice.announced.get(reward.id)))) {
            return;
        }
        Map<Long, ChoiceReward> current = notice.announced.isEmpty() ? choices : available.apply(player);
        boolean hasAlert = current.values().stream()
                .anyMatch(reward -> Stamp.of(data, reward).equals(notice.announced.get(reward.id)));
        notice.announced.clear();
        current.forEach((id, reward) -> notice.announced.put(id, Stamp.of(data, reward)));
        if (current.isEmpty() || hasAlert) return;
        player.sendSystemMessage(Component.translatable(CHAT_KEY + "ready").withStyle(style ->
                style.withColor(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/" + COMMAND))));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                Function<ServerPlayer, Map<Long, ChoiceReward>> available,
                                BiConsumer<ServerPlayer, Batch> send) {
        dispatcher.register(Commands.literal(COMMAND).executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Batch batch = open(player, available);
            if (batch.rewardIds().isEmpty()) {
                player.sendSystemMessage(Component.translatable(CHAT_KEY + "none"));
                return 0;
            }
            send.accept(player, batch);
            return 1;
        }));
    }

    private static Batch open(ServerPlayer player, Function<ServerPlayer, Map<Long, ChoiceReward>> available) {
        UUID token = UUID.randomUUID();
        PlayerNotice notice = PLAYERS.computeIfAbsent(player, ignored -> new PlayerNotice());
        Map<Long, ChoiceReward> current = available.apply(player);
        notice.announced.keySet().retainAll(current.keySet());
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || current.isEmpty()) {
            notice.session = null;
            return new Batch(token, List.of());
        }
        TeamData data = file.getOrCreateTeamData(player);
        Map<Long, Offer> offers = new LinkedHashMap<>();
        current.values().stream().limit(MAX_BATCH).forEach(reward -> offers.put(reward.id,
                new Offer(Stamp.of(data, reward), List.copyOf(reward.getTable().getWeightedRewards()))));
        notice.session = new Session(token, SubmitterQuestIndex.forFile(file).revision(), offers);
        return new Batch(token, List.copyOf(offers.keySet()));
    }

    public static Batch select(ServerPlayer player, UUID token, Map<Long, Integer> selections,
                               Function<ServerPlayer, Map<Long, ChoiceReward>> available, ChoiceDelivery deliver) {
        PlayerNotice notice = PLAYERS.get(player);
        Session session = notice == null ? null : notice.session;
        if (session == null || !session.token.equals(token)) return null;
        notice.session = null;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file != null && session.revision == SubmitterQuestIndex.forFile(file).revision()) {
            TeamData data = file.getOrCreateTeamData(player);
            Map<Long, ChoiceReward> current = available.apply(player);
            for (var selection : selections.entrySet()) {
                Offer offer = session.offers.get(selection.getKey());
                ChoiceReward reward = current.get(selection.getKey());
                int index = selection.getValue();
                if (offer == null || reward == null || index < 0 || index >= offer.options.size()) continue;
                if (session.revision != SubmitterQuestIndex.forFile(file).revision()) break;
                if (!offer.stamp.equals(Stamp.of(data, reward)) || !canChoose(data, player.getUUID(), reward)
                        || !offer.options.equals(reward.getTable().getWeightedRewards())) continue;
                deliver.deliver(player, data, reward, index);
            }
        }
        return open(player, available);
    }

    public static void clear() {
        PLAYERS.clear();
    }

    public record Batch(UUID token, List<Long> rewardIds) {}

    @FunctionalInterface
    public interface ChoiceDelivery {
        void deliver(ServerPlayer player, TeamData data, ChoiceReward reward, int index);
    }

    private static final class PlayerNotice {
        private final Map<Long, Stamp> announced = new HashMap<>();
        private Session session;
    }

    private record Stamp(UUID teamId, ChoiceReward reward, int completion, long completedAt) {
        private static Stamp of(TeamData data, ChoiceReward reward) {
            var quest = reward.getQuest();
            return new Stamp(data.getTeamId(), reward, data.getCompletionCount(quest),
                    data.getCompletedTime(quest.id).map(Date::getTime).orElse(0L));
        }
    }

    private record Offer(Stamp stamp, List<WeightedReward> options) {}
    private record Session(UUID token, long revision, Map<Long, Offer> offers) {}
}
