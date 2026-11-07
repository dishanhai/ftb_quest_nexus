package com.dishanhai.ftb_quest_nexus.mixin;

// Author: dishanhai. Only defer vanilla auto-claims already covered by a submitter's reward pass.
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqAutoClaimDeferral;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TeamData.class, remap = false)
public abstract class FtbTeamDataAutoClaimMixin {
    @Redirect(method = "checkAutoCompletion", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/TeamData;claimReward(Lnet/minecraft/server/level/ServerPlayer;Ldev/ftb/mods/ftbquests/quest/reward/Reward;Z)V"))
    private void nexus$deferItemClaim(TeamData data, ServerPlayer player, Reward reward, boolean notify) {
        if (!FtbqAutoClaimDeferral.shouldDefer(data, reward)) {
            data.claimReward(player, reward, notify);
        }
    }
}
