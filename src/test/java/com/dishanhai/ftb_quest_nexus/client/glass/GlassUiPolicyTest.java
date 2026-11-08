package com.dishanhai.ftb_quest_nexus.client.glass;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Pure material and scope rules shared by both platforms. */
class GlassUiPolicyTest {
    @Test void opacityPreservesRgbAndScalesExistingAlpha() {
        assertEquals(0x59123456, GlassUiPolicy.scaleAlpha(0x59123456, 1));
        assertEquals(0x2D123456, GlassUiPolicy.scaleAlpha(0x59123456, 0.5));
        assertEquals(0x00123456, GlassUiPolicy.scaleAlpha(0x59123456, 0));
    }

    @Test void opacityClampsAndTreatsInvalidValuesAsDefault() {
        assertEquals(0x59123456, GlassUiPolicy.scaleAlpha(0x59123456, Double.NaN));
        assertEquals(0x59123456, GlassUiPolicy.scaleAlpha(0x59123456, Double.POSITIVE_INFINITY));
        assertEquals(0x59123456, GlassUiPolicy.scaleAlpha(0x59123456, 2));
        assertEquals(0x00123456, GlassUiPolicy.scaleAlpha(0x59123456, -1));
    }

    @Test void onlyQuestGuiAndLibraryDialogsBelongToScope() {
        assertTrue(GlassUiPolicy.isQuestScreenClass("dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen"));
        assertTrue(GlassUiPolicy.isQuestScreenClass("dev.ftb.mods.ftbquests.client.gui.SelectChoiceRewardScreen"));
        assertFalse(GlassUiPolicy.isQuestScreenClass("dev.ftb.mods.ftbquests.client.gui_other.Screen"));
        assertFalse(GlassUiPolicy.isQuestScreenClass("dev.ftb.mods.ftblibrary.ui.BaseScreen"));
        assertTrue(GlassUiPolicy.isLibraryScreenClass("dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay"));
        assertFalse(GlassUiPolicy.isLibraryScreenClass("other.mod.Screen"));
        assertFalse(GlassUiPolicy.isLibraryScreenClass(null));
    }

    @Test void nexusChildPickersInheritThemeOnlyThroughAnOwningParent() {
        for (String type : new String[] {
                "com.dishanhai.ftb_quest_nexus.client.FtbqSubmitterSelectionClient",
                "com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalPicker"}) {
            assertTrue(GlassUiPolicy.isLibraryScreenClass(type));
            assertFalse(GlassUiPolicy.isQuestScreenClass(type));
        }
    }
}
