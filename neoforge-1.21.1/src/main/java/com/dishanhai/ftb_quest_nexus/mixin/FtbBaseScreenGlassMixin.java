package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import com.dishanhai.ftb_quest_nexus.client.glass.QuestUiHooks;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author: dishanhai. Generic library dialogs are changed only when owned by FTBQ. */
@Mixin(value = BaseScreen.class, remap = false)
public abstract class FtbBaseScreenGlassMixin {
    @Inject(method = "initGui", at = @At("HEAD"))
    private void nexus$opened(CallbackInfo ci) {
        QuestUiHooks.opened((BaseScreen) (Object) this);
    }

    @Inject(method = "getTheme", at = @At("HEAD"), cancellable = true)
    private void nexus$theme(CallbackInfoReturnable<Theme> ci) {
        QuestGlassTheme theme = QuestUiHooks.theme((BaseScreen) (Object) this);
        if (theme != null) ci.setReturnValue(theme);
    }

    @Inject(method = "drawDefaultBackground", at = @At("HEAD"), cancellable = true)
    private void nexus$clearDefaultBackground(GuiGraphics graphics, CallbackInfoReturnable<Boolean> ci) {
        if (QuestUiHooks.theme((BaseScreen) (Object) this) != null) ci.setReturnValue(false);
    }
}
