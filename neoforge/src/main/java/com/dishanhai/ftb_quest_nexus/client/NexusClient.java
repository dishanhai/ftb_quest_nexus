package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassTooltip;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Author: dishanhai. Client subscribers are isolated from the common entry point. */
@EventBusSubscriber(modid = QuestNexus.MOD_ID, value = Dist.CLIENT)
public final class NexusClient {
    private NexusClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NexusContent.MENU.get(), SubmitterScreen::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            GaussBlur.allowScreen(screen -> screen instanceof SubmitterScreen);
            NeoForge.EVENT_BUS.addListener(NexusClient::tooltip);
        });
    }

    private static void tooltip(RenderTooltipEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof SubmitterScreen) GlassTooltip.render(event);
    }
}
