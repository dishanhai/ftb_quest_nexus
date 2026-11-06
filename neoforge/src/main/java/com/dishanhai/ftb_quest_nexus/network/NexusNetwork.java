package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Author: dishanhai. Three directional payloads, handled on the main game thread. */
public final class NexusNetwork {
    private NexusNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(FtbqSubmitterListRequestPacket.TYPE, FtbqSubmitterListRequestPacket.STREAM_CODEC,
                FtbqSubmitterListRequestPacket::handle);
        registrar.playToServer(FtbqQueueTaskPacket.TYPE, FtbqQueueTaskPacket.STREAM_CODEC,
                FtbqQueueTaskPacket::handle);
        registrar.playToClient(FtbqSubmitterListResponsePacket.TYPE, FtbqSubmitterListResponsePacket.STREAM_CODEC,
                NexusNetwork::handleList);
    }

    private static void handleList(FtbqSubmitterListResponsePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.open(packet);
    }

    // This class is resolved only when a clientbound payload is received on a physical client.
    private static final class ClientHandler {
        private static void open(FtbqSubmitterListResponsePacket packet) {
            FtbqSubmitterSelectionClient.open(packet.taskId(), packet.entries());
        }
    }
}
