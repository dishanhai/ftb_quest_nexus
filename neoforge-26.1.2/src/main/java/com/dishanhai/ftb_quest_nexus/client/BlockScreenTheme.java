package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.client.glass.NexusSurface;
import com.dishanhai.sh_better_ui.client.gui.scaled.RoundedCorners;
import com.dishanhai.sh_better_ui.client.gui.scaled.UiTheme;
import com.dishanhai.sh_better_ui.kernel.api.glass.GlassVariant;
import com.dishanhai.sh_better_ui.theme.BuiltinThemes;
import com.dishanhai.sh_better_ui.theme.ThemeManager;
import com.dishanhai.sh_better_ui.theme.ThemeResolver;
import com.dishanhai.sh_better_ui.theme.resource.ThemeJsonParser;
import com.dishanhai.sh_better_ui.theme.resource.ThemeResourceLoader;
import com.dishanhai.sh_better_ui.theme.token.ThemeTokens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Author: dishanhai. Resolves independent palettes without changing SHBUI's global theme. */
final class BlockScreenTheme {
    private static final String FOLLOW = "ftb_quest_nexus:follow_shbui";
    private static final String LABEL = "config.ftb_quest_nexus.ftbq_ui.";
    private static final String PRESET_LABEL = "sh_better_ui.panel.theme.group.";
    private static final int COLORS = 31;
    private static final ThemeManager THEMES = new ThemeManager();
    private static final ThemeResolver RESOLVER = new ThemeResolver();
    private static final ThemeResourceLoader LOADER = new ThemeResourceLoader(new ThemeJsonParser());
    private static final Map<String, Frame> RESOLVED = new HashMap<>();
    private static final List<Preset> PRESETS = List.of(
            preset("classic", "classic", 0xFF241910, 0xFF33271B, 0xFF8A5B26, 0xFFFFAA00, null),
            preset("glass", "clearGlass", 0xCC0E1726, 0xEA17283C, 0xA64B7395, 0xFF9ACDFF, GlassVariant.CLEAR),
            preset("smoke", "smokeGlass", 0xCC24282C, 0xEA30363B, 0xA6A8B0B4, 0xFF86B5BF, GlassVariant.REGULAR),
            preset("slate", "slate", 0xFF151C26, 0xFF202B39, 0xFF53657A, 0xFF9ACDFF, null),
            new Preset(new ThemeChoice("liquidGlass", PRESET_LABEL + "liquidGlass"),
                    new Frame(UiTheme.fromTokens(BuiltinThemes.liquidGlassSnapshot()), GlassVariant.REGULAR, 0xFF8CD6B1)),
            preset("network", "network", 0xCC090E16, 0xEA111F2C, 0xFF316E9B, 0xFF86C5F4, GlassVariant.REGULAR),
            preset("aurora", "aurora", 0xCC0C2524, 0xEA173634, 0xA65A9D88, 0xFF80E3BD, GlassVariant.REGULAR),
            preset("amethyst", "amethyst", 0xCC22172F, 0xEA352743, 0xA6846BA6, 0xFFC6ACFF, GlassVariant.REGULAR),
            preset("rose", "rose", 0xCC301C29, 0xEA462B3C, 0xA6A5728D, 0xFFF5B3CD, GlassVariant.REGULAR),
            preset("ember", "ember", 0xCC2D2118, 0xEA433226, 0xA6A77C54, 0xFFFFCB8C, GlassVariant.REGULAR));
    private static final Frame DEFAULT = PRESETS.get(1).frame();
    private Frame frame = DEFAULT;

    record ThemeChoice(String id, String nameKey) {}
    private record Preset(ThemeChoice choice, Frame frame) {}

    private static Preset preset(String id, String name, int background, int surface, int edge, int accent,
                                 GlassVariant glass) {
        return new Preset(new ThemeChoice(id, PRESET_LABEL + name),
                new Frame(new UiTheme(false, background, surface, edge, 0xFFE5EEF8, accent, COLORS),
                        glass, 0xFF8CD6B1));
    }

    static void reload(ResourceManager resources) {
        var entries = new ArrayList<ThemeResourceLoader.Resource>();
        resources.listResourceStacks(ThemeResourceLoader.DIRECTORY, path -> path.getPath().endsWith(".json"))
                .forEach((path, stack) -> {
                    String id = ThemeResourceLoader.themeId(path.getNamespace(), path.getPath());
                    for (var resource : stack) {
                        entries.add(new ThemeResourceLoader.Resource(id, resource.sourcePackId(), resource::open));
                    }
                });
        var batch = LOADER.prepare(entries, THEMES.tokenCatalog());
        if (batch.error() == null) {
            THEMES.replaceResourceThemes(batch.definitions(), batch.diagnostics());
            RESOLVED.clear();
        } else {
            THEMES.resolutionFailed(new IllegalArgumentException(batch.error()));
        }
    }

    static List<ThemeChoice> choices(String selected) {
        var choices = new ArrayList<ThemeChoice>();
        choices.add(new ThemeChoice(FOLLOW, LABEL + "follow_shbui"));
        PRESETS.forEach(preset -> choices.add(preset.choice()));
        THEMES.registry().themes().values().forEach(theme -> choices.add(new ThemeChoice(theme.id(), theme.nameKey())));
        if (choices.stream().noneMatch(choice -> choice.id().equals(selected))) {
            choices.add(new ThemeChoice(selected, LABEL + "missing_theme"));
        }
        return choices;
    }

    static Component label(ThemeChoice choice) {
        return Component.translatableWithFallback(choice.nameKey(), choice.id(), choice.id());
    }

    Frame current() { return frame; }

    Frame resolve(String id) {
        if (FOLLOW.equals(id)) {
            var theme = UiTheme.current();
            return frame = new Frame(theme, variant(theme), theme.light() ? 0xFF186144 : 0xFF8CD6B1);
        }
        return frame = RESOLVED.computeIfAbsent(id, BlockScreenTheme::resolvePreset);
    }

    private static Frame resolvePreset(String id) {
        for (var preset : PRESETS) {
            if (preset.choice().id().equals(id)) return preset.frame();
        }
        if (THEMES.registry().find(id).isEmpty()) return DEFAULT;
        var snapshot = RESOLVER.resolve(THEMES.registry(), THEMES.tokenCatalog(), List.of(id), null);
        var theme = UiTheme.fromTokens(snapshot);
        return new Frame(theme, variant(theme), snapshot.resolve(ThemeTokens.SUCCESS).argb());
    }

    static GlassVariant variant(UiTheme theme) {
        if (theme.hudGlass()) return GlassVariant.CLEAR;
        return theme.liquidGlass() ? GlassVariant.REGULAR : null;
    }

    record Frame(UiTheme theme, GlassVariant variant, int completeText) {
        void paint(Runnable extract) { UiTheme.preview(theme, extract); }
        int text() { return theme.text(); }
        int muted() { return theme.secondaryText(0xFFB8C9D9, .24f); }

        void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
            NexusSurface.panel(graphics, x, y, width, height, theme.background(),
                    theme.border(), variant, 1, -1);
            graphics.nextStratum();
        }

        void list(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
            RoundedCorners.draw(graphics, x, y, width, height, 4, 1, theme.border(), theme.surface());
        }
    }
}
