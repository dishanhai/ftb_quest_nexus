package com.dishanhai.ftb_quest_nexus.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.concurrent.ConcurrentCommentedConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Author: dishanhai. Local FTB Quest Nexus interface preferences. */
public final class ClientUiConfig {
    private static final String SECTION = "ftbqUi";
    private static volatile ModConfig clientConfig;

    public enum Mode { ORIGINAL, GLASS }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.DoubleValue OPACITY;
    public static final ModConfigSpec.IntValue BLUR_RADIUS;
    private static final ModConfigSpec.BooleanValue SHOW_SUBMITTER_BUTTON;
    private static final ModConfigSpec.BooleanValue SEARCH_SUGGESTIONS;
    private static final ModConfigSpec.ConfigValue<String> SUBMITTER_THEME;
    private static final ModConfigSpec.ConfigValue<String> CHECKER_THEME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.translation("config.ftb_quest_nexus.ftbq_ui").push(SECTION);
        MODE = builder.translation("config.ftb_quest_nexus.ftbq_ui.mode")
                .defineEnum("mode", Mode.ORIGINAL);
        OPACITY = builder.translation("config.ftb_quest_nexus.ftbq_ui.opacity")
                .defineInRange("opacity", 1.0, 0.0, 1.0);
        BLUR_RADIUS = builder.translation("config.ftb_quest_nexus.ftbq_ui.blur_radius")
                .defineInRange("blurRadius", 8, 0, 16);
        SHOW_SUBMITTER_BUTTON = builder.translation("config.ftb_quest_nexus.ftbq_ui.submitter_button")
                .define("showSubmitterButton", true);
        SEARCH_SUGGESTIONS = builder.translation("config.ftb_quest_nexus.ftbq_ui.search_suggestions")
                .define("searchSuggestions", true);
        SUBMITTER_THEME = builder.translation("config.ftb_quest_nexus.ftbq_ui.submitter_theme")
                .define("submitterTheme", "glass");
        CHECKER_THEME = builder.translation("config.ftb_quest_nexus.ftbq_ui.checker_theme")
                .define("checkerTheme", "glass");
        builder.pop();
        SPEC = builder.build();
    }

    private ClientUiConfig() {}

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) clientConfig = event.getConfig();
    }

    public static boolean showSubmitterButton() {
        return SPEC.isLoaded() ? SHOW_SUBMITTER_BUTTON.get() : SHOW_SUBMITTER_BUTTON.getDefault();
    }

    public static boolean searchSuggestions() {
        return SPEC.isLoaded() ? SEARCH_SUGGESTIONS.get() : SEARCH_SUGGESTIONS.getDefault();
    }

    public static String submitterTheme() {
        return SPEC.isLoaded() ? SUBMITTER_THEME.get() : SUBMITTER_THEME.getDefault();
    }

    public static String checkerTheme() {
        return SPEC.isLoaded() ? CHECKER_THEME.get() : CHECKER_THEME.getDefault();
    }

    public static boolean save(Mode mode, double opacity, int blurRadius,
                               boolean showSubmitterButton, boolean searchSuggestions,
                               String submitterTheme, String checkerTheme) {
        ModConfig config = clientConfig;
        var loaded = config == null ? null : config.getLoadedConfig();
        if (loaded == null || !(loaded.config() instanceof ConcurrentCommentedConfig data)) return false;
        CommentedConfig previous = data.get(SECTION);
        if (previous == null) return false;
        CommentedConfig updated = data.createSubConfig();
        for (var entry : previous.entrySet()) updated.set(entry.getKey(), entry.getRawValue());
        updated.putAllComments(previous);
        updated.set("mode", mode.name());
        updated.set("opacity", opacity);
        updated.set("blurRadius", blurRadius);
        updated.set("showSubmitterButton", showSubmitterButton);
        updated.set("searchSuggestions", searchSuggestions);
        updated.set("submitterTheme", submitterTheme);
        updated.set("checkerTheme", checkerTheme);
        data.set(SECTION, updated);
        SPEC.afterReload();
        try {
            loaded.save();
            return true;
        } catch (WritingException error) {
            data.set(SECTION, previous);
            SPEC.afterReload();
            LogUtils.getLogger().warn("Could not save {}", config.getFileName(), error);
            return false;
        }
    }
}
