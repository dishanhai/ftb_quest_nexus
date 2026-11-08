package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Author: dishanhai. Client requests may only discover their own team's submitters. */
public record FtbqSubmitterListRequestPacket(long requestId, long taskId, int page) implements CustomPacketPayload {
    public static final Type<FtbqSubmitterListRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_list_request"));
    public static final StreamCodec<FriendlyByteBuf, FtbqSubmitterListRequestPacket> STREAM_CODEC =
            StreamCodec.ofMember(FtbqSubmitterListRequestPacket::encode, FtbqSubmitterListRequestPacket::new);

    private FtbqSubmitterListRequestPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), buffer.readVarInt());
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeLong(taskId);
        buffer.writeVarInt(page);
    }
    @Override public Type<FtbqSubmitterListRequestPacket> type() { return TYPE; }

    public static void handle(FtbqSubmitterListRequestPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        var file = ServerQuestFile.INSTANCE;
        var task = file == null ? null : file.getTask(packet.taskId);
        var entries = SubmitterQuestIndex.supportsQueueTask(task)
                ? SubmitterBlockEntity.listSubmitters(player) : List.<SubmitterBlockEntity.SubmitterEntry>of();
        PacketDistributor.sendToPlayer(player,
                new FtbqSubmitterListResponsePacket(packet.requestId, packet.taskId,
                        SubmitterSelectionPage.of(packet.page, entries)));
    }
}
