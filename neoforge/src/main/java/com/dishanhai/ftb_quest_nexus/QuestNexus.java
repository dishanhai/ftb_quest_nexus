package com.dishanhai.ftb_quest_nexus;

import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;

/** Author: dishanhai. Independent FTB Quests extensions for NeoForge. */
@Mod(QuestNexus.MOD_ID)
public final class QuestNexus {
    public static final String MOD_ID = "ftb_quest_nexus";

    public QuestNexus(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientUiConfig.SPEC);
        NexusContent.BLOCKS.register(bus);
        NexusContent.ITEMS.register(bus);
        NexusContent.BLOCK_ENTITIES.register(bus);
        NexusContent.MENUS.register(bus);
        bus.addListener(NexusNetwork::register);
        bus.addListener(QuestNexus::buildCreativeTab);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SubmitterBlockEntity.clearLoadedSubmitters());
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(NexusContent.ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(NexusContent.CHECKER_ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
