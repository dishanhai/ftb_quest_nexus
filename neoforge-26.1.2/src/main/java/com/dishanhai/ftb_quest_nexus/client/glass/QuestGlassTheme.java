package com.dishanhai.ftb_quest_nexus.client.glass;

import dev.ftb.mods.ftblibrary.client.icon.IconHelper;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.client.gui.WidgetType;
import dev.ftb.mods.ftbquests.client.gui.FTBQuestsTheme;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.dishanhai.sh_better_ui.client.gui.scaled.RoundedCorners;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassVariant;

/** Author: dishanhai. Reuses SH-Better-UI surfaces while FTBQ keeps its widgets and semantics. */
public final class QuestGlassTheme extends FTBQuestsTheme {
    private static final int EDGE = 0x98547999;
    private static final int ACCENT = 0xFF9ACDFF;
    private static final int PANEL = 0xA00E1726;
    private static final int INNER = 0x80101928;
    private static final int CONTROL = 0xBD183049;
    private static final int HOVER = 0xDA254A69;
    private final double opacity;
    private final int blurRadius;

    public QuestGlassTheme(double opacity, int blurRadius) {
        this.opacity = Math.clamp(opacity, 0.0, 1.0);
        this.blurRadius = Math.clamp(blurRadius, 0, 16);
    }

    public int color(int argb) {
        return GlassUiPolicy.scaleAlpha(argb, opacity);
    }

    public Color4I borderColor() { return Color4I.rgba(color(EDGE)); }
    public Color4I backgroundColor() { return Color4I.rgba(color(INNER)); }

    private void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int edge, int fill) {
        if (opacity > 0) RoundedCorners.draw(graphics, x, y, w, h, 3, 1, edge, fill);
    }

    public void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        box(graphics, x, y, w, h, color(EDGE), color(INNER));
    }

    @Override public void drawGui(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {

        Icon<?> original = ThemeProperties.BACKGROUND.get();
        if (!(original instanceof ImageIcon image && image.getResourceId().equals(BACKGROUND_SQUARES.getResourceId()))) {
            IconHelper.renderIcon(original, g, x, y, w, h);
        }
        NexusSurface.panel(g, x, y, w, h, PANEL, EDGE, GlassVariant.REGULAR, (float) opacity, blurRadius);
        g.nextStratum();
    }

    @Override public void drawWidget(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        drawButton(g, x, y, w, h, type);
    }

    @Override public void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        int fill = type == WidgetType.MOUSE_OVER ? HOVER : CONTROL;
        int edge = type == WidgetType.MOUSE_OVER ? ACCENT : EDGE;
        box(g, x, y, w, h, color(edge), color(type == WidgetType.DISABLED ? INNER : fill));
    }

    @Override public void drawSlot(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, color(type == WidgetType.MOUSE_OVER ? ACCENT : EDGE), color(INNER));
    }

    @Override public void drawContainerSlot(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawSlot(g, x, y, w, h, WidgetType.NORMAL);
    }

    @Override public void drawPanelBackground(GuiGraphicsExtractor g, int x, int y, int w, int h) { panel(g, x, y, w, h); }
    @Override public void drawContextMenuBackground(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, color(EDGE), color(PANEL));
    }
    @Override public void drawTextBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, color(EDGE), color(INNER));
    }
    @Override public void drawScrollBarBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, color(EDGE), color(INNER));
    }
    @Override public void drawScrollBar(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type, boolean vertical) {
        box(g, x, y, w, h, color(EDGE), color(type == WidgetType.MOUSE_OVER ? ACCENT : HOVER));
    }
    @Override public void drawCheckboxBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean checked) {
        drawSlot(g, x, y, w, h, checked ? WidgetType.MOUSE_OVER : WidgetType.NORMAL);
    }
    @Override public void drawHorizontalTab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected) {
        box(g, x, y, w, h, color(selected ? ACCENT : EDGE), color(selected ? HOVER : CONTROL));
    }
}
