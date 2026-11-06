package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.EditRewardTableScreen;
import dev.ftb.mods.ftbquests.client.gui.EmergencyItemsScreen;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import dev.ftb.mods.ftbquests.client.gui.RewardNotificationsScreen;
import dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.ValidItemsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. These FTBQ classes override BaseScreen.getTheme themselves. */
@Mixin(value = {QuestScreen.class, EditRewardTableScreen.class, EmergencyItemsScreen.class,
        MultilineTextEditorScreen.class, RewardNotificationsScreen.class,
        SelectChoiceRewardScreen.class, ValidItemsScreen.class}, remap = false)
public abstract class FtbQuestScreenThemeMixin {
    @Inject(method = "getTheme", at = @At("RETURN"), cancellable = true)
    private void nexus$questTheme(CallbackInfoReturnable<Theme> cir) {
        var theme = QuestGlassUi.theme((BaseScreen) (Object) this);
        if (theme != null) cir.setReturnValue(theme);
    }
}
