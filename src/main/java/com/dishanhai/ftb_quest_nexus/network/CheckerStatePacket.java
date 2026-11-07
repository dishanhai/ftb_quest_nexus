package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.checker.CheckerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Author: dishanhai. Server-owned state and one bounded catalog chunk for an open checker menu. */
public record CheckerStatePacket(int containerId, BlockPos pos, CompoundTag state) {
    public CheckerStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readNbt());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeNbt(state);
    }
    public static void handle(CheckerStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> applyClient(packet));
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static void applyClient(CheckerStatePacket packet) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof CheckerMenu menu
                && menu.containerId == packet.containerId && menu.checker != null
                && menu.checker.getBlockPos().equals(packet.pos) && packet.state != null) {
            menu.readState(packet.state);
        }
    }
}

