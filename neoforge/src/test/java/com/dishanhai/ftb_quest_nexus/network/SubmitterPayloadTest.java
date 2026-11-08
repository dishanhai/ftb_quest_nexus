package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
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
            var packet = new FtbqSubmitterListRequestPacket(17L, -1L, 2);
            FtbqSubmitterListRequestPacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, FtbqSubmitterListRequestPacket.STREAM_CODEC.decode(buffer));
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
    void queueSearchPreservesRevisionAndMatchesAcrossMaskWords() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var matches = new java.util.BitSet();
            matches.set(0);
            matches.set(64);
            matches.set(529);
            var packet = new SubmitterSearchPacket(23, Long.MAX_VALUE, true, matches.toLongArray());
            SubmitterSearchPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = SubmitterSearchPacket.STREAM_CODEC.decode(buffer);
            assertEquals(packet.containerId(), decoded.containerId());
            assertEquals(packet.revision(), decoded.revision());
            assertTrue(decoded.filtering());
            assertEquals(matches, java.util.BitSet.valueOf(decoded.mask()));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test
    void listRoundTripPreservesDisplayFieldsAndOwnsAnImmutableSnapshot() {
        var original = new ArrayList<SubmitterBlockEntity.SubmitterEntry>();
        var entry = new SubmitterBlockEntity.SubmitterEntry(UUID.randomUUID(), "minecraft:overworld 1 2 3", 4, true);
        original.add(entry);
        var packet = new FtbqSubmitterListResponsePacket(17, 123, SubmitterSelectionPage.of(0, original));
        original.clear();
        assertEquals(List.of(entry), packet.results().entries());
        assertThrows(UnsupportedOperationException.class, () -> packet.results().entries().clear());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FtbqSubmitterListResponsePacket.STREAM_CODEC.encode(buffer, packet);
            assertEquals(packet, FtbqSubmitterListResponsePacket.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally { buffer.release(); }
    }

    @Test
    void oversizedAndNegativeListCountsFailBeforeEntryAllocation() {
        for (int count : new int[] {-1, SubmitterSelectionPage.PAGE_SIZE + 1, Integer.MAX_VALUE}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeLong(17);
                buffer.writeLong(123);
                buffer.writeVarInt(0);
                buffer.writeVarInt(1);
                buffer.writeVarInt(count);
                assertThrows(DecoderException.class, () -> FtbqSubmitterListResponsePacket.STREAM_CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }

    @Test
    void authorizedListKeepsEntriesAfterTheOld256EntryLimit() {
        var entries = java.util.stream.IntStream.range(0, 300)
                .mapToObj(i -> new SubmitterBlockEntity.SubmitterEntry(new UUID(0, i), "overworld", 0, true))
                .toList();
        var last = SubmitterSelectionPage.of(Integer.MAX_VALUE, entries);
        assertEquals(last.pageCount() - 1, last.page());
        assertTrue(last.entries().contains(entries.getLast()));
        var reconstructed = java.util.stream.IntStream.range(0, last.pageCount())
                .boxed().flatMap(page -> SubmitterSelectionPage.of(page, entries).entries().stream()).toList();
        assertEquals(entries, reconstructed);
        assertEquals(0, SubmitterSelectionPage.of(-1, entries).page());
        assertEquals(1, SubmitterSelectionPage.of(0, List.of()).pageCount());
    }
}
