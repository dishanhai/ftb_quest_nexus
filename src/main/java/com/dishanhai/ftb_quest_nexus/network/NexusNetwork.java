package com.dishanhai.ftb_quest_nexus.network;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NexusNetwork {
    private static final String VERSION = "6";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(QuestNexus.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private NexusNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, FtbqSubmitterListRequestPacket.class,
                FtbqSubmitterListRequestPacket::encode, FtbqSubmitterListRequestPacket::new,
                FtbqSubmitterListRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, FtbqSubmitterListResponsePacket.class,
                FtbqSubmitterListResponsePacket::encode, FtbqSubmitterListResponsePacket::new,
                FtbqSubmitterListResponsePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, FtbqQueueTaskPacket.class,
                FtbqQueueTaskPacket::encode, FtbqQueueTaskPacket::new,
                FtbqQueueTaskPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, CheckerStatePacket.class,
                CheckerStatePacket::encode, CheckerStatePacket::new,
                CheckerStatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, SubmitterStatePacket.class,
                SubmitterStatePacket::encode, SubmitterStatePacket::new,
                SubmitterStatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, SubmitterSearchPacket.class,
                SubmitterSearchPacket::encode, SubmitterSearchPacket::new,
                SubmitterSearchPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
