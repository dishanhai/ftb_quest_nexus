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

/** Author: dishanhai. Limits the overlay to FTBQ screens and their own child dialogs. */
public final class QuestUiHooks {
    private record Selection(boolean owned, QuestGlassTheme theme) {}
    private static final Map<BaseScreen, Selection> THEMES = new WeakHashMap<>();

    private QuestUiHooks() {}

    public static void opened(BaseScreen screen) {
        THEMES.remove(screen);
        if (theme(screen) != null) GlassBlur.warmUp();
    }

    public static QuestGlassTheme theme(BaseScreen screen) {
        Selection selection = THEMES.get(screen);
        if (selection == null) {
            selection = select(screen);
            THEMES.put(screen, selection);
        }
        return selection.theme();
    }

    private static Selection select(BaseScreen screen) {
        boolean owned = false;
        Set<BaseScreen> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BaseScreen current = screen; current != null && visited.add(current); ) {
            String name = current.getClass().getName();
            if (GlassUiPolicy.isQuestScreenClass(name)) owned = true;
            else if (!GlassUiPolicy.isLibraryScreenClass(name)) break;
            Selection inherited = current == screen ? null : THEMES.get(current);
            if (inherited != null && inherited.owned()) return inherited;
            Screen previous = current.getPrevScreen();
            current = previous instanceof IScreenWrapper wrapper ? wrapper.getGui() : null;
        }
        if (!owned) return new Selection(false, null);
        return new Selection(true, ClientUiConfig.MODE.get() == ClientUiConfig.Mode.GLASS
                ? new QuestGlassTheme(ClientUiConfig.OPACITY.get(), ClientUiConfig.BLUR_RADIUS.get()) : null);
    }

    public static boolean isGlass(Screen screen) {
        return screen instanceof IScreenWrapper wrapper && theme(wrapper.getGui()) != null;
    }
}
