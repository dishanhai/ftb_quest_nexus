package com.dishanhai.ftb_quest_nexus.client;

import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.SimpleTextButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.TextField;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.client.util.PositionedIngredient;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Author: dishanhai. One scrollable picker for all of the bound player's pending choices. */
public final class FtbqChoiceRewardScreen extends SelectChoiceRewardScreen {
    private static final String KEY = "gui.ftb_quest_nexus.choice_rewards.";

    private final BiConsumer<UUID, Map<Long, Integer>> submit;
    private final Map<Long, Integer> selections = new LinkedHashMap<>();
    private UUID token;
    private List<ChoiceReward> rewards;
    private boolean waiting;

    private FtbqChoiceRewardScreen(UUID token, List<ChoiceReward> rewards,
                                   BiConsumer<UUID, Map<Long, Integer>> submit) {
        super(rewards.getFirst());
        this.token = token;
        this.rewards = rewards;
        this.submit = submit;
        setTitle(Component.translatable(KEY + "title"));
        setBorder(6, 4, 2);
        showBottomPanel(true);
    }

    public static void open(UUID token, List<Long> rewardIds, BiConsumer<UUID, Map<Long, Integer>> submit) {
        List<ChoiceReward> rewards = resolve(rewardIds);
        if (!rewards.isEmpty()) new FtbqChoiceRewardScreen(token, rewards, submit).openGui();
    }

    public static void update(UUID token, List<Long> rewardIds) {
        FtbqChoiceRewardScreen screen = ClientUtils.getCurrentGuiAs(FtbqChoiceRewardScreen.class);
        if (screen == null || !screen.waiting) return;
        List<ChoiceReward> rewards = resolve(rewardIds);
        if (rewards.isEmpty()) {
            screen.closeGui();
            return;
        }
        screen.token = token;
        screen.rewards = rewards;
        screen.selections.clear();
        screen.waiting = false;
        screen.setTitle(Component.translatable(KEY + "title"));
        screen.scrollBar.setValue(0);
        screen.refreshWidgets();
    }

    private static List<ChoiceReward> resolve(List<Long> rewardIds) {
        ClientQuestFile file = (ClientQuestFile.exists() ? ClientQuestFile.getInstance() : null);
        if (file == null) return List.of();
        return rewardIds.stream().distinct().map(file::getReward)
                .filter(ChoiceReward.class::isInstance).map(ChoiceReward.class::cast)
                .filter(choice -> {
                    var table = choice.getTable();
                    return table != null && !table.getWeightedRewards().isEmpty();
                }).toList();
    }

    @Override
    public void addButtons(Panel panel) {
        for (ChoiceReward choice : rewards) {
            var table = choice.getTable();
            if (table == null || table.getWeightedRewards().isEmpty()) continue;
            Component group = Component.translatable(KEY + "group", choice.getQuest().getTitle(), choice.getTitle())
                    .withStyle(ChatFormatting.BOLD);
            panel.add(new TextField(panel).setMaxWidth(Math.max(1, getWidth() - 20)).setTrim()
                    .showTooltipForLongText().setText(group));
            var entries = table.getWeightedRewards();
            for (int index = 0; index < entries.size(); index++) {
                panel.add(new OptionButton(panel, choice.id, index, entries.get(index).getReward()));
            }
        }
    }

    @Override
    protected void doAccept() {
        if (waiting || selections.isEmpty()) return;
        waiting = true;
        setTitle(Component.translatable(KEY + "waiting"));
        submit.accept(token, Map.copyOf(selections));
    }

    private final class OptionButton extends SimpleTextButton {
        private final long rewardId;
        private final int index;
        private final Reward option;
        private final Component selectedTitle;

        private OptionButton(Panel panel, long rewardId, int index, Reward option) {
            super(panel, option.getTitle(), option.getIcon());
            this.rewardId = rewardId;
            this.index = index;
            this.option = option;
            selectedTitle = Component.translatable(KEY + "selected", option.getTitle()).withStyle(ChatFormatting.GREEN);
        }

        @Override
        public Component getTitle() {
            return isSelected() ? selectedTitle : super.getTitle();
        }

        @Override
        public void onClicked(MouseButton button) {
            if (!button.isLeft() || waiting) return;
            playClickSound();
            if (isSelected()) selections.remove(rewardId);
            else selections.put(rewardId, index);
        }

        @Override
        public void addMouseOverText(TooltipList tooltip) {
            super.addMouseOverText(tooltip);
            option.addMouseOverText(tooltip);
        }

        @Override
        public Optional<PositionedIngredient> getIngredientUnderMouse() {
            return PositionedIngredient.of(option.getIngredient(this), this);
        }

        private boolean isSelected() {
            return selections.getOrDefault(rewardId, -1) == index;
        }
    }
}
