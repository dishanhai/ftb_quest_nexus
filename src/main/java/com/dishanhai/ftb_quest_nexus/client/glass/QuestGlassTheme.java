package com.dishanhai.ftb_quest_nexus.client.glass;

import com.dishanhai.sh_better_ui.client.blur.GlassStyle;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.glass.GuiRect;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftbquests.client.gui.FTBQuestsTheme;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphics;

/** Author: dishanhai. Structural skin; FTBQ still supplies text, icons and status colours. */
public final class QuestGlassTheme extends FTBQuestsTheme {
    private static final GlassTheme MATERIAL = GlassTheme.GLASS;
    private final double opacity;
    private final float blurRadius;
    private final boolean canvas;

    QuestGlassTheme(double opacity, double blurRadius, boolean canvas) {
        this.opacity = opacity;
        this.blurRadius = (float) blurRadius;
        this.canvas = canvas;
    }

    public int color(int argb) { return GlassUiPolicy.scaleAlpha(argb, opacity); }
    public Color4I borderColor() { return Color4I.rgba(color(MATERIAL.frameDark)); }
    public Color4I sidebarColor() { return Color4I.rgba(color(MATERIAL.panelInner)); }

    private void box(GuiGraphics g, int x, int y, int w, int h, int border, int fill) {
        if (w <= 0 || h <= 0 || opacity <= 0) return;
        MATERIAL.box(g, x, y, w, h, color(border), color(fill));
    }

    @Override public void drawGui(GuiGraphics g, int x, int y, int w, int h, WidgetType type) {
        if (blurRadius > 0) {
            GlassBlur.backdrop(g, new GuiRect(x, y, w, h), 1f, GlassStyle.BACKDROP, blurRadius);
        }
        if (canvas) {
            Icon background = ThemeProperties.BACKGROUND.get();
            // Replace the built-in squares while retaining a pack's custom canvas image.
            if (!(background instanceof ImageIcon image)
                    || !image.texture.toString().equals("ftblibrary:textures/gui/background_squares.png")) {
                background.draw(g, x, y, w, h);
            }
        }
        box(g, x, y, w, h, MATERIAL.frameDark, MATERIAL.panelBg);
    }

    @Override public void drawButton(GuiGraphics g, int x, int y, int w, int h, WidgetType type) {
        int fill = type == WidgetType.MOUSE_OVER ? MATERIAL.buttonHover : MATERIAL.buttonBg;
        if (type == WidgetType.DISABLED) fill = MATERIAL.boxBg;
        box(g, x, y, w, h, type == WidgetType.MOUSE_OVER ? MATERIAL.frame : MATERIAL.frameDark, fill);
    }

    @Override public void drawWidget(GuiGraphics g, int x, int y, int w, int h, WidgetType type) {
        drawButton(g, x, y, w, h, type);
    }

    @Override public void drawSlot(GuiGraphics g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, type == WidgetType.MOUSE_OVER ? MATERIAL.frame : MATERIAL.cellEdge, MATERIAL.boxBg);
    }

    @Override public void drawContainerSlot(GuiGraphics g, int x, int y, int w, int h) {
        drawSlot(g, x, y, w, h, WidgetType.NORMAL);
    }

    @Override public void drawPanelBackground(GuiGraphics g, int x, int y, int w, int h) {
        box(g, x, y, w, h, MATERIAL.frameDark, MATERIAL.panelInner);
    }

    @Override public void drawContextMenuBackground(GuiGraphics g, int x, int y, int w, int h) {
        box(g, x, y, w, h, MATERIAL.frameDark, MATERIAL.buttonBg);
    }

    @Override public void drawScrollBarBackground(GuiGraphics g, int x, int y, int w, int h, WidgetType type) {
        box(g, x, y, w, h, MATERIAL.barBorder, MATERIAL.barBg);
    }

    @Override public void drawScrollBar(GuiGraphics g, int x, int y, int w, int h, WidgetType type, boolean vertical) {
        box(g, x, y, w, h, type == WidgetType.MOUSE_OVER ? MATERIAL.frame : MATERIAL.frameDark, MATERIAL.buttonHover);
    }

    @Override public void drawTextBox(GuiGraphics g, int x, int y, int w, int h) {
        box(g, x, y, w, h, MATERIAL.frameDark, MATERIAL.boxBg);
    }

    @Override public void drawCheckboxBackground(GuiGraphics g, int x, int y, int w, int h, boolean radio) {
        drawSlot(g, x, y, w, h, WidgetType.NORMAL);
    }

    @Override public void drawCheckbox(GuiGraphics g, int x, int y, int w, int h, WidgetType type, boolean selected, boolean radio) {
        // Retain an unambiguous selection mark instead of another full opaque widget.
        if (selected && opacity > 0) g.fill(x + 3, y + 3, x + w - 3, y + h - 3, color(MATERIAL.frame));
    }

    @Override public void drawHorizontalTab(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        box(g, x, y, w, h, selected ? MATERIAL.frame : MATERIAL.frameDark, selected ? MATERIAL.selectBg : MATERIAL.buttonBg);
    }
}
