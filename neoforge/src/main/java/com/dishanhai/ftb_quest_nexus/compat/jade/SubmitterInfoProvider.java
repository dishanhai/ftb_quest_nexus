package com.dishanhai.ftb_quest_nexus.compat.jade;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class SubmitterInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    public static final SubmitterInfoProvider INSTANCE = new SubmitterInfoProvider();
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter");
    private static final String GUI = "gui.ftb_quest_nexus.submitter.";

    private SubmitterInfoProvider() {}

    @Override
    public ResourceLocation getUid() { return UID; }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SubmitterBlockEntity be)) return;
        CompoundTag state = new CompoundTag();
        state.putBoolean("online", be.isOnline());
        state.putString("status", Component.Serializer.toJson(be.getStatusComponent(), accessor.getLevel().registryAccess()));
        state.putString("id", be.getSubmitterIdText());
        state.putString("team", be.getTeamIdText());
        state.putString("player", be.getRewardPlayerIdText());
        state.putInt("queue", be.getQueueSize());
        state.putString("queueDetail", Component.Serializer.toJson(be.getQueueDetailComponent(), accessor.getLevel().registryAccess()));
        state.putLong("submitted", be.getLastSubmitted());
        state.putLong("rewards", be.getLastClaimedRewards());
        state.putBoolean("suppress", be.isSuppressCompletionToast());
        state.putBoolean("queuedOnly", be.isClaimOnlyQueuedRewards());
        data.put("nexusSubmitter", state);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag root = accessor.getServerData();
        if (!root.contains("nexusSubmitter")) return;
        CompoundTag state = root.getCompound("nexusSubmitter");
        tooltip.add(Component.translatable(GUI + (state.getBoolean("online") ? "online" : "offline")));
        Component status = Component.Serializer.fromJson(state.getString("status"), accessor.getLevel().registryAccess());
        if (status != null) tooltip.add(Component.translatable(GUI + "status", status));
        tooltip.add(Component.translatable(GUI + "uuid", identifier(state.getString("id"))));
        tooltip.add(Component.translatable(GUI + "team", identifier(state.getString("team"))));
        tooltip.add(Component.translatable(GUI + "player", identifier(state.getString("player"))));
        tooltip.add(Component.translatable(GUI + "queue", state.getInt("queue")));
        Component queueDetail = Component.Serializer.fromJson(state.getString("queueDetail"), accessor.getLevel().registryAccess());
        if (queueDetail != null) tooltip.add(Component.translatable(GUI + "queue_detail", queueDetail));
        tooltip.add(Component.translatable(GUI + "submitted", state.getLong("submitted")));
        tooltip.add(Component.translatable(GUI + "rewards", state.getLong("rewards")));
        tooltip.add(Component.translatable(GUI + (state.getBoolean("suppress") ? "toast.suppressed" : "toast.normal")));
        tooltip.add(Component.translatable(GUI + (state.getBoolean("queuedOnly") ? "claim.queued" : "claim.all")));
    }

    private static Component identifier(String text) {
        return text.isBlank() ? Component.translatable(GUI + "unbound") : Component.literal(text);
    }
}
