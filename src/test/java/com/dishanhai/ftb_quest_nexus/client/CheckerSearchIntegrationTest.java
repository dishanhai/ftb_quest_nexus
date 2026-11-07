package com.dishanhai.ftb_quest_nexus.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Author: dishanhai. The checker consumes the shared search API and the complete server catalog. */
class CheckerSearchIntegrationTest {
    @Test
    void searchUsesLibraryCompletionAndCompleteCatalog() throws IOException {
        Set<String> calls = new HashSet<>();
        try (var input = getClass().getClassLoader().getResourceAsStream(
                "com/dishanhai/ftb_quest_nexus/client/CheckerScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                                    String descriptor, boolean isInterface) {
                            calls.add(owner.substring(owner.lastIndexOf('/') + 1) + "." + name);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(calls.containsAll(Set.of("SearchIndex.search", "SearchField.<init>", "SearchField.setScope",
                "SearchField.renderSuggestions", "SearchField.mouseClickedSuggestion",
                "SearchField.mouseScrolledSuggestions", "CheckerMenu.getCatalogRows",
                "CheckerBlockEntity$CheckRow.completed", "CheckerBlockEntity$CheckRow.available",
                "CheckerBlockEntity$CheckRow.fluid", "IngredientFilter.all", "IngredientFilter.items",
                "IngredientFilter.fluids", "IngredientFilter.kinds")), calls.toString());
        assertFalse(calls.contains("CheckerMenu.getPage"), "Search results must be paginated after filtering");
    }
}
