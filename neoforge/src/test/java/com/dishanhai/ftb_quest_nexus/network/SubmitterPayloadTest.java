package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Exercise actual 1.21.1 payload codecs and allocation limits. */
class SubmitterPayloadTest {
    @Test
    void registrationRequestPreservesUnsignedTaskBits() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new FtbqSubmitterListRequestPacket(-1L);
            FtbqSubmitterListRequestPacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(-1L, FtbqSubmitterListRequestPacket.STREAM_CODEC.decode(buffer).taskId());
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test
    void queueRequestPreservesSubmitterAndTaskIdentity() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var id = UUID.randomUUID();
            var packet = new FtbqQueueTaskPacket(id, Long.MIN_VALUE);
            FtbqQueueTaskPacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, FtbqQueueTaskPacket.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test
    void listRoundTripPreservesDisplayFieldsAndOwnsAnImmutableSnapshot() {
        var original = new ArrayList<SubmitterBlockEntity.SubmitterEntry>();
        var entry = new SubmitterBlockEntity.SubmitterEntry(UUID.randomUUID(), "minecraft:overworld 1 2 3", 4, true);
        original.add(entry);
        var packet = new FtbqSubmitterListResponsePacket(123, original);
        original.clear();
        assertEquals(List.of(entry), packet.entries());
        assertThrows(UnsupportedOperationException.class, () -> packet.entries().clear());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FtbqSubmitterListResponsePacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, FtbqSubmitterListResponsePacket.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test
    void oversizedAndNegativeListCountsFailBeforeEntryAllocation() {
        for (int count : new int[] {-1, 257, Integer.MAX_VALUE}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeLong(123);
                buffer.writeVarInt(count);
                assertThrows(DecoderException.class, () -> FtbqSubmitterListResponsePacket.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
