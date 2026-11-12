package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.sh_better_ui.client.api.MaterialsApi.ThemeChoice;
import com.dishanhai.sh_better_ui.client.api.ShBetterUiApi;
import com.dishanhai.sh_better_ui.client.glass.GlassTheme;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiTheme;
import com.dishanhai.sh_better_ui.kernel.api.UIApis;
import com.dishanhai.sh_better_ui.kernel.api.UnifiedUI;
import com.dishanhai.sh_better_ui.theme.ThemeSnapshot;
import com.dishanhai.sh_better_ui.theme.token.ThemeTokens;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Author: dishanhai. Resolves the panel and control palette together for one screen. */
final class BlockScreenTheme {
    private static final String FOLLOW_SHBUI = "ftb_quest_nexus:follow_shbui";
    private static final String FOLLOW_LABEL = "config.ftb_quest_nexus.ftbq_ui.follow_shbui";
    private static final String MISSING_LABEL = "config.ftb_quest_nexus.ftbq_ui.missing_theme";
    private static final int ALL_COLORS = 0b11111;
    private static final Frame DEFAULT = new Frame(GlassTheme.GLASS, null);

    private ThemeSnapshot snapshot;
    private Frame frame = DEFAULT;

    static List<ThemeChoice> choices(String selected) {
        var choices = new ArrayList<ThemeChoice>();
        choices.add(new ThemeChoice(FOLLOW_SHBUI, FOLLOW_LABEL));
        choices.addAll(ShBetterUiApi.materials().themeChoices());
        if (choices.stream().noneMatch(choice -> choice.id().equals(selected))) {
            choices.add(new ThemeChoice(selected, MISSING_LABEL));
        }
        return choices;
    }

    static Component label(ThemeChoice choice) {
        return Component.translatableWithFallback(choice.nameKey(), choice.id(), choice.id());
    }

    Frame current() {
        return frame;
    }

    Frame resolve(String id) {
        if (FOLLOW_SHBUI.equals(id)) {
            var next = UnifiedUI.isAvailable()
                    ? UnifiedUI.get().find(UIApis.THEME).map(service -> service.snapshot()).orElse(null) : null;
            if (next == null) return reset();
            if (next != snapshot) {
                frame = new Frame(GlassTheme.fromSnapshot(next), new UiTheme(next.resolve(ThemeTokens.LIGHT),
                        next.resolve(ThemeTokens.BACKGROUND).argb(), next.resolve(ThemeTokens.SURFACE).argb(),
                        next.resolve(ThemeTokens.BORDER).argb(), next.resolve(ThemeTokens.TEXT).argb(),
                        next.resolve(ThemeTokens.ACCENT).argb(), ALL_COLORS));
                snapshot = next;
            }
        } else {
            GlassTheme material = ShBetterUiApi.materials().findTheme(id).orElse(GlassTheme.GLASS);
            if (material == GlassTheme.GLASS) return reset();
            if (snapshot != null || material != frame.panel()) {
                int ink = material.colors.TEXT;
                boolean light = (ink & 0xff) + (ink >> 8 & 0xff) + (ink >> 16 & 0xff) < 384;
                frame = new Frame(material, new UiTheme(light, material.panelBg, material.buttonBg,
                        material.frameDark, ink, material.frame, ALL_COLORS));
                snapshot = null;
            }
        }
        return frame;
    }

    private Frame reset() {
        snapshot = null;
        return frame = DEFAULT;
    }

    record Frame(GlassTheme panel, UiTheme controls) {
        void paint(Runnable draw) {
            if (controls == null) draw.run();
            else UiTheme.preview(controls, draw);
        }

        int completeText() {
            return panel == GlassTheme.GLASS ? 0xFF8CD6B1 : panel.colors.OK;
        }
    }
}
