package com.dishanhai.ftb_quest_nexus.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Author: dishanhai. Clears the tooltip queued under the machine's search suggestions. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsTooltipAccessor {
    @Accessor("deferredTooltip")
    void nexus$setDeferredTooltip(Runnable tooltip);
}
