package com.dishanhai.ftb_quest_nexus.client;

import java.util.List;

/** Geometry shared by the FTBQ injection and its coexistence tests. No client classes are linked here. */
public final class HeaderButtonLayout {
    private HeaderButtonLayout() {}
    public record Bounds(int x, int y, int width, int height) {}
    public record Placement(int buttonX, int titleX, int titleWidth) {}

    public static Placement place(int panelWidth, int iconSize, List<Bounds> existingButtons) {
        int x = iconSize + 8;
        int right = panelWidth - 4;
        for (Bounds button : existingButtons) {
            if (button.y() >= 4 + iconSize || button.y() + button.height() <= 0) continue;
            if (button.x() < panelWidth / 2) x = Math.max(x, button.x() + button.width() + 2);
            else right = Math.min(right, button.x() - 2);
        }
        int titleX = x + iconSize + 3;
        return new Placement(x, titleX, Math.max(1, right - titleX));
    }
}
