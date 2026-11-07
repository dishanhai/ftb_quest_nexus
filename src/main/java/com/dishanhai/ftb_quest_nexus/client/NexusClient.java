package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import com.dishanhai.sh_better_ui.client.blur.GaussBlur;
import com.dishanhai.sh_better_ui.client.glass.GlassTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Author: dishanhai. Only loaded on physical clients; the common entry point does not link screen classes. */
@Mod.EventBusSubscriber(modid = QuestNexus.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NexusClient {
    private NexusClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(NexusContent.MENU.get(), SubmitterScreen::new);
            GaussBlur.allowScreen(screen -> screen instanceof SubmitterScreen || QuestGlassUi.active(screen));
            // Claim the event before Modern UI's HIGH handler draws text into the blur input.
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, NexusClient::tooltip);
        });
    }

    private static void tooltip(RenderTooltipEvent.Pre event) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof SubmitterScreen || QuestGlassUi.active(screen)) GlassTooltip.render(event);
    }
}
