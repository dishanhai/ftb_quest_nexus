package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices.Batch;
import com.dishanhai.ftb_quest_nexus.client.FtbqChoiceRewardScreen;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Author: dishanhai. Private choice screen and bounded, server-validated selection. */
public final class FtbqChoiceRewardPackets {
    private FtbqChoiceRewardPackets() {}

    public static void sendOpen(ServerPlayer player, Batch batch) {
        sendView(player, batch, false);
    }

    private static void sendView(ServerPlayer player, Batch batch, boolean refresh) {
        NexusNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new View(batch, refresh));
    }

    public record View(Batch batch, boolean refresh) {
        View(FriendlyByteBuf buffer) {
            this(new Batch(buffer.readUUID(), decodeRewardIds(buffer)), buffer.readBoolean());
        }

        void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(batch.token());
            buffer.writeVarInt(batch.rewardIds().size());
            batch.rewardIds().forEach(buffer::writeLong);
            buffer.writeBoolean(refresh);
        }

        static void handle(View packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            if (context.getDirection().getReceptionSide().isClient()) {
                context.enqueueWork(() -> packet.openClient());
            }
            context.setPacketHandled(true);
        }

        @OnlyIn(Dist.CLIENT)
        private void openClient() {
            if (refresh) {
                FtbqChoiceRewardScreen.update(batch.token(), batch.rewardIds());
            } else {
                FtbqChoiceRewardScreen.open(batch.token(), batch.rewardIds(), View::sendSelection);
            }
        }

        @OnlyIn(Dist.CLIENT)
        private static void sendSelection(UUID token, Map<Long, Integer> selections) {
            NexusNetwork.CHANNEL.sendToServer(new Selection(token, selections));
        }
    }

    public record Selection(UUID token, Map<Long, Integer> selections) {
        public Selection {
            selections = Collections.unmodifiableMap(new LinkedHashMap<>(selections));
        }

        Selection(FriendlyByteBuf buffer) {
            this(buffer.readUUID(), decodeSelections(buffer));
        }

        void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(token);
            buffer.writeVarInt(selections.size());
            selections.forEach((id, index) -> {
                buffer.writeLong(id);
                buffer.writeVarInt(index);
            });
        }

        static void handle(Selection packet, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Batch batch = ChoiceRewardNotices.select(player, packet.token(), packet.selections(),
                        SubmitterBlockEntity::availableChoiceRewards, (recipient, data, reward, index) -> {
                            var selected = reward.getTable().getWeightedRewards().get(index).getReward();
                            // Forge FTBQ's native choice packet delivers before recording the claim.
                            if (data.markRewardAsClaimed(recipient.getUUID(), reward, System.currentTimeMillis())) {
                                selected.claim(recipient, true);
                            }
                        });
                if (batch != null) sendView(player, batch, true);
            });
            context.setPacketHandled(true);
        }
    }

    private static List<Long> decodeRewardIds(FriendlyByteBuf buffer) {
        int size = readBatchSize(buffer);
        List<Long> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(buffer.readLong());
        return List.copyOf(ids);
    }

    private static Map<Long, Integer> decodeSelections(FriendlyByteBuf buffer) {
        int size = readBatchSize(buffer);
        Map<Long, Integer> selections = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            long id = buffer.readLong();
            int index = buffer.readVarInt();
            if (index < 0 || selections.putIfAbsent(id, index) != null) {
                throw new DecoderException("Invalid choice reward selection");
            }
        }
        return selections;
    }

    private static int readBatchSize(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > ChoiceRewardNotices.MAX_BATCH) {
            throw new DecoderException("Invalid choice reward batch size: " + size);
        }
        return size;
    }
}
