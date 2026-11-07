package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Author: dishanhai. Server-confirmed state for one viewer's six-row queue page. */
public record SubmitterStatePacket(int containerId, BlockPos pos, CompoundTag state) {
    public SubmitterStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readNbt());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeNbt(state);
    }
    public static void handle(SubmitterStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> applyClient(packet));
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static void applyClient(SubmitterStatePacket packet) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof SubmitterMenu menu
                && menu.containerId == packet.containerId && menu.submitter != null
                && menu.submitter.getBlockPos().equals(packet.pos) && packet.state != null) {
            menu.readState(packet.state);
        }
    }
}
