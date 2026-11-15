package com.dishanhai.ftb_quest_nexus.mixin;

import com.dishanhai.ftb_quest_nexus.client.glass.QuestGlassTheme;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.theme.property.ColorProperty;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Author: dishanhai. Only changes the editor status-bar fill, keeping coordinates and its divider. */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class FtbQuestStatusGlassMixin {
    @Redirect(method = "drawStatusBar", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/theme/property/ColorProperty;get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;"))
    private Object nexus$statusBackground(ColorProperty property, QuestObjectBase object,
                                           GuiGraphicsExtractor graphics, Theme theme, Matrix3x2fStack pose) {
        if (theme instanceof QuestGlassTheme glass && property == ThemeProperties.WIDGET_BACKGROUND) {
            return glass.backgroundColor();
        }
        return property.get(object);
    }
}
