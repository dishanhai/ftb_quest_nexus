package com.dishanhai.ftb_quest_nexus.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Author: dishanhai. Chemical reward payloads keep the offered amount on the server. */
class ChemicalRewardPayloadTest {
    private static final UUID DELIVERY = new UUID(1, 2);
    private static final ResourceLocation CHEMICAL = ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen");

    @Test
    void offerPreservesLongMillibucketAmounts() {
        for (boolean canPackage : new boolean[] {false, true}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var offer = new ChemicalRewardPackets.Offer(DELIVERY, CHEMICAL, Long.MAX_VALUE, canPackage);
                ChemicalRewardPackets.Offer.STREAM_CODEC.encode(buffer, offer);
                assertEquals(offer, ChemicalRewardPackets.Offer.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void offerRejectsNonpositiveAmounts() {
        for (long amount : new long[] {0L, -1L}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeUUID(DELIVERY);
                buffer.writeResourceLocation(CHEMICAL);
                buffer.writeVarLong(amount);
                buffer.writeBoolean(true);
                assertThrows(DecoderException.class, () -> ChemicalRewardPackets.Offer.STREAM_CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void selectionTransmitsOnlyTheDeliveryId() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var selection = new ChemicalRewardPackets.Selection(DELIVERY);
            ChemicalRewardPackets.Selection.STREAM_CODEC.encode(buffer, selection);
            assertEquals(2 * Long.BYTES, buffer.readableBytes());
            assertEquals(selection, ChemicalRewardPackets.Selection.STREAM_CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }
}
