package com.dishanhai.ftb_quest_nexus;

import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Author: dishanhai. Independent FTB Quests extensions for NeoForge. */
@Mod(QuestNexus.MOD_ID)
public final class QuestNexus {
    public static final String MOD_ID = "ftb_quest_nexus";

    public QuestNexus(IEventBus bus) {
        NexusContent.BLOCKS.register(bus);
        NexusContent.ITEMS.register(bus);
        NexusContent.BLOCK_ENTITIES.register(bus);
        NexusContent.MENUS.register(bus);
        bus.addListener(NexusNetwork::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SubmitterBlockEntity.clearLoadedSubmitters());
    }
}
