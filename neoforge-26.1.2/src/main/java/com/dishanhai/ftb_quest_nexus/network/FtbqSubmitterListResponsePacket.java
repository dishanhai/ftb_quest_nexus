package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Author: dishanhai. Returns one authorized page to the initiating client screen. */
public record FtbqSubmitterListResponsePacket(long requestId, long taskId, SubmitterSelectionPage results)
        implements CustomPacketPayload {
    public static final Type<FtbqSubmitterListResponsePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_list"));
    public static final StreamCodec<FriendlyByteBuf, FtbqSubmitterListResponsePacket> STREAM_CODEC =
            StreamCodec.ofMember(FtbqSubmitterListResponsePacket::encode, FtbqSubmitterListResponsePacket::new);

    private FtbqSubmitterListResponsePacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), SubmitterSelectionPage.decode(buffer));
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeLong(taskId);
        results.encode(buffer);
    }

    @Override public Type<FtbqSubmitterListResponsePacket> type() { return TYPE; }
}
