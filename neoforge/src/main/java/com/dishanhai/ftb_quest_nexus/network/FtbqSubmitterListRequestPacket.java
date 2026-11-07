package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Author: dishanhai. Client requests may only discover their own team's submitters. */
public record FtbqSubmitterListRequestPacket(long taskId) implements CustomPacketPayload {
    public static final Type<FtbqSubmitterListRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_list_request"));
    public static final StreamCodec<FriendlyByteBuf, FtbqSubmitterListRequestPacket> STREAM_CODEC =
            StreamCodec.ofMember(FtbqSubmitterListRequestPacket::encode, FtbqSubmitterListRequestPacket::new);

    private FtbqSubmitterListRequestPacket(FriendlyByteBuf buffer) { this(buffer.readLong()); }
    private void encode(FriendlyByteBuf buffer) { buffer.writeLong(taskId); }
    @Override public Type<FtbqSubmitterListRequestPacket> type() { return TYPE; }

    public static void handle(FtbqSubmitterListRequestPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || ServerQuestFile.INSTANCE == null) return;
        Task task = ServerQuestFile.INSTANCE.getTask(packet.taskId);
        if (!ConsumableTask.supported(task)) return;
        PacketDistributor.sendToPlayer(player,
                new FtbqSubmitterListResponsePacket(packet.taskId, SubmitterBlockEntity.listSubmitters(player)));
    }
}
