package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassUi;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. Keep the quest icon, repeat timer and original modal contents. */
@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class FtbViewQuestGlassMixin {
    @Redirect(method = "drawBackground", at = @At(value = "INVOKE", ordinal = 0,
            target = "Ldev/ftb/mods/ftblibrary/icon/Icon;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void nexus$detailBackground(Icon icon, GuiGraphics g, int x, int y, int w, int h) {
        var theme = QuestGlassUi.theme(((ViewQuestPanel) (Object) this).getGui());
        if (theme == null) icon.draw(g, x, y, w, h);
        else theme.drawContextMenuBackground(g, x, y, w, h);
    }

    @Redirect(method = "drawBackground", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftblibrary/icon/Color4I;draw(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void nexus$detailSeparator(Color4I original, GuiGraphics g, int x, int y, int w, int h) {
        var theme = QuestGlassUi.theme(((ViewQuestPanel) (Object) this).getGui());
        (theme == null ? original : theme.borderColor()).draw(g, x, y, w, h);
    }
}
