package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.theme.property.ColorProperty;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. The editor status bar bypasses the normal theme API. */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class FtbQuestStatusGlassMixin {
    @Redirect(method = "drawStatusBar", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/theme/property/ColorProperty;get()Ljava/lang/Object;"))
    private Object nexus$statusBackground(ColorProperty property) {
        var theme = QuestGlassUi.theme(((QuestPanel) (Object) this).getGui());
        if (theme != null && property == ThemeProperties.WIDGET_BACKGROUND) return theme.sidebarColor();
        return property.get();
    }
}
