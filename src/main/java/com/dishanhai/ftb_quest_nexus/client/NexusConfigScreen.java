package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig.Mode;
import com.dishanhai.sh_better_ui.client.api.MaterialsApi.ThemeChoice;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.gui.display.GraphicDisplayLayout.Bounds;
import com.dishanhai.sh_better_ui.client.gui.display.GraphicIcon;
import com.dishanhai.sh_better_ui.client.gui.scaled.DarkModeTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.LiquidGlassTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.RoundedCorners;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiButton;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiPreferences;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiSlider;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiStyle;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiUnderlinedTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/** Author: dishanhai. A client editor for FTBQ appearance and Nexus interface preferences. */
final class NexusConfigScreen extends Screen {
    private static final String KEY = "config.ftb_quest_nexus.ftbq_ui";
    // Match SH-Better-UI ControlPanelScreen's viewport limits.
    private static final int MAX_PANEL_WIDTH = 624;
    private static final int MAX_PANEL_HEIGHT = 330;
    private static final int SCREEN_MARGIN = 6;
    private static final int PADDING = 14;
    private static final int GAP = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int BODY_TOP = 54;

    private enum Page { APPEARANCE, FEATURES, BLOCK_APPEARANCE }

    static {
        GaussBlur.allowScreen(screen -> screen instanceof NexusConfigScreen);
    }

    private final Screen parent;
    private final boolean loaded = ClientUiConfig.SPEC.isLoaded();
    private final Mode savedMode = loaded ? ClientUiConfig.MODE.get() : ClientUiConfig.MODE.getDefault();
    private final double savedOpacity = loaded ? ClientUiConfig.OPACITY.get() : ClientUiConfig.OPACITY.getDefault();
    private final int savedBlur = loaded ? ClientUiConfig.BLUR_RADIUS.get() : ClientUiConfig.BLUR_RADIUS.getDefault();
    private final boolean savedSubmitterButton = ClientUiConfig.showSubmitterButton();
    private final boolean savedSearchSuggestions = ClientUiConfig.searchSuggestions();
    private final String savedSubmitterTheme = ClientUiConfig.submitterTheme();
    private final String savedCheckerTheme = ClientUiConfig.checkerTheme();
    private Page page = Page.APPEARANCE;
    private Mode mode = savedMode;
    private double opacity = savedOpacity;
    private int blur = savedBlur;
    private boolean submitterButton = savedSubmitterButton;
    private boolean searchSuggestions = savedSearchSuggestions;
    private String submitterTheme = savedSubmitterTheme;
    private String checkerTheme = savedCheckerTheme;
    private boolean saveFailed;
    private AbstractWidget modeControl;
    private AbstractWidget opacitySlider;
    private AbstractWidget blurSlider;
    private AbstractWidget submitterToggle;
    private AbstractWidget suggestionsToggle;
    private UiButton submitterThemeControl;
    private UiButton checkerThemeControl;
    private UiButton apply;

    NexusConfigScreen(Screen parent) {
        super(Component.translatable(KEY + ".title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = panelWidth() - 2 * PADDING;
        int x = panelLeft() + PADDING;
        int y = panelTop();
        addRenderableWidget(tabs(x, y + 28, contentWidth, page.ordinal(),
                List.of(pageLabel(Page.APPEARANCE), pageLabel(Page.FEATURES),
                        pageLabel(Page.BLOCK_APPEARANCE)), this::selectPage));
        int bodyY = y + BODY_TOP + 4;
        modeControl = addRenderableWidget(tabs(x, bodyY, contentWidth, mode.ordinal(),
                List.of(modeLabel(Mode.ORIGINAL), modeLabel(Mode.GLASS)), this::selectMode));
        int sliderTop = bodyY + ROW_HEIGHT + 4;
        opacitySlider = addRenderableWidget(slider(x, sliderTop, contentWidth, 100,
                (int) Math.round(opacity * 100), value -> label("opacity_value", value), value -> {
                    opacity = value / 100.0;
                    draftChanged();
                }));
        blurSlider = addRenderableWidget(slider(x, sliderTop + ROW_HEIGHT + 4, contentWidth, 16,
                blur, value -> label("blur_value", value), value -> {
                    blur = value;
                    draftChanged();
                }));
        submitterToggle = addRenderableWidget(toggle(x, bodyY + 2, contentWidth, "submitter_button",
                () -> submitterButton, value -> {
                    submitterButton = value;
                    draftChanged();
                }));
        suggestionsToggle = addRenderableWidget(toggle(x, bodyY + ROW_HEIGHT + GAP + 2, contentWidth,
                "search_suggestions", () -> searchSuggestions, value -> {
                    searchSuggestions = value;
                    draftChanged();
                }));
        submitterThemeControl = addRenderableWidget(themeControl(x, bodyY + 12, contentWidth,
                "submitter_theme", () -> submitterTheme, value -> submitterTheme = value));
        checkerThemeControl = addRenderableWidget(themeControl(x, bodyY + 50, contentWidth,
                "checker_theme", () -> checkerTheme, value -> checkerTheme = value));
        int buttonWidth = (contentWidth - GAP) / 2;
        addRenderableWidget(roundedButton(x, footerTop(), buttonWidth, CommonComponents.GUI_CANCEL,
                false, GraphicIcon.BACK, button -> onClose()));
        apply = addRenderableWidget(roundedButton(x + buttonWidth + GAP, footerTop(),
                contentWidth - buttonWidth - GAP, label("apply"), true, GraphicIcon.SUCCESS, button -> apply()));
        updateControls();
    }

    private void selectPage(int index) {
        page = Page.values()[index];
        updateControls();
    }

    private void selectMode(int index) {
        mode = Mode.values()[index];
        draftChanged();
    }

    private void draftChanged() {
        saveFailed = false;
        updateControls();
    }

    private void updateControls() {
        boolean appearance = page == Page.APPEARANCE;
        boolean features = page == Page.FEATURES;
        boolean blocks = page == Page.BLOCK_APPEARANCE;
        showControl(modeControl, appearance, loaded);
        showControl(opacitySlider, appearance, loaded && mode == Mode.GLASS);
        showControl(blurSlider, appearance, loaded && mode == Mode.GLASS);
        showControl(submitterToggle, features, loaded);
        showControl(suggestionsToggle, features, loaded);
        showControl(submitterThemeControl, blocks, loaded);
        showControl(checkerThemeControl, blocks, loaded);
        apply.active = loaded && (mode != savedMode || opacity != savedOpacity || blur != savedBlur
                || submitterButton != savedSubmitterButton || searchSuggestions != savedSearchSuggestions
                || !submitterTheme.equals(savedSubmitterTheme) || !checkerTheme.equals(savedCheckerTheme));
    }

    private static void showControl(AbstractWidget widget, boolean visible, boolean enabled) {
        widget.visible = visible;
        widget.active = visible && enabled;
    }

    private void apply() {
        if (ClientUiConfig.save(mode, opacity, blur, submitterButton, searchSuggestions,
                submitterTheme, checkerTheme)) {
            onClose();
        } else {
            saveFailed = true;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // NeoForge calls this from Screen.render; Forge 1.20.1 uses a different signature.
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int y = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int contentWidth = panelWidth - 2 * PADDING;
        int x = left + PADDING;
        DarkModeTheme.background(graphics, 0, 0, width, height);
        renderPanel(graphics, left, y, panelWidth, panelHeight, contentWidth,
                Math.max(0, footerTop() - y - BODY_TOP - 16));
        int ink = LiquidGlassTheme.textColor();
        drawLabel(graphics, title, x, y + 15, contentWidth, ink);
        if (page == Page.BLOCK_APPEARANCE) {
            drawLabel(graphics, label("submitter_theme"), x, y + BODY_TOP + 5, contentWidth, ink);
            drawLabel(graphics, label("checker_theme"), x, y + BODY_TOP + 43, contentWidth, ink);
        }
        Component status = !loaded ? label("unavailable")
                : saveFailed ? label("save_failed") : label(switch (page) {
                    case APPEARANCE -> "reopen";
                    case FEATURES -> "reopen_features";
                    case BLOCK_APPEARANCE -> "reopen_blocks";
                });
        drawLabel(graphics, status, x, footerTop() - 12, contentWidth,
                LiquidGlassTheme.mutedTextColor());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLabel(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        var line = Language.getInstance().getVisualOrder(font.substrByWidth(text, maxWidth));
        graphics.drawString(font, line, x, y, color, false);
    }

    private int panelWidth() { return Math.min(MAX_PANEL_WIDTH, width - 2 * SCREEN_MARGIN); }
    private int panelHeight() { return Math.min(MAX_PANEL_HEIGHT, height - 2 * SCREEN_MARGIN); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return (height - panelHeight()) / 2; }
    private int footerTop() { return panelTop() + panelHeight() - PADDING - ROW_HEIGHT; }

    private static Component label(String key, Object... args) {
        return Component.translatable(KEY + "." + key, args);
    }

    private static Component modeLabel(Mode value) {
        return label(value == Mode.GLASS ? "glass" : "original");
    }

    private static Component pageLabel(Page value) {
        return label(switch (value) {
            case APPEARANCE -> "appearance";
            case FEATURES -> "features";
            case BLOCK_APPEARANCE -> "block_appearance";
        });
    }

    private UiButton themeControl(int x, int y, int width, String key,
                                  Supplier<String> selected, Consumer<String> changed) {
        List<ThemeChoice> choices = BlockScreenTheme.choices(selected.get());
        List<String> ids = choices.stream().map(ThemeChoice::id).toList();
        var button = roundedButton(x, y, width, CommonComponents.EMPTY, false, GraphicIcon.NEXT, pressed -> {
            int direction = Screen.hasShiftDown() ? -1 : 1;
            int index = Math.floorMod(ids.indexOf(selected.get()) + direction, choices.size());
            ThemeChoice choice = choices.get(index);
            changed.accept(choice.id());
            updateThemeButton((UiButton) pressed, key, choice);
            draftChanged();
        });
        updateThemeButton(button, key, choices.get(ids.indexOf(selected.get())));
        return button;
    }

    private static void updateThemeButton(UiButton button, String key, ThemeChoice choice) {
        Component name = BlockScreenTheme.label(choice);
        button.setMessage(name);
        button.setPreview(Component.translatable("options.generic_value", label(key), name));
    }

    private static UiButton toggle(int x, int y, int width, String key, BooleanSupplier value,
                                   Consumer<Boolean> changed) {
        var toggle = new UiButton(x, y, width, ROW_HEIGHT, CommonComponents.EMPTY, pressed -> {
            boolean selected = !value.getAsBoolean();
            changed.accept(selected);
            updateToggle((UiButton) pressed, key, selected);
        }, UiPreferences.configured(), false, UiStyle.LIQUID_GLASS);
        toggle.setSampleBackground(false);
        toggle.setPreview(label(key + ".tooltip"));
        updateToggle(toggle, key, value.getAsBoolean());
        return toggle;
    }

    private static void updateToggle(UiButton toggle, String key, boolean selected) {
        toggle.setMessage(label("toggle_value", label(key),
                Component.translatable(selected ? "options.on" : "options.off")));
        toggle.setSelected(selected);
        toggle.setModeIndicators(2, selected ? 1 : 0);
    }

    private static UiButton roundedButton(int x, int y, int width, Component text, boolean primary,
                                          GraphicIcon icon, Button.OnPress onPress) {
        var button = new UiButton(x, y, width, ROW_HEIGHT, text, onPress, UiPreferences.configured(),
                primary, UiStyle.LIQUID_GLASS);
        button.setSampleBackground(false);
        button.setLeadingIcon(icon);
        return button;
    }

    private static UiSlider slider(int x, int y, int width, int max, int initial, IntFunction<Component> label,
                                   IntConsumer changed) {
        return new UiSlider(new Bounds(x, y, width, ROW_HEIGHT), 0, max, 1, initial,
                UiPreferences.configured(), label, changed);
    }

    private static UiUnderlinedTabs tabs(int x, int y, int width, int index, List<Component> labels,
                                         IntConsumer onSelect) {
        var tabs = labels.stream().map(label -> new UiUnderlinedTabs.Tab(label, CommonComponents.EMPTY)).toList();
        return new UiUnderlinedTabs(new Bounds(x, y, width, ROW_HEIGHT), tabs, index, onSelect);
    }

    private static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                    int contentWidth, int bodyHeight) {
        var preferences = UiPreferences.configured();
        LiquidGlassTheme.popover(graphics, x, y, width, height, preferences);
        boolean contrast = preferences.highContrast();
        RoundedCorners.draw(graphics, x + PADDING - 3, y + BODY_TOP, contentWidth + 6, bodyHeight, 5f, 1f,
                contrast ? LiquidGlassTheme.textColor() : DarkModeTheme.borderColor(),
                contrast ? DarkModeTheme.backgroundColor() : DarkModeTheme.surfaceColor());
    }
}
