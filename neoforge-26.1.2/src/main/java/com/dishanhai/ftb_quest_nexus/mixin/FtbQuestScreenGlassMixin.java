package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import com.dishanhai.ftb_quest_nexus.client.glass.QuestUiHooks;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.theme.property.ColorProperty;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Keeps the original screen's dragging and selection code intact. */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class FtbQuestScreenGlassMixin {
    @Inject(method = "getTheme", at = @At("HEAD"), cancellable = true)
    private void nexus$theme(CallbackInfoReturnable<Theme> ci) {
        QuestGlassTheme theme = QuestUiHooks.theme((QuestScreen) (Object) this);
        if (theme != null) ci.setReturnValue(theme);
    }

    @Redirect(method = {"drawBackground", "drawForeground"}, at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/theme/property/ColorProperty;get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;"))
    private Object nexus$structureColors(ColorProperty property, QuestObjectBase object) {
        QuestGlassTheme theme = QuestUiHooks.theme((QuestScreen) (Object) this);
        if (theme != null) {
            if (property == ThemeProperties.WIDGET_BORDER) return theme.borderColor();
            if (property == ThemeProperties.WIDGET_BACKGROUND) return theme.backgroundColor();
        }
        return property.get(object);
    }
}
