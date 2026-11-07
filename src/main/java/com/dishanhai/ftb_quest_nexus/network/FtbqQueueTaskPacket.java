package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class FtbqQueueTaskPacket {
    private final UUID submitterId;
    private final long taskId;

    public FtbqQueueTaskPacket(UUID submitterId, long taskId) {
        this.submitterId = submitterId;
        this.taskId = taskId;
    }

    public FtbqQueueTaskPacket(FriendlyByteBuf buf) {
        this.submitterId = buf.readUUID();
        this.taskId = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(submitterId);
        buf.writeLong(taskId);
    }

    public static void handle(FtbqQueueTaskPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> applyOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void applyOnServer(FtbqQueueTaskPacket packet, ServerPlayer player) {
        if (player == null || ServerQuestFile.INSTANCE == null) return;
        Task task = ServerQuestFile.INSTANCE.getTask(packet.taskId);
        if (!ConsumableTask.supported(task)) return;
        SubmitterBlockEntity.queueTask(player, packet.submitterId, packet.taskId);
    }
}
