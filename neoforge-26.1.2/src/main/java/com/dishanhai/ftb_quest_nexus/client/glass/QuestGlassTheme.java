package com.dishanhai.ftb_quest_nexus.client.glass;

import dev.ftb.mods.ftblibrary.client.icon.IconHelper;

import com.dishanhai.sh_better_ui.client.blur.GlassStyle;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.glass.GuiRect;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.client.gui.WidgetType;
import dev.ftb.mods.ftbquests.client.gui.FTBQuestsTheme;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Author: dishanhai. Reuses SH-Better-UI surfaces while FTBQ keeps its widgets and semantics. */
public final class QuestGlassTheme extends FTBQuestsTheme {
    private static final GlassTheme GLASS = GlassTheme.GLASS;
    private final double opacity;
    private final int blurRadius;

    public QuestGlassTheme(double opacity, int blurRadius) {
        this.opacity = Math.clamp(opacity, 0.0, 1.0);
        this.blurRadius = Math.clamp(blurRadius, 0, 16);
    }

    public int color(int argb) {
        return GlassUiPolicy.scaleAlpha(argb, opacity);
    }

    public Color4I borderColor() { return Color4I.rgba(color(GLASS.frameDark)); }
    public Color4I backgroundColor() { return Color4I.rgba(color(GLASS.panelInner)); }

    private void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int edge, int fill) {
        if (opacity > 0) GLASS.box(graphics, x, y, w, h, edge, fill);
    }

    public void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        box(graphics, x, y, w, h, color(GLASS.frameDark), color(GLASS.panelInner));
    }

    public void backdrop(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (blurRadius > 0) {
            GlassBlur.backdrop(graphics, new GuiRect(x, y, w, h), 1f, GlassStyle.BACKDROP, blurRadius);
        }
    }

    @Override public void drawGui(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        backdrop(g, x, y, w, h);
        Icon<?> original = ThemeProperties.BACKGROUND.get();
        if (!(original instanceof ImageIcon image && image.getResourceId().equals(BACKGROUND_SQUARES.getResourceId()))) {
            IconHelper.renderIcon(original, g, x, y, w, h);
        }
        box(g, x, y, w, h, color(GLASS.frameDark), color(GLASS.panelBg));
    }

    @Override public void drawWidget(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        drawButton(g, x, y, w, h, type);
    }

    @Override public void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        int fill = type == WidgetType.MOUSE_OVER ? GLASS.buttonHover : GLASS.buttonBg;
        int edge = type == WidgetType.MOUSE_OVER ? GLASS.frame : GLASS.frameDark;
        box(g, x, y, w, h, color(edge), color(type == WidgetType.DISABLED ? GLASS.boxBg : fill));
    }

    @Override public void drawSlot(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, color(type == WidgetType.MOUSE_OVER ? GLASS.frame : GLASS.cellEdge), color(GLASS.boxBg));
    }

    @Override public void drawContainerSlot(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawSlot(g, x, y, w, h, WidgetType.NORMAL);
    }

    @Override public void drawPanelBackground(GuiGraphicsExtractor g, int x, int y, int w, int h) { panel(g, x, y, w, h); }
    @Override public void drawContextMenuBackground(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, color(GLASS.frameDark), color(GLASS.rowBg));
    }
    @Override public void drawTextBox(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, color(GLASS.barBorder), color(GLASS.boxBg));
    }
    @Override public void drawScrollBarBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, color(GLASS.barBorder), color(GLASS.barBg));
    }
    @Override public void drawScrollBar(GuiGraphicsExtractor g, int x, int y, int w, int h, WidgetType type, boolean vertical) {
        box(g, x, y, w, h, color(GLASS.frameDark), color(type == WidgetType.MOUSE_OVER ? GLASS.frame : GLASS.buttonHover));
    }
    @Override public void drawCheckboxBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean checked) {
        drawSlot(g, x, y, w, h, checked ? WidgetType.MOUSE_OVER : WidgetType.NORMAL);
    }
    @Override public void drawHorizontalTab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected) {
        box(g, x, y, w, h, color(selected ? GLASS.frame : GLASS.frameDark), color(selected ? GLASS.selectBg : GLASS.buttonBg));
    }
}
