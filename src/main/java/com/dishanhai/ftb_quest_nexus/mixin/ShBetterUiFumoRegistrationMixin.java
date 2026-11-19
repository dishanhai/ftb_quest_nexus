// Author: dishanhai
package com.dishanhai.ftb_quest_nexus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nexus owns the fallback when it is installed with SH-Better-UI. */
@Pseudo
@Mixin(targets = "com.dishanhai.sh_better_ui.internal.fumo.FumoRegistration", remap = false)
abstract class ShBetterUiFumoRegistrationMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nexus$skipSharedUiFallback(CallbackInfo callback) {
        callback.cancel();
    }
}
