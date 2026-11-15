package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.sh_better_ui.client.gui.scaled.RoundedCorners;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiPreferences;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiTheme;
import com.dishanhai.sh_better_ui.client.gui.search.SearchIndex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Author: dishanhai. Hosts SHBUI search completions beside the native text editor. */
final class MachineSearchField<T> extends EditBox {
    private static final int LIMIT = 8;
    private static final int MAX_LENGTH = 128;
    private static final int GAP = 2;
    private static final int INSET = 5;
    private final Font font;
    private final int fieldHeight;
    private final int textInsetY;
    private SearchIndex<T> index;
    private Predicate<? super T> scope = value -> true;
    private Consumer<String> onChange = value -> {};
    private Consumer<String> onSubmit = value -> {};
    private List<SearchIndex.Suggestion<T>> suggestions;
    private String undoQuery;
    private Component hint = Component.empty();
    private int hintColor;
    private boolean dismissed;
    private int selection;

    MachineSearchField(Font font, int x, int y, int width, int height, SearchIndex<T> index) {
        super(font, x + INSET, y + textInsetY(font, height), Math.max(1, width - INSET * 2),
                Math.min(height, font.lineHeight), Component.empty());
        this.font = font;
        this.fieldHeight = height;
        this.textInsetY = textInsetY(font, height);
        this.index = index;
        setBordered(false);
        setMaxLength(MAX_LENGTH);
        super.setResponder(value -> {
            dismissed = false;
            undoQuery = null;
            selection = 0;
            suggestions = null;
            onChange.accept(value);
        });
    }

    private static int textInsetY(Font font, int height) {
        return Math.max(0, (height - font.lineHeight) / 2);
    }

    private int fieldTop() { return getY() - textInsetY; }

    void setIndex(SearchIndex<T> index) {
        this.index = index;
        suggestions = null;
    }

    void setScope(Predicate<? super T> scope) {
        this.scope = scope;
        suggestions = null;
    }

    @Override public void setResponder(Consumer<String> responder) { onChange = responder; }
    void setOnSubmit(Consumer<String> submit) { onSubmit = submit; }

    @Override
    public void setHint(Component hint) {
        this.hint = hint;
        hintColor = 0;
        super.setHint(hint);
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return isActive() && x >= getX() - INSET && x < getRight() + INSET
                && y >= fieldTop() && y < fieldTop() + fieldHeight;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var theme = UiTheme.current();
        var preferences = UiPreferences.configured();
        int edge = isFocused() ? theme.accent() : preferences.highContrast() ? theme.text() : theme.border();
        RoundedCorners.draw(graphics, getX() - INSET, fieldTop(), getWidth() + INSET * 2, fieldHeight, 5,
                isFocused() ? 1.5f : 1, edge, theme.surface() | 0xFF000000);
        setTextColor(theme.text());
        int muted = theme.secondaryText(0xFFB8C9D9, .24f) | 0xFF000000;
        if (hintColor != muted) {
            hintColor = muted;
            super.setHint(hint.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(muted))));
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private List<SearchIndex.Suggestion<T>> suggestions() {
        if (!isFocused() || dismissed) return List.of();
        if (suggestions == null) {
            suggestions = index.suggest(getValue(), scope, LIMIT).stream()
                    .filter(item -> item.completion().length() <= MAX_LENGTH).toList();
            selection = Math.clamp(selection, 0, Math.max(0, suggestions.size() - 1));
        }
        return suggestions;
    }

    boolean dismissSuggestions() {
        if (suggestions().isEmpty()) return false;
        dismissed = true;
        return true;
    }

    private boolean accept(int candidate) {
        var items = suggestions();
        if (candidate < 0 || candidate >= items.size()) return false;
        String previous = getValue();
        setValue(items.get(candidate).completion());
        if (!getValue().equals(previous)) undoQuery = previous;
        dismissed = true;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isFocused()) {
            if (event.key() == GLFW.GLFW_KEY_Z && event.hasControlDownWithQuirk()
                    && !event.hasShiftDown() && !event.hasAltDown() && undoQuery != null) {
                setValue(undoQuery);
                return true;
            }
            if (event.modifiers() == 0) {
                if (event.isEscape()) return dismissSuggestions();
                if (event.isConfirmation()) {
                    dismissSuggestions();
                    onSubmit.accept(getValue());
                    return true;
                }
                if (!suggestions().isEmpty()) {
                    if (event.key() == GLFW.GLFW_KEY_TAB) return accept(selection);
                    if (event.isUp() || event.isDown()) {
                        move(event.isUp() ? -1 : 1);
                        return true;
                    }
                }
            }
        }
        return super.keyPressed(event);
    }

    private void move(int delta) {
        selection = Math.clamp(selection + delta, 0, suggestions().size() - 1);
    }

    Optional<Dropdown> suggestionArea(int screenHeight) {
        int count = suggestions().size();
        if (count == 0) return Optional.empty();
        int rowHeight = font.lineHeight + 7;
        int below = fieldTop() + fieldHeight + GAP;
        int above = fieldTop() - GAP;
        int belowRows = Math.max(0, screenHeight - below) / rowHeight;
        int aboveRows = Math.max(0, above) / rowHeight;
        boolean placeBelow = belowRows >= count || belowRows >= aboveRows;
        int rows = Math.min(count, placeBelow ? belowRows : aboveRows);
        if (rows == 0) return Optional.empty();
        int first = Math.clamp(selection - rows + 1, 0, count - rows);
        int y = placeBelow ? below : above - rows * rowHeight;
        return Optional.of(new Dropdown(getX() - INSET, y, getWidth() + INSET * 2, rowHeight, rows, first));
    }

    boolean mouseClickedSuggestion(double x, double y, int button, int height) {
        var area = suggestionArea(height).filter(box -> box.contains(x, y));
        if (area.isEmpty()) {
            if (!isMouseOver(x, y)) dismissSuggestions();
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) accept(area.get().indexAt(y));
        return true;
    }

    boolean mouseScrolledSuggestions(double x, double y, double delta, int height) {
        if (suggestionArea(height).filter(box -> box.contains(x, y)).isEmpty()) return false;
        if (delta != 0) move(delta > 0 ? -1 : 1);
        return true;
    }

    void renderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int height) {
        suggestionArea(height).ifPresent(box -> {
            var theme = UiTheme.current();
            int fill = theme.surface() | 0xFF000000;
            RoundedCorners.draw(graphics, box.x(), box.y(), box.width(), box.height(), 4, 1, theme.border(), fill);
            graphics.nextStratum();
            for (int row = 0; row < box.rows(); row++) {
                int index = box.first() + row;
                int y = box.y() + row * box.rowHeight();
                if (index == selection || box.contains(mouseX, mouseY) && index == box.indexAt(mouseY)) {
                    RoundedCorners.draw(graphics, box.x() + 1, y + 1, box.width() - 2, box.rowHeight() - 2, 3,
                            1, theme.accent(), theme.controlFill(0xFF253C55, .2f));
                }
                var text = Language.getInstance().getVisualOrder(
                        font.substrByWidth(label(suggestions().get(index)), Math.max(0, box.width() - 10)));
                graphics.text(font, text, box.x() + 5, y + 4, theme.text(), false);
            }
        });
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        super.updateWidgetNarration(output);
        var items = suggestions();
        if (items.isEmpty()) return;
        output.add(NarratedElementType.POSITION,
                Component.translatable("narrator.position.object_list", selection + 1, items.size()));
        output.add(NarratedElementType.HINT, label(items.get(selection)));
        output.add(NarratedElementType.USAGE, Component.translatable("sh_better_ui.search.usage"));
    }

    private static Component label(SearchIndex.Suggestion<?> item) {
        if (!item.label().equals(item.entry().name()) || item.label().equals(item.completion())) {
            return Component.literal(item.label());
        }
        return Component.translatable("sh_better_ui.search.suggestion.id", item.completion(), item.label());
    }

    record Dropdown(int x, int y, int width, int rowHeight, int rows, int first) {
        int height() { return rows * rowHeight; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height();
        }
        int indexAt(double mouseY) { return first + (int) (mouseY - y) / rowHeight; }
    }
}
