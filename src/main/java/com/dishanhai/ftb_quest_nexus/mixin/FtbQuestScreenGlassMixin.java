package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.theme.property.ColorProperty;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. Replace structural colours without cancelling drag/box selection. */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class FtbQuestScreenGlassMixin {
    @Redirect(method = {"drawBackground", "drawForeground"}, at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/theme/property/ColorProperty;get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;"))
    private Object nexus$structuralColor(ColorProperty property, QuestObjectBase object) {
        var theme = QuestGlassUi.theme((QuestScreen) (Object) this);
        if (theme != null) {
            if (property == ThemeProperties.WIDGET_BORDER) return theme.borderColor();
            if (property == ThemeProperties.WIDGET_BACKGROUND) return theme.sidebarColor();
        }
        return property.get(object);
    }
}
