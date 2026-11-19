package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqSubmitterToastSuppressor;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collection;
import java.util.Collections;

@Mixin(value = TeamData.class, remap = false)
public abstract class FtbTeamDataToastMixin {
    @ModifyArg(method = "markTaskCompleted", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/events/QuestProgressEventData;<init>(Ljava/util/Date;Ldev/ftb/mods/ftbquests/quest/TeamData;Ldev/ftb/mods/ftbquests/quest/QuestObject;Ljava/util/Collection;Ljava/util/Collection;)V"), index = 4)
    private Collection<ServerPlayer> nexus$suppressAutomaticToast(Collection<ServerPlayer> players) {
        return FtbqSubmitterToastSuppressor.isSuppressing() ? Collections.emptyList() : players;
    }
}
