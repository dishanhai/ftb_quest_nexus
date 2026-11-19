package com.dishanhai.ftb_quest_nexus.client.glass;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Validates injection contracts against the actual pinned NeoForge FTBQ jar. */
class QuestGlassMixinContractTest {
    private static final String QUESTS = "dev/ftb/mods/ftbquests/client/gui/";
    private static final String GUI = "(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V";
    private static final String DRAW = "(Lnet/minecraft/client/gui/GuiGraphics;IIII)V";

    @Test void everyOverridingQuestScreenHasItsOwnThemeMethod() throws IOException {
        for (String name : List.of("quests/QuestScreen", "EditRewardTableScreen", "EmergencyItemsScreen",
                "MultilineTextEditorScreen", "RewardNotificationsScreen", "RewardSelectorScreen",
                "SelectChoiceRewardScreen", "quests/ValidItemsScreen")) {
            assertTrue(methods(QUESTS + name).contains("getTheme()Ldev/ftb/mods/ftblibrary/ui/Theme;"), name);
        }
    }

    @Test void librarySuperclassOverridesDeclareTheInjectedThemeMethod() throws IOException {
        for (String name : List.of("AbstractThreePanelScreen", "ButtonListBaseScreen", "KeyReferenceScreen")) {
            assertTrue(methods("dev/ftb/mods/ftblibrary/ui/misc/" + name)
                    .contains("getTheme()Ldev/ftb/mods/ftblibrary/ui/Theme;"), name);
        }
        List<String> base = methods("dev/ftb/mods/ftblibrary/ui/BaseScreen");
        assertTrue(base.contains("initGui()V"));
        assertTrue(base.contains("drawDefaultBackground(Lnet/minecraft/client/gui/GuiGraphics;)Z"));
    }

    @Test void directPanelRedirectsOnlyReplaceStructuralDrawingSites() throws IOException {
        List<String> view = calls(QUESTS + "quests/ViewQuestPanel", "drawBackground", GUI);
        assertEquals(3, count(view, "dev/ftb/mods/ftblibrary/icon/Icon.draw" + DRAW));
        assertEquals(1, count(view, "dev/ftb/mods/ftblibrary/icon/Color4I.draw" + DRAW));
        List<String> chapter = calls(QUESTS + "quests/ChapterPanel", "drawBackground", GUI);
        assertEquals(1, count(chapter, "dev/ftb/mods/ftblibrary/icon/Icon.draw" + DRAW));
    }

    @Test void screenDecorationRedirectsLeaveDraggingAndSelectionInTheOriginalMethod() throws IOException {
        List<String> background = calls(QUESTS + "quests/QuestScreen", "drawBackground", GUI);
        String propertyGet = "dev/ftb/mods/ftbquests/quest/theme/property/ColorProperty.get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;";
        assertEquals(2, count(background, propertyGet));
        assertTrue(background.stream().anyMatch(call -> call.contains("QuestPanel.setScrollX")));
        assertTrue(background.stream().anyMatch(call -> call.contains("QuestPanel.setScrollY")));
        assertTrue(background.stream().anyMatch(call -> call.contains("QuestPanel.isDraggingSelectionBox")));
        assertEquals(1, count(calls(QUESTS + "quests/QuestScreen", "drawForeground", GUI), propertyGet));
    }

    @Test void editorStatusBarHasASeparateBackgroundWithoutReplacingItsText() throws IOException {
        List<String> status = calls(QUESTS + "quests/QuestPanel", "drawStatusBar",
                "(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;Lcom/mojang/blaze3d/vertex/PoseStack;)V");
        assertEquals(2, count(status, "dev/ftb/mods/ftbquests/quest/theme/property/ColorProperty.get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;"));
        assertEquals(4, count(status, "dev/ftb/mods/ftblibrary/ui/Theme.drawString(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/Object;III)I"));
    }

    private static long count(List<String> calls, String value) {
        return calls.stream().filter(value::equals).count();
    }

    private static List<String> methods(String name) throws IOException {
        List<String> methods = new ArrayList<>();
        reader(name).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                methods.add(name + descriptor);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return methods;
    }

    private static List<String> calls(String className, String methodName, String descriptor) throws IOException {
        List<String> calls = new ArrayList<>();
        reader(className).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (!name.equals(methodName) || !desc.equals(descriptor)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                        calls.add(owner + "." + name + desc);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertFalse(calls.isEmpty(), className + "." + methodName);
        return calls;
    }

    private static ClassReader reader(String name) throws IOException {
        try (var stream = QuestGlassMixinContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(stream, name);
            return new ClassReader(stream);
        }
    }
}
