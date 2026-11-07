package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Author: dishanhai. Server-confirmed state for one viewer's six-row queue page. */
public record SubmitterStatePacket(int containerId, BlockPos pos, CompoundTag state) implements CustomPacketPayload {
    public static final Type<SubmitterStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "submitter_state"));
    public static final StreamCodec<FriendlyByteBuf, SubmitterStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(SubmitterStatePacket::encode, SubmitterStatePacket::new);

    private SubmitterStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readNbt());
    }
    private void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeNbt(state);
    }
    @Override public Type<SubmitterStatePacket> type() { return TYPE; }
}
