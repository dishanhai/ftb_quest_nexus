package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import com.dishanhai.ftb_quest_nexus.client.glass.QuestUiHooks;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.screens.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.client.gui.screens.KeyReferenceScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Inherited library theme overrides still obey the FTBQ ownership scope. */
@Mixin(value = {AbstractThreePanelScreen.class, KeyReferenceScreen.class}, remap = false)
public abstract class FtbLibraryScreenGlassMixin {
    @Inject(method = "getTheme", at = @At("HEAD"), cancellable = true)
    private void nexus$theme(CallbackInfoReturnable<Theme> ci) {
        QuestGlassTheme theme = QuestUiHooks.theme((BaseScreen) (Object) this);
        if (theme != null) ci.setReturnValue(theme);
    }
}
