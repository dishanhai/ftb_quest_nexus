package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import mekanism.api.MekanismAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Author: dishanhai. FTB Library picker for the current Mekanism chemical registry. */
public final class ChemicalPicker extends AbstractButtonListScreen {
    private final Panel previous;
    private final Consumer<ResourceLocation> onSelected;
    private final List<ResourceLocation> chemicals;

    private ChemicalPicker(Panel previous, Consumer<ResourceLocation> onSelected) {
        this.previous = previous;
        this.onSelected = onSelected;
        chemicals = MekanismAPI.CHEMICAL_REGISTRY.keySet().stream()
                .filter(id -> !id.equals(MekanismAPI.EMPTY_CHEMICAL_KEY.location()))
                .sorted(Comparator.comparing((ResourceLocation id) -> ChemicalTask.nameOf(id).getString(),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(ResourceLocation::toString))
                .toList();
        setTitle(Component.translatable(ChemicalTask.LANG + ".picker"));
        setHasSearchBox(true);
        showBottomPanel(false);
        showCloseButton(true);
    }

    public static void register() {
        ChemicalTask.TYPE.setGuiProvider((panel, quest, callback) -> new ChemicalPicker(panel, id -> {
            ChemicalTask task = new ChemicalTask(0, quest);
            task.setChemicalId(id);
            callback.accept(task);
        }).openGui());
        ChemicalReward.TYPE.setGuiProvider((panel, quest, callback) -> new ChemicalPicker(panel, id -> {
            ChemicalReward reward = new ChemicalReward(0, quest);
            reward.setChemical(id);
            callback.accept(reward);
        }).openGui());
    }

    static void addChemicalConfig(ConfigGroup group, ChemicalTask task) {
        addChemicalConfig(group, task.chemicalId(), task::setChemicalId);
    }

    static void addChemicalConfig(ConfigGroup group, ResourceLocation chemical, Consumer<ResourceLocation> setter) {
        group.add("chemical", new ChemicalConfig(), chemical, setter, chemical)
                .setNameKey(ChemicalTask.LANG + ".chemical");
    }

    @Override
    public boolean onInit() {
        return setSizeProportional(0.65F, 0.8F);
    }

    @Override
    public void addButtons(Panel panel) {
        for (ResourceLocation id : chemicals) panel.add(new ChemicalButton(panel, id));
    }

    @Override
    public String getFilterText(Widget widget) {
        return widget instanceof ChemicalButton button
                ? (button.getTitle().getString() + " " + button.id).toLowerCase(Locale.ROOT)
                : super.getFilterText(widget);
    }

    @Override
    protected void doCancel() {
        previous.run();
    }

    @Override
    public void onBack() {
        doCancel();
    }

    @Override
    protected void doAccept() {
        // A row click selects and accepts that chemical in one action.
    }

    private final class ChemicalButton extends SimpleTextButton {
        private final ResourceLocation id;

        private ChemicalButton(Panel panel, ResourceLocation id) {
            super(panel, ChemicalTask.nameOf(id), ChemicalTask.iconOf(id));
            this.id = id;
        }

        @Override
        public void onClicked(MouseButton button) {
            if (!button.isLeft()) return;
            playClickSound();
            previous.run();
            onSelected.accept(id);
        }

        @Override
        public void addMouseOverText(TooltipList tooltip) {
            super.addMouseOverText(tooltip);
            tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class ChemicalConfig extends ConfigValue<ResourceLocation> {
        @Override
        public Component getStringForGUI(ResourceLocation id) {
            return ChemicalTask.nameOf(id);
        }

        @Override
        public Icon getIcon(ResourceLocation id) {
            return ChemicalTask.iconOf(id);
        }

        @Override
        public void onClicked(Widget widget, MouseButton button, ConfigCallback callback) {
            if (getCanEdit()) {
                new ChemicalPicker(widget.getGui(), id -> callback.save(setCurrentValue(id))).openGui();
            }
        }
    }
}
