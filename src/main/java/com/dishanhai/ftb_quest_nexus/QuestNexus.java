package com.dishanhai.ftb_quest_nexus;

import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;

/** Author: dishanhai. Independent FTB Quests extensions. */
@Mod(QuestNexus.MOD_ID)
public final class QuestNexus {
    public static final String MOD_ID = "ftb_quest_nexus";

    public QuestNexus() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientUiConfig.SPEC);
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        NexusContent.BLOCKS.register(bus);
        NexusContent.ITEMS.register(bus);
        NexusContent.BLOCK_ENTITIES.register(bus);
        NexusContent.MENUS.register(bus);
        NexusNetwork.init();
        bus.addListener(QuestNexus::buildCreativeTab);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SubmitterBlockEntity.clearLoadedSubmitters());
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(NexusContent.ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(NexusContent.CHECKER_ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
