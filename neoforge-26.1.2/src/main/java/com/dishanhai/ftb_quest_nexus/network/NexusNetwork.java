package com.dishanhai.ftb_quest_nexus.network;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.dishanhai.ftb_quest_nexus.client.ChemicalRewardWarning;
import com.dishanhai.ftb_quest_nexus.client.FtbqChoiceRewardScreen;
import com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;

/** Author: dishanhai. Directional payloads, handled on the main game thread. */
public final class NexusNetwork {
    private NexusNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("10");
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
        registrar.playToClient(FtbqChoiceRewardPackets.View.TYPE, FtbqChoiceRewardPackets.View.STREAM_CODEC,
                NexusNetwork::handleChoiceView);
        registrar.playToServer(FtbqChoiceRewardPackets.Selection.TYPE, FtbqChoiceRewardPackets.Selection.STREAM_CODEC,
                FtbqChoiceRewardPackets.Selection::handle);
        registrar.playToClient(ChemicalRewardPackets.Offer.TYPE, ChemicalRewardPackets.Offer.STREAM_CODEC,
                NexusNetwork::handleChemicalOffer);
        registrar.playToServer(ChemicalRewardPackets.Selection.TYPE, ChemicalRewardPackets.Selection.STREAM_CODEC,
                ChemicalRewardPackets.Selection::handle);
    }

    private static void handleList(FtbqSubmitterListResponsePacket packet, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientHandler.open(packet);
    }

    private static void handleCheckerState(CheckerStatePacket packet, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientHandler.updateChecker(packet);
    }

    private static void handleSubmitterState(SubmitterStatePacket packet, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientHandler.updateSubmitter(packet);
    }

    private static void handleChoiceView(FtbqChoiceRewardPackets.View packet, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientHandler.choiceView(packet);
    }

    private static void handleChemicalOffer(ChemicalRewardPackets.Offer packet, IPayloadContext context) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientHandler.chemicalOffer(packet);
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
            FtbqSubmitterSelectionClient.receive(packet);
        }

        private static void choiceView(FtbqChoiceRewardPackets.View packet) {
            var batch = packet.batch();
            if (packet.refresh()) {
                FtbqChoiceRewardScreen.update(batch.token(), batch.rewardIds());
            } else {
                FtbqChoiceRewardScreen.open(batch.token(), batch.rewardIds(), ClientHandler::sendChoice);
            }
        }

        private static void sendChoice(UUID token, Map<Long, Integer> selections) {
            ClientPacketDistributor.sendToServer(new FtbqChoiceRewardPackets.Selection(token, selections));
        }

        private static void chemicalOffer(ChemicalRewardPackets.Offer packet) {
            ChemicalRewardWarning.open(packet);
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
