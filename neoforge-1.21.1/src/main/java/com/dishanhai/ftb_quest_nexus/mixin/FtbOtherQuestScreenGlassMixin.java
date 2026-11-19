package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import com.dishanhai.ftb_quest_nexus.client.glass.QuestUiHooks;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.EditRewardTableScreen;
import dev.ftb.mods.ftbquests.client.gui.EmergencyItemsScreen;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import dev.ftb.mods.ftbquests.client.gui.RewardNotificationsScreen;
import dev.ftb.mods.ftbquests.client.gui.RewardSelectorScreen;
import dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.ValidItemsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Covers FTBQ screens which override BaseScreen.getTheme themselves. */
@Mixin(value = {EditRewardTableScreen.class, EmergencyItemsScreen.class, MultilineTextEditorScreen.class,
        RewardNotificationsScreen.class, RewardSelectorScreen.class, SelectChoiceRewardScreen.class,
        ValidItemsScreen.class}, remap = false)
public abstract class FtbOtherQuestScreenGlassMixin {
    @Inject(method = "getTheme", at = @At("HEAD"), cancellable = true)
    private void nexus$theme(CallbackInfoReturnable<Theme> ci) {
        QuestGlassTheme theme = QuestUiHooks.theme((BaseScreen) (Object) this);
        if (theme != null) ci.setReturnValue(theme);
    }
}
