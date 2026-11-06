package com.dishanhai.ftb_quest_nexus.api.ftbq;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FtbqSubmitterToastSuppressorTest {
    @Test void suppressesOnlyInsideActionAndRestoresNestedState() {
        assertFalse(FtbqSubmitterToastSuppressor.isSuppressing());
        FtbqSubmitterToastSuppressor.runSuppressed(() -> {
            assertTrue(FtbqSubmitterToastSuppressor.isSuppressing());
            FtbqSubmitterToastSuppressor.runSuppressed(() ->
                    assertTrue(FtbqSubmitterToastSuppressor.isSuppressing()));
            assertTrue(FtbqSubmitterToastSuppressor.isSuppressing());
        });
        assertFalse(FtbqSubmitterToastSuppressor.isSuppressing());
    }

    @Test void exceptionCannotLeakSuppressionToNextTask() {
        assertThrows(IllegalStateException.class, () -> FtbqSubmitterToastSuppressor.runSuppressed(() -> {
            throw new IllegalStateException("expected");
        }));
        assertFalse(FtbqSubmitterToastSuppressor.isSuppressing());
    }

    @Test void suppressionDoesNotEscapeServerThread() throws InterruptedException {
        boolean[] child = {true};
        FtbqSubmitterToastSuppressor.runSuppressed(() -> {
            Thread thread = new Thread(() -> child[0] = FtbqSubmitterToastSuppressor.isSuppressing());
            thread.start();
            try { thread.join(); }
            catch (InterruptedException e) { throw new AssertionError(e); }
        });
        assertFalse(child[0]);
    }
}
