package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalRewards;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Author: dishanhai. An offer describes a pending reward and a choice names its server-side delivery. */
public final class ChemicalRewardPackets {
    private ChemicalRewardPackets() {}

    public record Offer(UUID deliveryId, Identifier chemicalId, long amount,
                        boolean canPackage) implements CustomPacketPayload {
        public static final Type<Offer> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "chemical_reward_offer"));
        public static final StreamCodec<FriendlyByteBuf, Offer> STREAM_CODEC =
                StreamCodec.ofMember(Offer::encode, Offer::new);

        private Offer(FriendlyByteBuf buffer) {
            this(buffer.readUUID(), buffer.readIdentifier(), readAmount(buffer), buffer.readBoolean());
        }

        private static long readAmount(FriendlyByteBuf buffer) {
            long amount = buffer.readVarLong();
            if (amount <= 0) throw new DecoderException("Chemical reward amount must be positive");
            return amount;
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(deliveryId);
            buffer.writeIdentifier(chemicalId);
            buffer.writeVarLong(amount);
            buffer.writeBoolean(canPackage);
        }

        @Override public Type<Offer> type() { return TYPE; }
    }

    public record Selection(UUID deliveryId) implements CustomPacketPayload {
        public static final Type<Selection> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "chemical_reward_selection"));
        public static final StreamCodec<FriendlyByteBuf, Selection> STREAM_CODEC =
                StreamCodec.ofMember(Selection::encode, Selection::new);

        private Selection(FriendlyByteBuf buffer) {
            this(buffer.readUUID());
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(deliveryId);
        }

        @Override public Type<Selection> type() { return TYPE; }

        static void handle(Selection packet, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                ChemicalRewards.packagePending(player, packet.deliveryId());
            }
        }
    }
}
