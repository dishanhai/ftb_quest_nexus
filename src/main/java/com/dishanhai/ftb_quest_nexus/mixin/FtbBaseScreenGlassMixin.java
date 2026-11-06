package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Scope is checked before touching any shared-library screen. */
@Mixin(value = BaseScreen.class, remap = false)
public abstract class FtbBaseScreenGlassMixin {
    @Inject(method = "initGui", at = @At("HEAD"))
    private void nexus$snapshotAppearance(CallbackInfo ci) {
        QuestGlassUi.opened((BaseScreen) (Object) this);
    }

    @Inject(method = "getTheme", at = @At("RETURN"), cancellable = true)
    private void nexus$dialogTheme(CallbackInfoReturnable<Theme> cir) {
        var theme = QuestGlassUi.theme((BaseScreen) (Object) this);
        if (theme != null) cir.setReturnValue(theme);
    }

    @Inject(method = "drawDefaultBackground", at = @At("RETURN"), cancellable = true)
    private void nexus$clearBackdrop(GuiGraphics graphics, CallbackInfoReturnable<Boolean> cir) {
        if (QuestGlassUi.theme((BaseScreen) (Object) this) != null) cir.setReturnValue(false);
    }
}
