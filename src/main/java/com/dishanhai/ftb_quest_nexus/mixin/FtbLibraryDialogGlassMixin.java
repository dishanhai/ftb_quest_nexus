package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.ui.misc.ButtonListBaseScreen;
import dev.ftb.mods.ftblibrary.ui.misc.KeyReferenceScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Only FTBQ-owned library dialogs receive the overlay. */
@Mixin(value = {AbstractThreePanelScreen.class, ButtonListBaseScreen.class, KeyReferenceScreen.class}, remap = false)
public abstract class FtbLibraryDialogGlassMixin {
    @Inject(method = "getTheme", at = @At("RETURN"), cancellable = true)
    private void nexus$inheritedDialogTheme(CallbackInfoReturnable<Theme> cir) {
        var theme = QuestGlassUi.theme((BaseScreen) (Object) this);
        if (theme != null) cir.setReturnValue(theme);
    }
}
