package com.dishanhai.ftb_quest_nexus.compat.jade;

import net.minecraft.nbt.NbtOps;

import net.minecraft.network.chat.ComponentSerialization;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class SubmitterInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    public static final SubmitterInfoProvider INSTANCE = new SubmitterInfoProvider();
    private static final Identifier UID = Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter");
    private static final String GUI = "gui.ftb_quest_nexus.submitter.";

    private SubmitterInfoProvider() {}

    @Override
    public Identifier getUid() { return UID; }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof SubmitterBlockEntity be)) return;
        CompoundTag state = new CompoundTag();
        state.putBoolean("online", be.isOnline());
        state.store("status", ComponentSerialization.CODEC, accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), be.getStatusComponent());
        state.putString("id", be.getSubmitterIdText());
        state.putString("team", be.getTeamIdText());
        state.putString("player", be.getRewardPlayerIdText());
        state.putInt("queue", be.getQueueSize());
        state.store("queueDetail", ComponentSerialization.CODEC, accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), be.getQueueDetailComponent());
        state.putLong("submitted", be.getLastSubmitted());
        state.putLong("submittedFluid", be.getLastSubmittedFluid());
        if (be.getLastSubmittedChemical() > 0) state.putLong("submittedChemical", be.getLastSubmittedChemical());
        state.putBoolean("matching", be.isMatching());
        state.putLong("rewards", be.getLastClaimedRewards());
        state.putBoolean("suppress", be.isSuppressCompletionToast());
        state.putBoolean("queuedOnly", be.isClaimOnlyQueuedRewards());
        data.put("nexusSubmitter", state);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag root = accessor.getServerData();
        if (!root.contains("nexusSubmitter")) return;
        CompoundTag state = root.getCompoundOrEmpty("nexusSubmitter");
        tooltip.add(Component.translatable(GUI + (state.getBooleanOr("online", false) ? "online" : "offline")));
        Component status = state.read("status", ComponentSerialization.CODEC, accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE)).orElse(null);
        if (status != null) tooltip.add(Component.translatable(GUI + "status", status));
        tooltip.add(Component.translatable(GUI + "uuid", identifier(state.getStringOr("id", ""))));
        tooltip.add(Component.translatable(GUI + "team", identifier(state.getStringOr("team", ""))));
        tooltip.add(Component.translatable(GUI + "player", identifier(state.getStringOr("player", ""))));
        tooltip.add(Component.translatable(GUI + "queue", state.getIntOr("queue", 0)));
        Component queueDetail = state.read("queueDetail", ComponentSerialization.CODEC, accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE)).orElse(null);
        if (queueDetail != null) tooltip.add(Component.translatable(GUI + "queue_detail", queueDetail));
        tooltip.add(Component.translatable(GUI + "submitted", state.getLongOr("submitted", 0L)));
        tooltip.add(Component.translatable(GUI + "submitted_fluid", state.getLongOr("submittedFluid", 0L)));
        if (state.contains("submittedChemical")) {
            tooltip.add(Component.translatable(GUI + "submitted_chemical", state.getLongOr("submittedChemical", 0L)));
        }
        tooltip.add(Component.translatable(GUI + "rewards", state.getLongOr("rewards", 0L)));
        tooltip.add(Component.translatable(GUI + (state.getBooleanOr("suppress", false) ? "toast.suppressed" : "toast.normal")));
        tooltip.add(Component.translatable(GUI + (state.getBooleanOr("matching", false) ? "claim.matched"
                : state.getBooleanOr("queuedOnly", false) ? "claim.queued" : "claim.all")));
    }

    private static Component identifier(String text) {
        return text.isBlank() ? Component.translatable(GUI + "unbound") : Component.literal(text);
    }
}
