package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Author: dishanhai. Compact match positions for one viewer's acknowledged queue. */
public record SubmitterSearchPacket(int containerId, long revision, boolean filtering, long[] mask) {
    public SubmitterSearchPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarLong(), buffer.readBoolean(), buffer.readLongArray());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(revision);
        buffer.writeBoolean(filtering);
        buffer.writeLongArray(mask);
    }
    public static void handle(SubmitterSearchPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.containerMenu instanceof SubmitterMenu menu
                    && menu.containerId == packet.containerId) {
                menu.applySearch(player, packet.revision, packet.filtering, packet.mask);
            }
        });
        context.setPacketHandled(true);
    }
}
