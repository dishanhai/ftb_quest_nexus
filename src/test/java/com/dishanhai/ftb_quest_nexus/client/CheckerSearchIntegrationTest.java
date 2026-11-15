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
        Set<String> calls = callsFrom("CheckerScreen");
        assertTrue(calls.containsAll(Set.of("SearchIndex.search", "CheckerMenu.getCatalogRows",
                "CheckerBlockEntity$CheckRow.completed", "CheckerBlockEntity$CheckRow.available",
                "CheckerBlockEntity$CheckRow.fluid")), calls.toString());
        boolean extractor = calls.contains("GuiGraphicsExtractor.nextStratum");
        String searchField = extractor ? "MachineSearchField" : "SearchField";
        for (String method : Set.of("<init>", "setScope", "renderSuggestions", "mouseClickedSuggestion",
                "mouseScrolledSuggestions")) {
            assertTrue(calls.contains(searchField + "." + method), calls.toString());
        }
        if (extractor) {
            assertTrue(callsFrom(searchField).contains("SearchIndex.suggest"));
        } else {
            assertTrue(calls.containsAll(Set.of("IngredientFilter.all", "IngredientFilter.items",
                    "IngredientFilter.fluids", "IngredientFilter.kinds")), calls.toString());
        }
        assertFalse(calls.contains("CheckerMenu.getPage"), "Search results must be paginated after filtering");
    }

    private Set<String> callsFrom(String type) throws IOException {
        Set<String> calls = new HashSet<>();
        try (var input = getClass().getResourceAsStream(type + ".class")) {
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
        return calls;
    }
}
