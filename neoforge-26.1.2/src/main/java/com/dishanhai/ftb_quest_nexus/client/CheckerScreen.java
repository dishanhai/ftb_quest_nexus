package com.dishanhai.ftb_quest_nexus.client;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.dishanhai.ftb_quest_nexus.mixin.GuiGraphicsTooltipAccessor;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;

import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqQuestFileRevision;
import com.dishanhai.ftb_quest_nexus.checker.CheckerBlockEntity;
import com.dishanhai.ftb_quest_nexus.checker.CheckerMenu;
import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.display.GraphicIcon;
import com.dishanhai.sh_better_ui.client.gui.filter.IngredientFilter;
import com.dishanhai.sh_better_ui.client.gui.scaled.GuiRenderUtil;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiButton;
import com.dishanhai.sh_better_ui.client.gui.search.SearchField;
import com.dishanhai.sh_better_ui.client.gui.search.SearchIndex;
import com.dishanhai.sh_better_ui.client.jei.JeiIntegration.Kind;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Author: dishanhai. SH-Better-UI controls display server-confirmed checker state. */
public final class CheckerScreen extends AbstractContainerScreen<CheckerMenu> {
    private final BlockScreenTheme appearance = new BlockScreenTheme();
    private static final String GUI = "gui.ftb_quest_nexus.checker.";
    private static final int LIST_TOP = 156;
    private static final int ROW_HEIGHT = 14;
    private static final SearchIndex<Integer> EMPTY_SEARCH_INDEX = new SearchIndex<>(List.of());
    private static final List<IngredientFilter> TYPE_FILTERS = List.of(
            IngredientFilter.all(), IngredientFilter.items(), IngredientFilter.fluids());
    private static final int ENERGY_FILTER = TYPE_FILTERS.size();
    private static final int CHEMICAL_FILTER = ENERGY_FILTER + 1;
    private UiButton pauseButton;
    private UiButton previousButton;
    private UiButton nextButton;
    private UiButton completionButton;
    private GraphicIcon completionIcon;
    private UiButton stockButton;
    private UiButton sortButton;
    private UiButton typeButton;
    private SearchField<Integer> search;
    private SearchIndex<Integer> index = EMPTY_SEARCH_INDEX;
    private boolean searchSuggestions;
    private List<CheckerBlockEntity.CheckRow> indexedRows = List.of();
    private List<Integer> visibleRows = List.of();
    private int completionFilter;
    private int typeFilter;
    private boolean stockedOnly;
    private boolean alphabetical;
    private int page;
    private int pageSize = 6;
    private long catalogRevision = -1;
    private long stockRevision = -1;
    private Object indexedQuestFile;
    private long indexedQuestRevision;
    private net.minecraft.locale.Language indexedLanguage;
    private Component hoveredText;
    private int panelWidth;
    private int panelHeight;

    public CheckerScreen(CheckerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 340, 286);
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
        panelWidth = Math.min(340, width - 12);
        panelHeight = Math.min(286, height - 8);
        pageSize = Math.max(1, Math.min(6, (panelHeight - LIST_TOP - 40) / ROW_HEIGHT));
        super.init();
        leftPos = (width - panelWidth) / 2;
        topPos = (height - panelHeight) / 2;
        GlassBlur.warmUp();
        completionIcon = NexusIcons.available(NexusIcons.TASK, GraphicIcon.LIST);
        searchSuggestions = ClientUiConfig.searchSuggestions();
        search = addRenderableWidget(new SearchField<>(font, leftPos + 10, topPos + 98,
                panelWidth - 20, 18, searchSuggestions ? index : EMPTY_SEARCH_INDEX));
        if (!searchSuggestions) search.setTooltip(null);
        search.setMessage(Component.translatable(GUI + "search"));
        search.setHint(Component.translatable(GUI + "search"));
        search.setValue(query);
        search.setResponder(ignored -> refreshResults(true));
        search.setOnSubmit(ignored -> refreshResults(true));
        int filterWidth = (panelWidth - 32) / 4;
        completionButton = addFilterButton(leftPos + 10, filterWidth, () -> {
            completionFilter = (completionFilter + 1) % 3;
            refreshResults(true);
        });
        stockButton = addFilterButton(leftPos + 14 + filterWidth, filterWidth, () -> {
            stockedOnly = !stockedOnly;
            refreshResults(true);
        });
        sortButton = addFilterButton(leftPos + 18 + filterWidth * 2, filterWidth, () -> {
            alphabetical = !alphabetical;
            refreshResults(true);
        });
        typeButton = addFilterButton(leftPos + 22 + filterWidth * 3, filterWidth, () -> {
            int lastFilter = ChemicalTask.isAvailable() ? CHEMICAL_FILTER : ENERGY_FILTER;
            typeFilter = (typeFilter + 1) % (lastFilter + 1);
            refreshResults(true);
        });
        int buttonWidth = (panelWidth - 26) / 4;
        int buttonY = topPos + panelHeight - 36;
        UiButton bindButton = addButton(leftPos + 10, buttonY, buttonWidth, "bind", 0);
        setLeadingIconIfLabelFits(bindButton, GraphicIcon.USER);
        pauseButton = addButton(leftPos + 14 + buttonWidth, buttonY, buttonWidth, "pause", 1);
        previousButton = addButton(leftPos + 18 + buttonWidth * 2, buttonY, buttonWidth, "previous", 2);
        previousButton.setIcon(GraphicIcon.PREVIOUS);
        nextButton = addButton(leftPos + 22 + buttonWidth * 3, buttonY, buttonWidth, "next", 3);
        nextButton.setIcon(GraphicIcon.NEXT);
        refreshCatalog();
        refreshResults(false);
    }

    private UiButton addFilterButton(int x, int width, Runnable action) {
        return addRenderableWidget(new UiButton(x, topPos + 120, width, 18, Component.empty(),
                ignored -> action.run()));
    }

    private boolean matchesFilters(int index) {
        var row = indexedRows.get(index);
        if ((completionFilter != 0 && row.completed() != (completionFilter == 1))
                || (stockedOnly && row.available() <= 0)) return false;
        if (row.energy()) return typeFilter == 0 || typeFilter == ENERGY_FILTER;
        if (row.chemical()) return typeFilter == 0 || typeFilter == CHEMICAL_FILTER;
        if (typeFilter >= ENERGY_FILTER) return false;
        var kinds = TYPE_FILTERS.get(typeFilter).kinds();
        Kind kind = row.fluid() ? Kind.FLUID : Kind.ITEM;
        return kinds.isEmpty() || kinds.contains(kind);
    }

    private void refreshCatalog() {
        var file = (ClientQuestFile.exists() ? ClientQuestFile.getInstance() : null);
        var language = net.minecraft.locale.Language.getInstance();
        long questRevision = FtbqQuestFileRevision.get(file);
        boolean catalogChanged = catalogRevision != menu.getCatalogRevision() || indexedQuestFile != file
                || indexedQuestRevision != questRevision || indexedLanguage != language;
        boolean stockChanged = stockRevision != menu.getStockRevision();
        stockRevision = menu.getStockRevision();
        if (catalogChanged) {
            catalogRevision = menu.getCatalogRevision();
            indexedQuestFile = file;
            indexedQuestRevision = questRevision;
            indexedLanguage = language;
            indexedRows = menu.getCatalogRows();
            List<SearchIndex.Entry<Integer>> entries = new ArrayList<>(indexedRows.size());
            for (int i = 0; i < indexedRows.size(); i++) {
                var row = indexedRows.get(i);
                String id = Long.toHexString(row.taskId());
                String title = goalTitle(row).getString();
                var task = file == null ? null : file.getTask(row.taskId());
                List<String> ids = task instanceof ChemicalTask chemical
                        ? List.of(id, chemical.chemicalId().toString()) : List.of(id);
                entries.add(new SearchIndex.Entry<>(i, title.isBlank() ? id : title, ids,
                        List.of(row.title().getString())));
            }
            index = new SearchIndex<>(entries);
            if (searchSuggestions) search.setIndex(index);
        }
        if (catalogChanged || (stockChanged && (completionFilter != 0 || stockedOnly))) refreshResults(false);
    }

    private void refreshResults(boolean resetPage) {
        search.setScope(this::matchesFilters);
        var entries = index.search(search.getValue(), this::matchesFilters).stream();
        if (alphabetical) entries = entries.sorted(Comparator.comparing(
                SearchIndex.Entry<Integer>::name, String.CASE_INSENSITIVE_ORDER));
        visibleRows = entries.map(SearchIndex.Entry::value).toList();
        page = resetPage ? 0 : Math.min(page, pageCount() - 1);
        updateButtons();
    }

    private int pageCount() {
        return Math.max(1, (visibleRows.size() + pageSize - 1) / pageSize);
    }

    private UiButton addButton(int x, int y, int width, String key, int action) {
        UiButton button = new UiButton(x, y, width, 20, Component.translatable(GUI + key),
                ignored -> sendAction(action));
        button.setPreview(Component.translatable(GUI + key + ".tooltip"));
        return addRenderableWidget(button);
    }

    private void setLeadingIconIfLabelFits(UiButton button, GraphicIcon icon) {
        // SHBUI reserves 6 units on each edge and 4 between the icon and the label.
        int labelWidth = button.getWidth() - GraphicIcon.PIXELS - 16;
        button.setLeadingIcon(font.width(button.getMessage()) <= labelWidth ? icon : null);
    }

    private void sendAction(int action) {
        if (action == 2 || action == 3) {
            page = Math.max(0, Math.min(page + (action == 2 ? -1 : 1), pageCount() - 1));
            updateButtons();
            return;
        }
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }
    }

    private void updateButtons() {
        if (pauseButton == null || menu.checker == null) return;
        boolean paused = menu.checker.isPaused();
        pauseButton.setMessage(Component.translatable(GUI + (paused ? "resume" : "pause")));
        setLeadingIconIfLabelFits(pauseButton, paused ? GraphicIcon.RESUME : GraphicIcon.PAUSE);
        pauseButton.setPreview(Component.translatable(GUI + (paused ? "resume" : "pause") + ".tooltip"));
        pauseButton.setSelected(paused);
        previousButton.active = page > 0;
        nextButton.active = page + 1 < pageCount();
        String completion = switch (completionFilter) {
            case 1 -> "filter.completed";
            case 2 -> "filter.incomplete";
            default -> "filter.all";
        };
        completionButton.setMessage(Component.translatable(GUI + completion));
        setLeadingIconIfLabelFits(completionButton, completionIcon);
        completionButton.setPreview(Component.translatable(GUI + "filter.completion.tooltip"));
        completionButton.setSelected(completionFilter != 0);
        stockButton.setMessage(Component.translatable(GUI + (stockedOnly ? "filter.stock_or_progress" : "filter.any_stock")));
        setLeadingIconIfLabelFits(stockButton, GraphicIcon.CACHE);
        stockButton.setPreview(Component.translatable(GUI + "filter.stock_or_progress.tooltip"));
        stockButton.setSelected(stockedOnly);
        sortButton.setMessage(Component.translatable(GUI + (alphabetical ? "sort.az" : "sort.original")));
        setLeadingIconIfLabelFits(sortButton, GraphicIcon.SORT);
        sortButton.setPreview(Component.translatable(GUI + "sort.tooltip"));
        sortButton.setSelected(alphabetical);
        String type = switch (typeFilter) {
            case 1 -> "filter.type.item";
            case 2 -> "filter.type.fluid";
            default -> typeFilter == CHEMICAL_FILTER ? "filter.type.chemical"
                    : typeFilter == ENERGY_FILTER ? "filter.type.energy" : "filter.type.all";
        };
        typeButton.setMessage(Component.translatable(GUI + type));
        setLeadingIconIfLabelFits(typeButton, GraphicIcon.FILTER);
        typeButton.setPreview(Component.translatable(GUI + (ChemicalTask.isAvailable()
                ? "filter.type.chemical.tooltip" : "filter.type.energy.tooltip")));
        typeButton.setSelected(typeFilter != 0);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    private void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.checker == null || menu.checker.isRemoved()) return;
        CheckerBlockEntity checker = menu.checker;
        GlassTheme theme = appearance.current().panel();
        theme.rootShadow(graphics, leftPos, topPos, panelWidth, panelHeight);
        theme.backdrop(graphics, leftPos, topPos, panelWidth, panelHeight);
        theme.box(graphics, leftPos, topPos, panelWidth, panelHeight, theme.colors.ROOT_EDGE, theme.panelBg);
        text(graphics, mouseX, mouseY, 10, 8, panelWidth - 20, title, theme.colors.TEXT_HEAD);
        text(graphics, mouseX, mouseY, 10, 24, panelWidth - 20,
                Component.translatable(GUI + (checker.isOnline() ? "online_with_energy" : "offline")), theme.colors.TEXT);
        text(graphics, mouseX, mouseY, 10, 38, panelWidth - 20,
                Component.translatable(GUI + "status", checker.getStatusComponent()), theme.colors.TEXT);
        text(graphics, mouseX, mouseY, 10, 52, panelWidth - 20,
                Component.translatable(GUI + "player", identifier(checker.getPlayerIdText())), theme.colors.TEXT_MUTED);
        text(graphics, mouseX, mouseY, 10, 66, panelWidth - 20,
                Component.translatable(GUI + "team", identifier(checker.getTeamIdText())), theme.colors.TEXT_MUTED);
        Component summary = ChemicalTask.isAvailable()
                ? Component.translatable(GUI + "summary_with_chemical", checker.getItemTaskCount(),
                        checker.getFluidTaskCount(), checker.getEnergyTaskCount(), checker.getChemicalTaskCount(),
                        checker.getCompletedLastScan())
                : Component.translatable(GUI + "summary_with_energy", checker.getItemTaskCount(),
                        checker.getFluidTaskCount(), checker.getEnergyTaskCount(), checker.getCompletedLastScan());
        text(graphics, mouseX, mouseY, 10, 82, panelWidth - 20, summary, theme.colors.TEXT);
        int amountX = panelWidth * 3 / 5;
        text(graphics, mouseX, mouseY, 10, 142, amountX - 14,
                Component.translatable(GUI + "goals"), theme.colors.TEXT_MUTED);
        text(graphics, mouseX, mouseY, amountX, 142, panelWidth - amountX - 10,
                Component.translatable(GUI + "stock_or_progress"), theme.colors.TEXT_MUTED);
        theme.box(graphics, leftPos + 8, topPos + LIST_TOP, panelWidth - 16, ROW_HEIGHT * pageSize,
                theme.cellEdge, theme.boxBg);
        if (visibleRows.isEmpty()) {
            int emptyY = LIST_TOP + Math.min(8, ROW_HEIGHT * pageSize - 12);
            text(graphics, mouseX, mouseY, 14, emptyY, panelWidth - 28,
                    Component.translatable(GUI + (menu.getCatalogRows().isEmpty() ? "empty" : "no_matches")),
                    theme.colors.TEXT_MUTED);
        } else {
            int rowIndex = 0;
            int end = Math.min(visibleRows.size(), (page + 1) * pageSize);
            for (int index : visibleRows.subList(page * pageSize, end)) {
                int y = LIST_TOP + rowIndex * ROW_HEIGHT;
                drawGoal(graphics, mouseX, mouseY, amountX, y, ROW_HEIGHT, indexedRows.get(index));
                rowIndex++;
            }
        }
        Component pageLabel = Component.translatable(GUI + "page_matches", page + 1, pageCount(), visibleRows.size());
        graphics.centeredText(font, pageLabel, leftPos + panelWidth / 2,
                topPos + panelHeight - 12, theme.colors.TEXT_MUTED);
    }

    private void drawGoal(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int amountX,
                          int y, int rowHeight, CheckerBlockEntity.CheckRow row) {
        var frame = appearance.current();
        int color = row.completed() ? frame.completeText() : frame.panel().colors.TEXT;
        Component title = goalTitle(row);
        String kind = row.chemical() ? "chemical" : row.energy() ? "energy" : row.fluid() ? "fluid" : "item";
        Component name = Component.translatable(GUI + kind + "_goal", title);
        Component amount = Component.translatable(GUI + (row.chemical() ? "fluid" : kind) + "_amount",
                row.available(), row.required());
        int offsetY = Math.max(0, (rowHeight - 12) / 2);
        text(graphics, mouseX, mouseY, 14, y + offsetY, amountX - 20, name, color);
        text(graphics, mouseX, mouseY, amountX, y + offsetY, panelWidth - amountX - 14, amount, color);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + panelWidth - 8
                && mouseY >= topPos + y && mouseY < topPos + y + rowHeight) {
            hoveredText = Component.translatable(GUI + "goal.tooltip", name, amount,
                    Component.translatable(GUI + (row.completed() ? "complete" : row.energy() ? "waiting_energy" : "waiting")));
        }
    }

    private static Component goalTitle(CheckerBlockEntity.CheckRow row) {
        var file = (ClientQuestFile.exists() ? ClientQuestFile.getInstance() : null);
        var task = file == null ? null : file.getTask(row.taskId());
        return task == null ? row.title() : Component.translatable(GUI + "target",
                task.getQuest().getTitle(), task.getTitle());
    }

    private static Component identifier(String value) {
        return value == null || value.isBlank()
                ? Component.translatable(GUI + "unbound") : Component.literal(value);
    }

    private void text(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width,
                      Component value, int color) {
        graphics.text(font, GuiRenderUtil.trimText(font, value.getString(), width),
                leftPos + x, topPos + y + 2, color, false);
        if (mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + 12) {
            hoveredText = value;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The checker has no inventory slots. Labels share the panel's screen coordinates.
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // SearchField uses the platform's native EditBox cursor timing.
        if (menu.checker == null || menu.checker.isRemoved()) onClose();
        else {
            if (menu.takeResyncRequest()) sendAction(CheckerMenu.RESYNC_BUTTON);
            refreshCatalog();
            updateButtons();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (search.mouseClickedSuggestion(event.x(), event.y(), event.button(), height)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && search.dismissSuggestions()) return true;
        if (search.isFocused()) {
            if (search.keyPressed(event)) return true;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB) return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (search.mouseScrolledSuggestions(mouseX, mouseY, vertical, height)) return true;
        if (vertical != 0 && mouseX >= leftPos + 8 && mouseX < leftPos + panelWidth - 8
                && mouseY >= topPos + LIST_TOP
                && mouseY < topPos + LIST_TOP + ROW_HEIGHT * pageSize) {
            if (vertical > 0 && previousButton.active) sendAction(2);
            if (vertical < 0 && nextButton.active) sendAction(3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return mouseX < left || mouseY < top || mouseX >= left + panelWidth || mouseY >= top + panelHeight;
    }

    @Override public int getXSize() { return panelWidth; }
    @Override public int getYSize() { return panelHeight; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredText = null;
        refreshCatalog();
        updateButtons();
        var suggestions = search.suggestionArea(height);
        boolean overSuggestions = suggestions.filter(area -> area.contains(mouseX, mouseY)).isPresent();
        var frame = appearance.resolve(ClientUiConfig.checkerTheme());
        int panelMouseX = overSuggestions ? -1 : mouseX;
        int panelMouseY = overSuggestions ? -1 : mouseY;
        frame.paint(() -> {
            extractPanel(graphics, panelMouseX, panelMouseY);
            super.extractRenderState(graphics, panelMouseX, panelMouseY, partialTick);
        });
        if (suggestions.isPresent()) {
            ((GuiGraphicsTooltipAccessor) graphics).nexus$setDeferredTooltip(null);
            GaussBlur.deferTopLayer(this, top -> frame.paint(() -> search.renderSuggestions(top, mouseX, mouseY, height)));
        } else if (hoveredText != null) {
            graphics.setTooltipForNextFrame(hoveredText, mouseX, mouseY);
        }
    }
}
