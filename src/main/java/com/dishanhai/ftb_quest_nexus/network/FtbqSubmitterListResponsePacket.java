package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
import com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Author: dishanhai. Returns one authorized page to the initiating client screen. */
public record FtbqSubmitterListResponsePacket(long requestId, long taskId, SubmitterSelectionPage results) {
    public FtbqSubmitterListResponsePacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), buffer.readLong(), SubmitterSelectionPage.decode(buffer));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeLong(taskId);
        results.encode(buffer);
    }

    public static void handle(FtbqSubmitterListResponsePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(FtbqSubmitterListResponsePacket packet) {
        FtbqSubmitterSelectionClient.receive(packet);
    }
}
