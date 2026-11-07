package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Author: dishanhai. Directional payloads, handled on the main game thread. */
public final class NexusNetwork {
    private NexusNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        registrar.playToServer(FtbqSubmitterListRequestPacket.TYPE, FtbqSubmitterListRequestPacket.STREAM_CODEC,
                FtbqSubmitterListRequestPacket::handle);
        registrar.playToServer(FtbqQueueTaskPacket.TYPE, FtbqQueueTaskPacket.STREAM_CODEC,
                FtbqQueueTaskPacket::handle);
        registrar.playToServer(SubmitterSearchPacket.TYPE, SubmitterSearchPacket.STREAM_CODEC,
                SubmitterSearchPacket::handle);
        registrar.playToClient(FtbqSubmitterListResponsePacket.TYPE, FtbqSubmitterListResponsePacket.STREAM_CODEC,
                NexusNetwork::handleList);
        registrar.playToClient(CheckerStatePacket.TYPE, CheckerStatePacket.STREAM_CODEC,
                NexusNetwork::handleCheckerState);
        registrar.playToClient(SubmitterStatePacket.TYPE, SubmitterStatePacket.STREAM_CODEC,
                NexusNetwork::handleSubmitterState);
    }

    private static void handleList(FtbqSubmitterListResponsePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.open(packet);
    }

    private static void handleCheckerState(CheckerStatePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.updateChecker(packet);
    }

    private static void handleSubmitterState(SubmitterStatePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.updateSubmitter(packet);
    }

    // This class is resolved only when a clientbound payload is received on a physical client.
    private static final class ClientHandler {
        private static void updateSubmitter(SubmitterStatePacket packet) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu menu
                    && menu.containerId == packet.containerId() && menu.submitter != null
                    && menu.submitter.getBlockPos().equals(packet.pos()) && packet.state() != null) {
                menu.readState(packet.state());
            }
        }

        private static void open(FtbqSubmitterListResponsePacket packet) {
            FtbqSubmitterSelectionClient.open(packet.taskId(), packet.entries());
        }

        private static void updateChecker(CheckerStatePacket packet) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof com.dishanhai.ftb_quest_nexus.checker.CheckerMenu menu
                    && menu.containerId == packet.containerId() && menu.checker != null
                    && menu.checker.getBlockPos().equals(packet.pos()) && packet.state() != null) {
                menu.readState(packet.state());
            }
        }
    }
}
