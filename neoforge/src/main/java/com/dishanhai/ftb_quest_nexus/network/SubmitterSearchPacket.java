package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Author: dishanhai. Compact match positions for one viewer's acknowledged queue. */
public record SubmitterSearchPacket(int containerId, long revision, boolean filtering, long[] mask)
        implements CustomPacketPayload {
    public static final Type<SubmitterSearchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_search"));
    public static final StreamCodec<FriendlyByteBuf, SubmitterSearchPacket> STREAM_CODEC =
            StreamCodec.ofMember(SubmitterSearchPacket::encode, SubmitterSearchPacket::new);

    private SubmitterSearchPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarLong(), buffer.readBoolean(), buffer.readLongArray());
    }
    private void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(filtering);
        buffer.writeLongArray(mask);
    }
    @Override public Type<SubmitterSearchPacket> type() { return TYPE; }
    public static void handle(SubmitterSearchPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof SubmitterMenu menu
                && menu.containerId == packet.containerId) {
            menu.applySearch(player, packet.revision, packet.filtering, packet.mask);
        }
    }
}
