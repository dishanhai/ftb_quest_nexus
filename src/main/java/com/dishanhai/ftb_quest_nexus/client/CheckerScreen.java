package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqQuestFileRevision;
import com.dishanhai.ftb_quest_nexus.checker.CheckerBlockEntity;
import com.dishanhai.ftb_quest_nexus.checker.CheckerMenu;
import com.dishanhai.ftb_quest_nexus.network.CheckerFluxPackets;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassPalette;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.filter.IngredientFilter;
import com.dishanhai.sh_better_ui.client.gui.scaled.GuiRenderUtil;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiButton;
import com.dishanhai.sh_better_ui.client.gui.search.SearchField;
import com.dishanhai.sh_better_ui.client.gui.search.SearchIndex;
import com.dishanhai.sh_better_ui.client.jei.JeiIntegration.Kind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Author: dishanhai. SH-Better-UI controls display server-confirmed checker state. */
public final class CheckerScreen extends AbstractContainerScreen<CheckerMenu> {
    private static final String GUI = "gui.ftb_quest_nexus.checker.";
    private static final int LIST_TOP = 156;
    private static final int ROW_HEIGHT = 14;
    private static final List<IngredientFilter> TYPE_FILTERS = List.of(
            IngredientFilter.all(), IngredientFilter.items(), IngredientFilter.fluids());
    private static final int ENERGY_FILTER = TYPE_FILTERS.size();
    private UiButton pauseButton;
    private UiButton previousButton;
    private UiButton nextButton;
    private UiButton completionButton;
    private UiButton stockButton;
    private UiButton sortButton;
    private UiButton typeButton;
    private SearchField<Integer> search;
    private SearchIndex<Integer> index = new SearchIndex<>(List.of());
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
    private UiButton fluxButton;

    public CheckerScreen(CheckerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = 286;
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
        imageHeight = Math.min(286, height - 8);
        pageSize = Math.max(1, Math.min(6, (imageHeight - LIST_TOP - 40) / ROW_HEIGHT));
        super.init();
        GlassBlur.warmUp();
        search = addRenderableWidget(new SearchField<>(font, leftPos + 10, topPos + 98,
                imageWidth - 20, 18, index));
        search.setMessage(Component.translatable(GUI + "search"));
        search.setHint(Component.translatable(GUI + "search"));
        search.setValue(query);
        search.setResponder(ignored -> refreshResults(true));
        search.setOnSubmit(ignored -> refreshResults(true));
        int filterWidth = (imageWidth - 32) / 4;
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
            typeFilter = (typeFilter + 1) % (ENERGY_FILTER + 1);
            refreshResults(true);
        });
        int buttonWidth = (imageWidth - 26) / 4;
        int buttonY = topPos + imageHeight - 36;
        addButton(leftPos + 10, buttonY, buttonWidth, "bind", 0);
        pauseButton = addButton(leftPos + 14 + buttonWidth, buttonY, buttonWidth, "pause", 1);
        previousButton = addButton(leftPos + 18 + buttonWidth * 2, buttonY, buttonWidth, "previous", 2);
        nextButton = addButton(leftPos + 22 + buttonWidth * 3, buttonY, buttonWidth, "next", 3);
        if (ModList.get().isLoaded("fluxnetworks")) {
            int fluxWidth = Math.min(106, imageWidth / 3);
            fluxButton = addRenderableWidget(new UiButton(leftPos + imageWidth - fluxWidth - 10,
                    topPos + 4, fluxWidth, 18, Component.translatable(GUI + "flux.open"),
                    ignored -> NexusNetwork.CHANNEL.sendToServer(new CheckerFluxPackets.Request(menu.containerId))));
        }
        refreshCatalog();
        refreshResults(false);
    }

    public static void openFluxSelector(CheckerFluxPackets.View view) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof CheckerScreen checker && client.player != null
                && client.player.containerMenu == checker.menu && checker.menu.containerId == view.containerId()) {
            client.setScreen(new CheckerFluxScreen(checker, view));
        }
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
        if (typeFilter == ENERGY_FILTER) return false;
        var kinds = TYPE_FILTERS.get(typeFilter).kinds();
        Kind kind = row.fluid() ? Kind.FLUID : Kind.ITEM;
        return kinds.isEmpty() || kinds.contains(kind);
    }

    private void refreshCatalog() {
        var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
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
                entries.add(new SearchIndex.Entry<>(i, title.isBlank() ? id : title, List.of(id),
                        List.of(row.title().getString())));
            }
            index = new SearchIndex<>(entries);
            search.setIndex(index);
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
        completionButton.setPreview(Component.translatable(GUI + "filter.completion.tooltip"));
        completionButton.setSelected(completionFilter != 0);
        stockButton.setMessage(Component.translatable(GUI + (stockedOnly ? "filter.stocked" : "filter.any_stock")));
        stockButton.setPreview(Component.translatable(GUI + "filter.stock.tooltip"));
        stockButton.setSelected(stockedOnly);
        sortButton.setMessage(Component.translatable(GUI + (alphabetical ? "sort.az" : "sort.original")));
        sortButton.setPreview(Component.translatable(GUI + "sort.tooltip"));
        sortButton.setSelected(alphabetical);
        String type = switch (typeFilter) {
            case 1 -> "filter.type.item";
            case 2 -> "filter.type.fluid";
            default -> typeFilter == ENERGY_FILTER ? "filter.type.energy" : "filter.type.all";
        };
        typeButton.setMessage(Component.translatable(GUI + type));
        typeButton.setPreview(Component.translatable(GUI + "filter.type.energy.tooltip"));
        typeButton.setSelected(typeFilter != 0);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (menu.checker == null || menu.checker.isRemoved()) return;
        CheckerBlockEntity checker = menu.checker;
        GlassTheme theme = GlassTheme.GLASS;
        theme.rootShadow(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.backdrop(graphics, leftPos, topPos, imageWidth, imageHeight);
        theme.box(graphics, leftPos, topPos, imageWidth, imageHeight, GlassPalette.ROOT_EDGE, theme.panelBg);
        int titleWidth = fluxButton == null ? imageWidth - 20 : fluxButton.getX() - leftPos - 16;
        text(graphics, mouseX, mouseY, 10, 8, titleWidth, title, GlassPalette.TEXT_HEAD);
        text(graphics, mouseX, mouseY, 10, 24, imageWidth - 20,
                Component.translatable(GUI + (checker.isOnline() ? "online" : "offline")), GlassPalette.TEXT);
        text(graphics, mouseX, mouseY, 10, 38, imageWidth - 20,
                Component.translatable(GUI + "status", checker.getStatusComponent()), GlassPalette.TEXT);
        text(graphics, mouseX, mouseY, 10, 52, imageWidth - 20,
                Component.translatable(GUI + "player", identifier(checker.getPlayerIdText())), GlassPalette.TEXT_MUTED);
        text(graphics, mouseX, mouseY, 10, 66, imageWidth - 20,
                Component.translatable(GUI + "team", identifier(checker.getTeamIdText())), GlassPalette.TEXT_MUTED);
        text(graphics, mouseX, mouseY, 10, 82, imageWidth - 20,
                Component.translatable(GUI + "summary", checker.getItemTaskCount(),
                        checker.getFluidTaskCount(), checker.getCompletedLastScan()), GlassPalette.TEXT);
        int amountX = imageWidth * 3 / 5;
        text(graphics, mouseX, mouseY, 10, 142, amountX - 14,
                Component.translatable(GUI + "goals"), GlassPalette.TEXT_MUTED);
        text(graphics, mouseX, mouseY, amountX, 142, imageWidth - amountX - 10,
                Component.translatable(GUI + "stock"), GlassPalette.TEXT_MUTED);
        theme.box(graphics, leftPos + 8, topPos + LIST_TOP, imageWidth - 16, ROW_HEIGHT * pageSize,
                theme.cellEdge, theme.boxBg);
        if (visibleRows.isEmpty()) {
            int emptyY = LIST_TOP + Math.min(8, ROW_HEIGHT * pageSize - 12);
            text(graphics, mouseX, mouseY, 14, emptyY, imageWidth - 28,
                    Component.translatable(GUI + (menu.getCatalogRows().isEmpty() ? "empty" : "no_matches")),
                    GlassPalette.TEXT_MUTED);
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
        graphics.drawCenteredString(font, pageLabel, leftPos + imageWidth / 2,
                topPos + imageHeight - 12, GlassPalette.TEXT_MUTED);
    }

    private void drawGoal(GuiGraphics graphics, int mouseX, int mouseY, int amountX,
                          int y, int rowHeight, CheckerBlockEntity.CheckRow row) {
        int color = row.completed() ? 0xFF8CD6B1 : GlassPalette.TEXT;
        Component title = goalTitle(row);
        String kind = row.energy() ? "energy" : row.fluid() ? "fluid" : "item";
        Component name = Component.translatable(GUI + kind + "_goal", title);
        Component amount = Component.translatable(GUI + kind + "_amount",
                row.available(), row.required());
        int offsetY = Math.max(0, (rowHeight - 12) / 2);
        text(graphics, mouseX, mouseY, 14, y + offsetY, amountX - 20, name, color);
        text(graphics, mouseX, mouseY, amountX, y + offsetY, imageWidth - amountX - 14, amount, color);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + y && mouseY < topPos + y + rowHeight) {
            hoveredText = Component.translatable(GUI + "goal.tooltip", name, amount,
                    Component.translatable(GUI + (row.completed() ? "complete" : row.energy() ? "waiting_energy" : "waiting")));
        }
    }

    private static Component goalTitle(CheckerBlockEntity.CheckRow row) {
        var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
        var task = file == null ? null : file.getTask(row.taskId());
        return task == null ? row.title() : Component.translatable(GUI + "target",
                task.getQuest().getTitle(), task.getTitle());
    }

    private static Component identifier(String value) {
        return value == null || value.isBlank()
                ? Component.translatable(GUI + "unbound") : Component.literal(value);
    }

    private void text(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width,
                      Component value, int color) {
        graphics.drawString(font, GuiRenderUtil.trimText(font, value.getString(), width),
                leftPos + x, topPos + y + 2, color, false);
        if (mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + 12) {
            hoveredText = value;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The checker has no inventory slots. Labels share the panel's screen coordinates.
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        search.tick();
        if (menu.checker == null || menu.checker.isRemoved()) onClose();
        else {
            if (menu.takeResyncRequest()) sendAction(CheckerMenu.RESYNC_BUTTON);
            refreshCatalog();
            updateButtons();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (search.mouseClickedSuggestion(mouseX, mouseY, button, height)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && search.dismissSuggestions()) return true;
        if (search.isFocused()) {
            if (search.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (search.mouseScrolledSuggestions(mouseX, mouseY, delta, height)) return true;
        if (delta != 0 && mouseX >= leftPos + 8 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + LIST_TOP
                && mouseY < topPos + LIST_TOP + ROW_HEIGHT * pageSize) {
            if (delta > 0 && previousButton.active) sendAction(2);
            if (delta < 0 && nextButton.active) sendAction(3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
        refreshCatalog();
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
