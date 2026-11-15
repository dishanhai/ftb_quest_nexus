package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalPicker;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalReward;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Author: dishanhai. Client subscribers are isolated from the common entry point. */
@EventBusSubscriber(modid = QuestNexus.MOD_ID, value = Dist.CLIENT)
public final class NexusClient {
    private static final SystemToast.SystemToastId CHEMICAL_UNAVAILABLE_TOAST = new SystemToast.SystemToastId();

    private NexusClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NexusContent.MENU.get(), SubmitterScreen::new);
        event.register(NexusContent.CHECKER_MENU.get(), CheckerScreen::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(QuestNexus.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (ignored, parent) -> new NexusConfigScreen(parent)));
        event.enqueueWork(() -> {
            if (ChemicalTask.isMekanismAvailable()) {
                ChemicalPicker.register();
            } else {
                ChemicalTask.TYPE.setGuiProvider((panel, quest, callback) -> SystemToast.addOrUpdate(
                        Minecraft.getInstance().getToastManager(), CHEMICAL_UNAVAILABLE_TOAST,
                        ChemicalTask.TYPE.getDisplayName(), Component.translatable(ChemicalTask.LANG + ".unavailable")));
                ChemicalReward.TYPE.setGuiProvider((panel, quest, callback) -> SystemToast.addOrUpdate(
                        Minecraft.getInstance().getToastManager(), CHEMICAL_UNAVAILABLE_TOAST,
                        ChemicalReward.TYPE.getDisplayName(), Component.translatable(ChemicalTask.LANG + ".unavailable")));
            }
        });
    }

    @SubscribeEvent
    public static void reloadThemes(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "machine_themes"),
                (ResourceManagerReloadListener) BlockScreenTheme::reload);
    }
}
