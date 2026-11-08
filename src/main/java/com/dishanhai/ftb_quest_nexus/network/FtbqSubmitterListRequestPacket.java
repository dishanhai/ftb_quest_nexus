package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterSelectionPage;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

/** Author: dishanhai. Client requests may only discover their own team's submitters. */
public record FtbqSubmitterListRequestPacket(long requestId, long taskId, int page) {
    public FtbqSubmitterListRequestPacket(FriendlyByteBuf buf) {
        this(buf.readLong(), buf.readLong(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(requestId);
        buf.writeLong(taskId);
        buf.writeVarInt(page);
    }

    public static void handle(FtbqSubmitterListRequestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> applyOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void applyOnServer(FtbqSubmitterListRequestPacket packet, ServerPlayer player) {
        if (player == null) return;
        var file = ServerQuestFile.INSTANCE;
        var task = file == null ? null : file.getTask(packet.taskId);
        var entries = SubmitterQuestIndex.supportsQueueTask(task)
                ? SubmitterBlockEntity.listSubmitters(player) : List.<SubmitterBlockEntity.SubmitterEntry>of();
        NexusNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FtbqSubmitterListResponsePacket(packet.requestId, packet.taskId,
                        SubmitterSelectionPage.of(packet.page, entries))
        );
    }
}
