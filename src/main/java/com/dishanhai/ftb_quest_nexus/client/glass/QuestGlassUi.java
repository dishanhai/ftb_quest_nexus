package com.dishanhai.ftb_quest_nexus.client.glass;

import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import com.dishanhai.sh_better_ui.client.glass.GlassBlur;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import net.minecraft.client.gui.screens.Screen;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Author: dishanhai. Per-open snapshots prevent config reloads changing half a screen. */
public final class QuestGlassUi {
    private static final Map<BaseScreen, Appearance> APPEARANCES = new WeakHashMap<>();
    private static final Appearance ORIGINAL = new Appearance(false, 1, 0, false);

    private QuestGlassUi() {}

    public static void opened(BaseScreen gui) {
        APPEARANCES.remove(gui);
        Appearance appearance = appearance(gui);
        if (appearance.glass && appearance.blurRadius > 0) GlassBlur.warmUp();
    }

    public static Appearance appearance(BaseScreen gui) {
        Appearance cached = APPEARANCES.get(gui);
        if (cached != null) return cached;
        Appearance result = resolve(gui, Collections.newSetFromMap(new IdentityHashMap<>()));
        APPEARANCES.put(gui, result);
        return result;
    }

    private static Appearance resolve(BaseScreen gui, Set<BaseScreen> seen) {
        if (!seen.add(gui)) return ORIGINAL;
        Appearance cached = APPEARANCES.get(gui);
        if (cached != null) return cached;
        String name = gui.getClass().getName();
        boolean quest = GlassUiPolicy.isQuestScreenClass(name);
        boolean library = GlassUiPolicy.isLibraryScreenClass(name);
        if ((quest || library) && gui.getPrevScreen() instanceof IScreenWrapper wrapper) {
            Appearance parent = resolve(wrapper.getGui(), seen);
            if (parent != ORIGINAL) {
                return new Appearance(parent.glass, parent.opacity, parent.blurRadius, false);
            }
        }
        if (quest) {
            return new Appearance(ClientUiConfig.MODE.get() == ClientUiConfig.Mode.GLASS,
                    ClientUiConfig.OPACITY.get(), ClientUiConfig.BLUR_RADIUS.get(),
                    name.equals("dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen"));
        }
        return ORIGINAL;
    }

    public static boolean active(Screen screen) {
        return screen instanceof IScreenWrapper wrapper && appearance(wrapper.getGui()).glass;
    }

    public static QuestGlassTheme theme(BaseScreen gui) {
        return appearance(gui).theme;
    }

    public static final class Appearance {
        public final boolean glass;
        public final double opacity;
        public final double blurRadius;
        public final QuestGlassTheme theme;

        private Appearance(boolean glass, double opacity, double blurRadius, boolean canvas) {
            this.glass = glass;
            this.opacity = opacity;
            this.blurRadius = blurRadius;
            theme = glass ? new QuestGlassTheme(opacity, blurRadius, canvas) : null;
        }
    }
}
