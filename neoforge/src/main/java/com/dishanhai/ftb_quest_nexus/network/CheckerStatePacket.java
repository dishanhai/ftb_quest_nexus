package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Author: dishanhai. Server-owned state and one bounded catalog chunk for an open checker menu. */
public record CheckerStatePacket(int containerId, BlockPos pos, CompoundTag state) implements CustomPacketPayload {
    public static final Type<CheckerStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "checker_state"));
    public static final StreamCodec<FriendlyByteBuf, CheckerStatePacket> STREAM_CODEC =
            StreamCodec.ofMember(CheckerStatePacket::encode, CheckerStatePacket::new);

    private CheckerStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readNbt());
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeNbt(state);
    }

    @Override public Type<CheckerStatePacket> type() { return TYPE; }
}
