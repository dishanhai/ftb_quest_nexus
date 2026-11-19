package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Author: dishanhai. Some editor operations only mark the quest file dirty. */
@Mixin(value = ServerQuestFile.class, remap = false)
public abstract class FtbServerQuestFileEditMixin {
    @Inject(method = "markDirty", at = @At("RETURN"))
    private void nexus$invalidateSubmitterIndex(CallbackInfo ci) {
        SubmitterQuestIndex.invalidate((ServerQuestFile) (Object) this);
    }
}
