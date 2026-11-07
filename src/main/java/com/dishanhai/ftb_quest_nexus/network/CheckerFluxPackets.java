package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.checker.CheckerMenu;
import com.dishanhai.ftb_quest_nexus.client.CheckerScreen;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Author: dishanhai. Network selection for the current checker's owner. */
public final class CheckerFluxPackets {
    private static final int MAX_OPTIONS = 512;
    private static final int MAX_NAME = 128;
    private static final int MAX_PASSWORD = 16;

    private CheckerFluxPackets() {}

    private static CheckerMenu menu(ServerPlayer player, int id) {
        return player != null && player.containerMenu instanceof CheckerMenu menu && menu.containerId == id
                && menu.stillValid(player) && menu.checker.canManageFlux(player) ? menu : null;
    }

    public record Option(int id, String name) {}

    public record Request(int containerId) {
        Request(FriendlyByteBuf buffer) { this(buffer.readVarInt()); }
        void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(containerId); }

        static void handle(Request packet, Supplier<NetworkEvent.Context> ctx) {
            var context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                var menu = menu(player, packet.containerId());
                if (menu == null) {
                    player.displayClientMessage(Component.translatable("gui.ftb_quest_nexus.checker.flux.denied"), true);
                    return;
                }
                var checker = menu.checker;
                NexusNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new View(menu.containerId, checker.fluxNetworkId(), checker.fluxOptions(player, MAX_OPTIONS)));
            });
            context.setPacketHandled(true);
        }
    }

    public record Selection(int containerId, int networkId, String password) {
        Selection(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(MAX_PASSWORD));
        }

        void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(networkId);
            buffer.writeUtf(password, MAX_PASSWORD);
        }

        static void handle(Selection packet, Supplier<NetworkEvent.Context> ctx) {
            var context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                var menu = menu(player, packet.containerId());
                boolean applied = menu != null
                        && menu.checker.selectFluxNetwork(player, packet.networkId(), packet.password());
                player.displayClientMessage(Component.translatable("gui.ftb_quest_nexus.checker.flux."
                        + (applied ? packet.networkId() == -1 ? "unlinked" : "linked" : "denied")), true);
            });
            context.setPacketHandled(true);
        }
    }

    public record View(int containerId, int currentNetwork, List<Option> options) {
        View(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readVarInt(), readOptions(buffer));
        }

        void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(currentNetwork);
            buffer.writeVarInt(options.size());
            for (Option option : options) {
                buffer.writeVarInt(option.id());
                buffer.writeUtf(option.name(), MAX_NAME);
            }
        }

        private static List<Option> readOptions(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            if (count < 0 || count > MAX_OPTIONS) throw new DecoderException("Invalid Flux network list size");
            List<Option> options = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                options.add(new Option(buffer.readVarInt(), buffer.readUtf(MAX_NAME)));
            }
            return List.copyOf(options);
        }

        static void handle(View packet, Supplier<NetworkEvent.Context> ctx) {
            var context = ctx.get();
            context.enqueueWork(packet::openClient);
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private void openClient() {
            CheckerScreen.openFluxSelector(this);
        }
    }
}
