package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqQuestFileRevision;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Author: dishanhai. Tracks definition edits separately from per-team progress. */
@Mixin(value = BaseQuestFile.class, remap = false)
public abstract class FtbQuestFileRevisionMixin implements FtbqQuestFileRevision {
    @Unique private long nexus$definitionRevision;

    @Override public long nexus$getDefinitionRevision() { return nexus$definitionRevision; }

    // Client translation updates can mark the file dirty without clearing its whole cache.
    @Inject(method = {"clearCachedData", "refreshIDMap", "markDirty"}, at = @At("RETURN"))
    private void nexus$definitionsChanged(CallbackInfo ci) {
        nexus$definitionRevision++;
        if ((Object) this instanceof ServerQuestFile file) SubmitterQuestIndex.invalidate(file);
    }
}
