package com.dishanhai.ftb_quest_nexus.mixin;

import dev.ftb.mods.ftblibrary.client.icon.IconHelper;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. Replaces structural fills, preserving quest icons and repeatable timers. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class FtbQuestPanelGlassMixin {
    @Redirect(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/client/icon/IconHelper;renderIcon(Ldev/ftb/mods/ftblibrary/icon/Icon;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", ordinal = 0))
    private void nexus$panel(Icon<?> icon, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                             GuiGraphicsExtractor originalGraphics, Theme theme, int originalX, int originalY, int originalW, int originalH) {
        if (theme instanceof QuestGlassTheme glass) glass.panel(graphics, x, y, w, h);
        else IconHelper.renderIcon(icon, graphics, x, y, w, h);
    }

    @Redirect(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/client/icon/IconHelper;renderIcon(Ldev/ftb/mods/ftblibrary/icon/Icon;Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", ordinal = 2))
    private void nexus$divider(Icon<?> color, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                               GuiGraphicsExtractor originalGraphics, Theme theme, int originalX, int originalY, int originalW, int originalH) {
        if (theme instanceof QuestGlassTheme glass) IconHelper.renderIcon(glass.borderColor(), graphics, x, y, w, h);
        else IconHelper.renderIcon(color, graphics, x, y, w, h);
    }
}
