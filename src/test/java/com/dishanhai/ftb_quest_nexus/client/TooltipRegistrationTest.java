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
        assertEquals(List.of("HIGHEST"), priorities);
        assertEquals(1, registrations.size());
        assertTrue(registrations.getFirst().endsWith("/EventPriority;Ljava/util/function/Consumer;)V"),
                "Use the priority overload that skips already-cancelled tooltip events");
    }
}
