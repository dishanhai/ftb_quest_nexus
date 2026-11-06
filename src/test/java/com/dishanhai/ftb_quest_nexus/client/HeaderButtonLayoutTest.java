package com.dishanhai.ftb_quest_nexus.client;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HeaderButtonLayoutTest {
    @Test
    void leavesQuestIconAndTitleReadable() {
        var result = HeaderButtonLayout.place(200, 16, List.of());
        assertEquals(24, result.buttonX());
        assertEquals(43, result.titleX());
        assertEquals(153, result.titleWidth());
    }

    @Test
    void originalSubmitterAndCrossChapterLinkCanCoexist() {
        var result = HeaderButtonLayout.place(232, 16, List.of(
                new HeaderButtonLayout.Bounds(20, 0, 16, 16),
                new HeaderButtonLayout.Bounds(38, 4, 16, 16),
                new HeaderButtonLayout.Bounds(214, 4, 16, 16),
                new HeaderButtonLayout.Bounds(196, 4, 16, 16),
                new HeaderButtonLayout.Bounds(180, 0, 16, 16)));
        assertEquals(56, result.buttonX());
        assertTrue(result.buttonX() >= 54);
        assertTrue(result.titleX() >= result.buttonX() + 16);
        assertEquals(178, result.titleX() + result.titleWidth());
    }

    @Test
    void taskContentDoesNotMoveHeaderButton() {
        var result = HeaderButtonLayout.place(200, 16, List.of(
                new HeaderButtonLayout.Bounds(80, 24, 18, 18)));
        assertEquals(24, result.buttonX());
    }
}
