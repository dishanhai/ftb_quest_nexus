package com.dishanhai.ftb_quest_nexus.client;

import com.dishanhai.sh_better_ui.client.gui.display.GraphicIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Author: dishanhai. Icons added within SH-Better-UI 1.0.3. */
final class NexusIcons {
    static final GraphicIcon QUEUE = icon("queue");
    static final GraphicIcon REWARD = icon("reward");
    static final GraphicIcon TASK = icon("task");
    static final GraphicIcon NETWORK_ONLINE = icon("status_ready");
    static final GraphicIcon NETWORK_OFFLINE = icon("status_failed");

    private NexusIcons() {}

    static GraphicIcon available(GraphicIcon icon, GraphicIcon fallback) {
        return Minecraft.getInstance().getResourceManager().getResource(icon.texture()).isPresent()
                ? icon : fallback;
    }

    private static GraphicIcon icon(String name) {
        return new GraphicIcon(ResourceLocation.tryParse("sh_better_ui:textures/gui/display/" + name + ".png"));
    }
}
