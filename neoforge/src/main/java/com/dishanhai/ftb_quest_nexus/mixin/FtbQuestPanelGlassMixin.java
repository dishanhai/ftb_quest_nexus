package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. Replaces structural fills, preserving quest icons and repeatable timers. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class FtbQuestPanelGlassMixin {
    @Redirect(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/icon/Icon;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", ordinal = 0))
    private void nexus$panel(Icon icon, GuiGraphics graphics, int x, int y, int w, int h,
                             GuiGraphics originalGraphics, Theme theme, int originalX, int originalY, int originalW, int originalH) {
        if (theme instanceof QuestGlassTheme glass) glass.panel(graphics, x, y, w, h);
        else icon.draw(graphics, x, y, w, h);
    }

    @Redirect(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/icon/Color4I;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void nexus$divider(Color4I color, GuiGraphics graphics, int x, int y, int w, int h,
                               GuiGraphics originalGraphics, Theme theme, int originalX, int originalY, int originalW, int originalH) {
        (theme instanceof QuestGlassTheme glass ? glass.borderColor() : color).draw(graphics, x, y, w, h);
    }
}
