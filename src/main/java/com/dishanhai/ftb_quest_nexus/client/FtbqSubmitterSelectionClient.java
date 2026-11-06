package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.network.FtbqQueueTaskPacket;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class FtbqSubmitterSelectionClient {
    private FtbqSubmitterSelectionClient() {}

    public static void open(long taskId, List<SubmitterBlockEntity.SubmitterEntry> entries) {
        BaseScreen screen = ClientUtils.getCurrentGuiAs(BaseScreen.class);
        if (screen == null) return;
        List<ContextMenuItem> menu = new ArrayList<>();
        if (entries.isEmpty()) {
            menu.add(new ContextMenuItem(Component.translatable("gui.ftb_quest_nexus.submitter.none"),
                    Icons.INFO_GRAY, null).setCloseMenu(false));
        } else {
            for (SubmitterBlockEntity.SubmitterEntry entry : entries) {
                Component online = Component.translatable("gui.ftb_quest_nexus.submitter.network."
                        + (entry.online() ? "online" : "offline"));
                Component title = Component.translatable("gui.ftb_quest_nexus.submitter.entry",
                        entry.id().toString().substring(0, 8), entry.pos(), entry.queueSize(), online);
                menu.add(new ContextMenuItem(title, Icons.ADD, button ->
                        NexusNetwork.CHANNEL.sendToServer(new FtbqQueueTaskPacket(entry.id(), taskId))));
            }
        }
        screen.openContextMenu(menu);
    }
}
