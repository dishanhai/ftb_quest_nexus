package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices.Batch;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import dev.ftb.mods.ftbquests.net.ClaimChoiceRewardMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Author: dishanhai. Private choice screen and bounded, server-validated selection. */
public final class FtbqChoiceRewardPackets {
    private FtbqChoiceRewardPackets() {}

    public static void sendOpen(ServerPlayer player, Batch batch) {
        PacketDistributor.sendToPlayer(player, new View(batch, false));
    }

    public record View(Batch batch, boolean refresh) implements CustomPacketPayload {
        public static final Type<View> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "choice_reward_view"));
        public static final StreamCodec<FriendlyByteBuf, View> STREAM_CODEC =
                StreamCodec.ofMember(View::encode, View::new);

        private View(FriendlyByteBuf buffer) {
            this(new Batch(buffer.readUUID(), decodeRewardIds(buffer)), buffer.readBoolean());
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(batch.token());
            buffer.writeVarInt(batch.rewardIds().size());
            batch.rewardIds().forEach(buffer::writeLong);
            buffer.writeBoolean(refresh);
        }

        @Override public Type<View> type() { return TYPE; }
    }

    public record Selection(UUID token, Map<Long, Integer> selections) implements CustomPacketPayload {
        public static final Type<Selection> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "choice_reward_selection"));
        public static final StreamCodec<FriendlyByteBuf, Selection> STREAM_CODEC =
                StreamCodec.ofMember(Selection::encode, Selection::new);

        public Selection {
            selections = Collections.unmodifiableMap(new LinkedHashMap<>(selections));
        }

        private Selection(FriendlyByteBuf buffer) {
            this(buffer.readUUID(), decodeSelections(buffer));
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(token);
            buffer.writeVarInt(selections.size());
            selections.forEach((id, index) -> {
                buffer.writeLong(id);
                buffer.writeVarInt(index);
            });
        }

        @Override public Type<Selection> type() { return TYPE; }

        static void handle(Selection packet, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var nativeContext = new FtbqChoiceContext(player);
            Batch batch = ChoiceRewardNotices.select(player, packet.token(), packet.selections(),
                    SubmitterBlockEntity::availableChoiceRewards, (recipient, data, reward, index) ->
                            ClaimChoiceRewardMessage.handle(new ClaimChoiceRewardMessage(reward.id, index), nativeContext));
            if (batch != null) PacketDistributor.sendToPlayer(player, new View(batch, true));
        }
    }

    // Nexus handles selections on the main thread. Run FTBQ's queued claim before refreshing the batch.
    private record FtbqChoiceContext(ServerPlayer player) implements NetworkManager.PacketContext {
        @Override public ServerPlayer getPlayer() { return player; }
        @Override public void queue(Runnable work) { work.run(); }
        @Override public Env getEnvironment() { return Env.SERVER; }
        @Override public RegistryAccess registryAccess() { return player.registryAccess(); }
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
