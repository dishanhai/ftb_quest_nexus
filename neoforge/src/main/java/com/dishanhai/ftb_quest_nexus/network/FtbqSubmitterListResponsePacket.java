package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Author: dishanhai. Bound list length and string size before decoding entry data. */
public record FtbqSubmitterListResponsePacket(long taskId,
        List<SubmitterBlockEntity.SubmitterEntry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;
    public static final Type<FtbqSubmitterListResponsePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_list"));
    public static final StreamCodec<FriendlyByteBuf, FtbqSubmitterListResponsePacket> STREAM_CODEC =
            StreamCodec.ofMember(FtbqSubmitterListResponsePacket::encode, FtbqSubmitterListResponsePacket::new);

    public FtbqSubmitterListResponsePacket {
        entries = List.copyOf(entries.subList(0, Math.min(entries.size(), MAX_ENTRIES)));
    }

    private FtbqSubmitterListResponsePacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), decodeEntries(buffer));
    }

    private static List<SubmitterBlockEntity.SubmitterEntry> decodeEntries(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new DecoderException("Invalid submitter list size: " + size);
        List<SubmitterBlockEntity.SubmitterEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            var id = buffer.readUUID();
            var pos = buffer.readUtf(128);
            int queued = buffer.readVarInt();
            if (queued < 0) throw new DecoderException("Invalid submitter queue size: " + queued);
            entries.add(new SubmitterBlockEntity.SubmitterEntry(id, pos, queued, buffer.readBoolean()));
        }
        return entries;
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(taskId);
        buffer.writeVarInt(entries.size());
        for (var entry : entries) {
            buffer.writeUUID(entry.id());
            buffer.writeUtf(entry.pos(), 128);
            buffer.writeVarInt(entry.queueSize());
            buffer.writeBoolean(entry.online());
        }
    }

    @Override public Type<FtbqSubmitterListResponsePacket> type() { return TYPE; }
}
