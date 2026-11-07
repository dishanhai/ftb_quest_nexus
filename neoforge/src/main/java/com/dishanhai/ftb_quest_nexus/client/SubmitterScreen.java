package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassPalette;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiButton;
import com.dishanhai.sh_better_ui.client.gui.search.SearchField;
import com.dishanhai.sh_better_ui.client.gui.search.SearchIndex;
import com.dishanhai.ftb_quest_nexus.network.SubmitterSearchPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.Util;

/** Native NeoForge presentation of the original submitter panel. Author: dishanhai. */
public final class SubmitterScreen extends AbstractContainerScreen<SubmitterMenu> {
    private static final String GUI = "gui.ftb_quest_nexus.submitter.";
    private static final int LIST_TOP = 134;
    private UiButton previousButton;
    private UiButton nextButton;
    private UiButton matchButton;
    private UiButton claimButton;
    private UiButton clearButton;
    private Component hoveredText;
    private SearchField<Long> search;
    private SearchIndex<Long> searchIndex = new SearchIndex<>(List.of());
    private List<SearchIndex.Entry<Long>> indexedEntries = List.of();
    private int ticksSinceSearchRefresh;
    private long indexedQueueRevision = -1;
    private ClientQuestFile indexedQuestFile;
    private Language indexedLanguage;

    public SubmitterScreen(SubmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 246;
    }

    @Override
    protected void repositionElements() {
        boolean searchFocused = search != null && search.isFocused();
        super.repositionElements();
        if (searchFocused) setFocused(search);
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getValue();
        imageWidth = Math.min(340, width - 12);
        imageHeight = Math.min(246, height - 8);
        super.init();
        GlassBlur.warmUp();
        search = addRenderableWidget(new SearchField<>(font, leftPos + imageWidth / 2,
                topPos + 6, imageWidth / 2 - 10, 14, searchIndex));
        search.setMessage(Component.translatable(GUI + "search"));
        search.setHint(Component.translatable(GUI + "search"));
        search.setValue(query);
        search.setResponder(ignored -> applySearch());
        search.setOnSubmit(ignored -> applySearch());
        refreshSearchIndex();
        String[] keys = {"bind", "clear", "toast", "claim", "previous", "next", "match"};
        int buttonWidth = (imageWidth - 44) / keys.length;
        for (int i = 0; i < keys.length; i++) {
            final int action = i;
            UiButton button = new UiButton(leftPos + 10 + i * (buttonWidth + 4),
                    topPos + imageHeight - 36, buttonWidth, 20, Component.translatable(GUI + keys[i]),
                    ignored -> sendAction(action));
            button.setPreview(Component.translatable(GUI + keys[i] + ".tooltip"));
            addRenderableWidget(button);
            if (i == 1) clearButton = button;
            if (i == 3) claimButton = button;
            if (i == 4) previousButton = button;
            if (i == 5) nextButton = button;
            if (i == 6) matchButton = button;
        }
        updateButtons();
    }

    private void refreshSearchIndex() {
        ClientQuestFile file = ClientQuestFile.INSTANCE;
        Language language = Language.getInstance();
        boolean queueChanged = indexedQueueRevision != menu.getQueueRevision();
        if (indexedQueueRevision == menu.getQueueRevision() && indexedQuestFile == file
                && indexedLanguage == language && ++ticksSinceSearchRefresh < 20) return;
        ticksSinceSearchRefresh = 0;
        List<SearchIndex.Entry<Long>> entries = new ArrayList<>();
        for (Long id : menu.getQueuedTaskIds()) {
            String idText = Long.toUnsignedString(id, 16);
            var task = file == null ? null : file.getTask(id);
            if (task == null) {
                entries.add(new SearchIndex.Entry<>(id,
                        Component.translatable(GUI + "queue_detail.invalid", idText).getString(),
                        List.of(idText, QuestObjectBase.getCodeString(id)), List.of()));
            } else {
                String name = Component.translatable(GUI + "target",
                        task.getQuest().getTitle(), task.getTitle()).getString();
                List<String> ids = new ArrayList<>(List.of(idText, task.getCodeString(),
                        task.getQuest().getCodeString()));
                List<String> aliases = new ArrayList<>();
                if (task instanceof dev.ftb.mods.ftbquests.quest.task.ItemTask item) {
                    var stack = item.getItemStack();
                    ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    aliases.add(stack.getHoverName().getString());
                } else if (task instanceof dev.ftb.mods.ftbquests.quest.task.FluidTask fluid) {
                    ids.add(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
                    aliases.add(Component.translatable(fluid.getFluid().getFluidType().getDescriptionId()).getString());
                } else if (task instanceof ChemicalTask chemical) {
                    ids.add(chemical.chemicalId().toString());
                    aliases.add(chemical.targetName().getString());
                }
                entries.add(new SearchIndex.Entry<>(id, name.isBlank() ? idText : name, ids, aliases));
            }
        }
        indexedQueueRevision = menu.getQueueRevision();
        indexedQuestFile = file;
        indexedLanguage = language;
        if (!queueChanged && entries.equals(indexedEntries)) return;
        indexedEntries = List.copyOf(entries);
        searchIndex = new SearchIndex<>(indexedEntries);
        search.setIndex(searchIndex);
        if (!search.getValue().isBlank() || menu.isFiltering()) applySearch();
    }

    private void applySearch() {
        boolean filtering = !search.getValue().isBlank();
        Set<Long> matches = searchIndex.search(search.getValue()).stream()
                .map(SearchIndex.Entry::value).collect(Collectors.toSet());
        BitSet mask = new BitSet();
        List<Long> queued = menu.getQueuedTaskIds();
        for (int i = 0; i < queued.size(); i++) {
            if (matches.contains(queued.get(i))) mask.set(i);
        }
        SubmitterSearchPacket packet = new SubmitterSearchPacket(menu.containerId,
                menu.getQueueRevision(), filtering, filtering ? mask.toLongArray() : new long[0]);
        PacketDistributor.sendToServer(packet);
    }

    private void sendAction(int action) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }
    }

    private void updateButtons() {
        if (previousButton == null) return;
        previousButton.active = menu.getPage() > 0;
        nextButton.active = menu.getPage() + 1 < menu.getPageCount();
        boolean matching = menu.submitter != null && menu.submitter.isMatching();
        matchButton.setSelected(matching);
        claimButton.active = !matching;
        clearButton.active = !matching;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.submitter == null || menu.submitter.isRemoved()) return;
        GlassTheme theme = GlassTheme.GLASS;
        theme.rootShadow(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.backdrop(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.box(graphics, leftPos, topPos, imageWidth, imageHeight, GlassPalette.ROOT_EDGE, theme.panelBg);
        SubmitterBlockEntity be = menu.submitter;
        int contentWidth = imageWidth - 20;
        row(graphics, mouseX, mouseY, 10, 8, imageWidth / 2 - 16, title, GlassPalette.TEXT_HEAD);
        row(graphics, mouseX, mouseY, 10, 22, contentWidth,
                Component.translatable(GUI + (be.isOnline() ? "online" : "offline")), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 34, contentWidth,
                Component.translatable(GUI + "status", be.getStatusComponent()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 46, contentWidth,
                Component.translatable(GUI + "uuid", identifier(be.getSubmitterIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 58, contentWidth,
                Component.translatable(GUI + "team", identifier(be.getTeamIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 70, contentWidth,
                Component.translatable(GUI + "player", identifier(be.getRewardPlayerIdText())), GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, 10, 84, contentWidth,
                ChemicalTask.isAvailable()
                        ? Component.translatable(GUI + "summary.chemical", be.getQueueSize(), be.getLastSubmitted(),
                                be.getLastSubmittedFluid(), be.getLastSubmittedChemical(), be.getLastClaimedRewards())
                        : Component.translatable(GUI + "summary.resources", be.getQueueSize(), be.getLastSubmitted(),
                                be.getLastSubmittedFluid(), be.getLastClaimedRewards()), GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 98, contentWidth,
                Component.translatable(GUI + (be.isSuppressCompletionToast() ? "toast.suppressed" : "toast.normal")),
                GlassPalette.TEXT);
        row(graphics, mouseX, mouseY, 10, 110, contentWidth,
                Component.translatable(GUI + (be.isMatching() ? "claim.matched"
                        : be.isClaimOnlyQueuedRewards() ? "claim.queued" : "claim.all")),
                GlassPalette.TEXT);
        int amountX = imageWidth * 3 / 5;
        row(graphics, mouseX, mouseY, 10, 122, amountX - 14,
                search.getValue().isBlank() ? Component.translatable(GUI + (be.isMatching() ? "match.goals" : "goals"))
                        : Component.translatable(GUI + "search.results", menu.getMatchedCount(), be.getQueueSize()),
                GlassPalette.TEXT_MUTED);
        row(graphics, mouseX, mouseY, amountX, 122, imageWidth - amountX - 10,
                Component.translatable(GUI + "progress"), GlassPalette.TEXT_MUTED);
        int rowHeight = queueRowHeight();
        theme.box(graphics, leftPos + 8, topPos + LIST_TOP, imageWidth - 16,
                rowHeight * SubmitterMenu.PAGE_SIZE, theme.cellEdge, theme.boxBg);
        if (menu.getRows().isEmpty()) {
            row(graphics, mouseX, mouseY, 14, LIST_TOP + 8, imageWidth - 28,
                    Component.translatable(GUI + (!search.getValue().isBlank() ? "search.empty"
                            : be.isMatching() ? "match.empty" : "empty")),
                    GlassPalette.TEXT_MUTED);
        } else {
            for (int i = 0; i < menu.getRows().size(); i++) {
                drawGoal(graphics, mouseX, mouseY, amountX, LIST_TOP + i * rowHeight,
                        rowHeight, menu.getRows().get(i));
            }
        }
        graphics.drawCenteredString(font, Component.translatable(GUI + "page", menu.getPage() + 1,
                menu.getPageCount()), leftPos + imageWidth / 2, topPos + imageHeight - 12, GlassPalette.TEXT_MUTED);
    }

    private int queueRowHeight() {
        return Math.max(10, (imageHeight - LIST_TOP - 40) / SubmitterMenu.PAGE_SIZE);
    }

    private void drawGoal(GuiGraphics graphics, int mouseX, int mouseY, int amountX,
                          int y, int rowHeight, SubmitterMenu.QueueRow goal) {
        int color = goal.completed() ? 0xFF8CD6B1 : GlassPalette.TEXT;
        var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
        var task = file == null || !goal.valid() ? null : file.getTask(goal.taskId());
        Component target = task == null ? goal.title() : Component.translatable(GUI + "target",
                task.getQuest().getTitle(), task.getTitle());
        String kind = goal.chemical() ? "chemical" : goal.fluid() ? "fluid" : "item";
        Component name = goal.valid() ? Component.translatable(GUI + kind + "_goal", target) : target;
        Component amount = goal.valid()
                ? Component.translatable(GUI + (goal.fluid() || goal.chemical() ? "fluid_amount" : "item_amount"),
                        goal.progress(), goal.required())
                : Component.translatable(GUI + "unavailable");
        int offsetY = Math.max(0, (rowHeight - 12) / 2);
        row(graphics, mouseX, mouseY, 14, y + offsetY, amountX - 20, name, color);
        row(graphics, mouseX, mouseY, amountX, y + offsetY, imageWidth - amountX - 14, amount, color);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + y && mouseY < topPos + y + rowHeight) {
            hoveredText = Component.translatable(GUI + "goal.tooltip", name, amount,
                    Component.translatable(GUI + (goal.completed() ? "complete" : "waiting")));
        }
    }

    private static Component identifier(String text) {
        return text == null || text.isBlank() ? Component.translatable(GUI + "unbound") : Component.literal(text);
    }

    private void row(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width,
                     Component text, int color) {
        int px = leftPos + x;
        int py = topPos + y;
        boolean hovered = mouseX >= px && mouseX < px + width && mouseY >= py && mouseY < py + 12;
        String rendered = text.getString();
        int textWidth = font.width(rendered);
        if (textWidth > width - 4 && hovered) {
            // Smoothly pan the full value while the tooltip provides a stationary copy.
            int overflow = textWidth - (width - 4);
            double seconds = Util.getMillis() / 1000.0;
            double period = Math.max(overflow * 0.04, 3.0);
            double position = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
            graphics.enableScissor(px, py, px + width - 4, py + 12);
            graphics.drawString(font, rendered, px - (int) (position * overflow), py + 2, color, false);
            graphics.disableScissor();
        } else {
            if (textWidth > width - 4) {
                rendered = font.plainSubstrByWidth(rendered, width - 4 - font.width("…")) + "…";
            }
            graphics.drawString(font, rendered, px, py + 2, color, false);
        }
        if (hovered) hoveredText = text;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are part of the original fixed panel; there is no player inventory.
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.submitter == null || menu.submitter.isRemoved()) onClose();
        else {
            refreshSearchIndex();
            updateButtons();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (search.mouseClickedSuggestion(mouseX, mouseY, button, height)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && search.dismissSuggestions()) return true;
        if (search.isFocused()) {
            if (search.keyPressed(key, scanCode, modifiers)) return true;
            // Keep inventory hotkeys available as input while editing the query.
            if (key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_TAB) return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (search.mouseScrolledSuggestions(mouseX, mouseY, vertical, height)) return true;
        if (vertical != 0 && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + LIST_TOP
                && mouseY < topPos + LIST_TOP + queueRowHeight() * SubmitterMenu.PAGE_SIZE) {
            if (vertical > 0 && previousButton.active) sendAction(4);
            if (vertical < 0 && nextButton.active) sendAction(5);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void setTooltipForNextRenderPass(List<FormattedCharSequence> lines, ClientTooltipPositioner positioner,
                                            boolean override) {
        if (search == null || search.suggestionArea(height).isEmpty()) {
            super.setTooltipForNextRenderPass(lines, positioner, override);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredText = null;
        updateButtons();
        var suggestions = search.suggestionArea(height);
        boolean overSuggestions = suggestions.filter(area -> area.contains(mouseX, mouseY)).isPresent();
        super.render(graphics, overSuggestions ? -1 : mouseX, overSuggestions ? -1 : mouseY, partialTick);
        if (suggestions.isPresent()) {
            GaussBlur.deferTopLayer(this, top -> search.renderSuggestions(top, mouseX, mouseY, height));
        } else if (hoveredText != null) {
            setTooltipForNextRenderPass(hoveredText);
        }
    }
}
