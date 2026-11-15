package com.dishanhai.ftb_quest_nexus.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. Glass must claim tooltips before Modern UI's HIGH renderer. */
class TooltipRegistrationTest {
    @Test
    void machineSearchUsesOneTooltipPathAndDrawsCompletionAfterBlur() throws IOException {
        for (String screen : List.of("SubmitterScreen", "CheckerScreen")) {
            List<String> renderCalls = new ArrayList<>();
            List<String> tooltipCalls = new ArrayList<>();
            try (var input = getClass().getClassLoader().getResourceAsStream(
                    "com/dishanhai/ftb_quest_nexus/client/" + screen + ".class")) {
                assertNotNull(input);
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        List<String> calls = switch (name) {
                            case "render", "extractRenderState" -> renderCalls;
                            case "setTooltipForNextRenderPass" -> tooltipCalls;
                            default -> null;
                        };
                        if (calls == null) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name,
                                                        String descriptor, boolean isInterface) {
                                calls.add(name);
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            if (renderCalls.contains("nextStratum")) {
                int clear = renderCalls.indexOf("nexus$setDeferredTooltip");
                int foreground = renderCalls.indexOf("nextStratum");
                assertTrue(clear >= 0 && clear < foreground, screen + " must clear the tooltip below completion");
                assertTrue(foreground < renderCalls.lastIndexOf("paint"),
                        screen + " must extract suggestions in a higher stratum");
                assertTrue(renderCalls.contains("setTooltipForNextFrame"), screen + " loses row tooltips");
                continue;
            }
            assertTrue(renderCalls.contains("deferTopLayer"), screen + " draws completion before blur");
            assertFalse(renderCalls.contains("renderSuggestions"), screen + " draws completion inline");
            assertFalse(renderCalls.contains("renderTooltip"), screen + " bypasses tooltip arbitration");
            if (renderCalls.contains("setTooltipForNextFrame")) {
                int clear = renderCalls.indexOf("nexus$setDeferredTooltip");
                assertTrue(clear >= 0 && clear < renderCalls.indexOf("deferTopLayer"),
                        screen + " must clear the queued tooltip before deferring suggestions");
            } else {
                assertTrue(renderCalls.contains("setTooltipForNextRenderPass"), screen + " loses row tooltips");
                assertEquals(List.of("suggestionArea", "isEmpty", "setTooltipForNextRenderPass"), tooltipCalls, screen);
            }
        }
    }

    @Test
    void tooltipListenerClaimsTheEventBeforeOtherRenderers() throws IOException {
        List<String> priorities = new ArrayList<>();
        List<String> registrations = new ArrayList<>();
        try (var input = getClass().getClassLoader().getResourceAsStream(
                "com/dishanhai/ftb_quest_nexus/client/NexusClient.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (owner.endsWith("/EventPriority")) priorities.add(name);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                                    String descriptor, boolean isInterface) {
                            if (owner.endsWith("/IEventBus") && name.equals("addListener")) {
                                registrations.add(descriptor);
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        // The 26.x extractor queues its own tooltips without a mod listener.
        if (getClass().getResource("MachineSearchField.class") != null) {
            assertTrue(priorities.isEmpty());
            assertTrue(registrations.isEmpty());
            return;
        }
        assertEquals(List.of("HIGHEST"), priorities);
        assertEquals(1, registrations.size());
        assertTrue(registrations.getFirst().endsWith("/EventPriority;Ljava/util/function/Consumer;)V"),
                "Use the priority overload that skips already-cancelled tooltip events");
    }
}
