package com.dishanhai.ftb_quest_nexus.client.glass;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Read pinned jar bytes without initializing client/Minecraft classes. */
class ForgeQuestGlassMixinContractTest {
    private static final String GUI = "dev/ftb/mods/ftbquests/client/gui/";
    private static final String DRAW_BG = "(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V";
    private static final String DRAW = "(Lnet/minecraft/client/gui/GuiGraphics;IIII)V";
    private static final String COLOR_GET = "dev/ftb/mods/ftbquests/quest/theme/property/ColorProperty.get(Ldev/ftb/mods/ftbquests/quest/QuestObjectBase;)Ljava/lang/Object;";

    @Test void overriddenThemeMethodsExistInAllSevenTargets() throws IOException {
        for (String target : List.of("quests/QuestScreen", "EditRewardTableScreen", "EmergencyItemsScreen",
                "MultilineTextEditorScreen", "RewardNotificationsScreen", "SelectChoiceRewardScreen", "quests/ValidItemsScreen")) {
            assertTrue(methods(GUI + target).contains("getTheme()Ldev/ftb/mods/ftblibrary/ui/Theme;"), target);
        }
    }

    @Test void detailRedirectsRetainTheQuestIconAndRepeatTimer() throws IOException {
        List<String> calls = calls(GUI + "quests/ViewQuestPanel", "drawBackground", DRAW_BG);
        assertEquals(3, count(calls, "dev/ftb/mods/ftblibrary/icon/Icon.draw" + DRAW));
        assertEquals(1, count(calls, "dev/ftb/mods/ftblibrary/icon/Color4I.draw" + DRAW));
        assertTrue(calls.stream().anyMatch(call -> call.contains("TeamData.getMilliSecondsUntilRepeatable")));
    }

    @Test void libraryOverridesAndEditorStatusBarHaveTheirOwnTargets() throws IOException {
        for (String target : List.of("AbstractThreePanelScreen", "ButtonListBaseScreen", "KeyReferenceScreen")) {
            assertTrue(methods("dev/ftb/mods/ftblibrary/ui/misc/" + target)
                    .contains("getTheme()Ldev/ftb/mods/ftblibrary/ui/Theme;"), target);
        }
        List<String> calls = calls(GUI + "quests/QuestPanel", "drawStatusBar",
                "(Lnet/minecraft/client/gui/GuiGraphics;Ldev/ftb/mods/ftblibrary/ui/Theme;Lcom/mojang/blaze3d/vertex/PoseStack;)V");
        assertEquals(1, count(calls, "dev/ftb/mods/ftbquests/quest/theme/property/ColorProperty.get()Ljava/lang/Object;"));
    }

    @Test void structuralRedirectsDoNotReplaceDragOrSelectionLogic() throws IOException {
        List<String> calls = calls(GUI + "quests/QuestScreen", "drawBackground", DRAW_BG);
        assertEquals(2, count(calls, COLOR_GET));
        assertTrue(calls.stream().anyMatch(call -> call.contains("QuestPanel.setScrollX")));
        assertTrue(calls.stream().anyMatch(call -> call.contains("QuestPanel.setScrollY")));
        assertTrue(calls.stream().anyMatch(call -> call.contains("GuiHelper.drawHollowRect")));
        assertEquals(1, count(calls(GUI + "quests/QuestScreen", "drawForeground", DRAW_BG), COLOR_GET));
    }

    private static long count(List<String> calls, String name) {
        return calls.stream().filter(name::equals).count();
    }

    private static List<String> methods(String name) throws IOException {
        List<String> methods = new ArrayList<>();
        reader(name).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                methods.add(name + desc);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return methods;
    }

    private static List<String> calls(String className, String method, String descriptor) throws IOException {
        List<String> calls = new ArrayList<>();
        reader(className).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                if (!name.equals(method) || !desc.equals(descriptor)) return null;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        calls.add(owner + "." + name + desc);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertFalse(calls.isEmpty(), className + "." + method);
        return calls;
    }

    private static ClassReader reader(String name) throws IOException {
        String file = name.startsWith("dev/ftb/mods/ftblibrary/")
                ? "libs/ftb-library-forge-2001.2.13.jar" : "libs/ftb-quests-forge-2001.4.22.jar";
        try (ZipFile jar = new ZipFile(file)) {
            var entry = jar.getEntry(name + ".class");
            assertNotNull(entry, name);
            try (var stream = jar.getInputStream(entry)) { return new ClassReader(stream); }
        }
    }
}
