package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Author: dishanhai. Queue mutation revalidates bound team and the actual server task. */
public record FtbqQueueTaskPacket(UUID submitterId, long taskId) implements CustomPacketPayload {
    public static final Type<FtbqQueueTaskPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "queue_task"));
    public static final StreamCodec<FriendlyByteBuf, FtbqQueueTaskPacket> STREAM_CODEC =
            StreamCodec.ofMember(FtbqQueueTaskPacket::encode, FtbqQueueTaskPacket::new);

    private FtbqQueueTaskPacket(FriendlyByteBuf buffer) { this(buffer.readUUID(), buffer.readLong()); }
    private void encode(FriendlyByteBuf buffer) { buffer.writeUUID(submitterId); buffer.writeLong(taskId); }
    @Override public Type<FtbqQueueTaskPacket> type() { return TYPE; }

    public static void handle(FtbqQueueTaskPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        SubmitterBlockEntity.queueTask(player, packet.submitterId, packet.taskId);
    }
}
