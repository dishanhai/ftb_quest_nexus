package com.dishanhai.ftb_quest_nexus;

import appeng.api.AECapabilities;
import com.dishanhai.ftb_quest_nexus.api.BindingTransfer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalReward;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalRewards;
import com.dishanhai.ftb_quest_nexus.network.FtbqChoiceRewardPackets;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.config.ClientUiConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.server.level.ServerPlayer;

/** Author: dishanhai. Independent FTB Quests extensions for NeoForge. */
@Mod(QuestNexus.MOD_ID)
public final class QuestNexus {
    public static final String MOD_ID = "ftb_quest_nexus";

    public QuestNexus(IEventBus bus, ModContainer container) {
        ChemicalTask.init();
        ChemicalReward.init();
        container.registerConfig(ModConfig.Type.CLIENT, ClientUiConfig.SPEC);
        NexusContent.BLOCKS.register(bus);
        NexusContent.ITEMS.register(bus);
        NexusContent.BLOCK_ENTITIES.register(bus);
        NexusContent.MENUS.register(bus);
        bus.addListener(NexusNetwork::register);
        bus.addListener(QuestNexus::registerCapabilities);
        bus.addListener(QuestNexus::buildCreativeTab);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            BindingTransfer.register(event.getDispatcher());
            ChemicalRewards.register(event.getDispatcher());
            ChoiceRewardNotices.register(event.getDispatcher(), SubmitterBlockEntity::availableChoiceRewards,
                    FtbqChoiceRewardPackets::sendOpen);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ChemicalRewards.remind(player);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            BindingTransfer.clear(event.getServer());
            SubmitterBlockEntity.clearLoadedSubmitters();
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, NexusContent.BLOCK_ENTITY.get(),
                (submitter, context) -> submitter);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, NexusContent.CHECKER_BLOCK_ENTITY.get(),
                (checker, context) -> checker);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NexusContent.CHECKER_BLOCK_ENTITY.get(),
                (checker, side) -> checker.energyInput());
    }

    private static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(NexusContent.ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.accept(NexusContent.CHECKER_ITEM.get(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
