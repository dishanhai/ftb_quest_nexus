package com.dishanhai.ftb_quest_nexus.compat.ars;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SourceStackDebitTest {
    @Test void conservesSourceForAnyRemainderAndNeverInventsJars() {
        for (int count = 1; count <= 64; count++) {
            int fill = 21;
            for (int limit = 0; limit <= fill * count + 1; limit++) {
                for (boolean canSplit : new boolean[]{false, true}) {
                    var plan = SourceStackDebit.plan(fill, count, limit, canSplit);
                    int split = plan.splitCount();
                    int unsplit = count - split;
                    int newFill = fill - plan.basePerJar();
                    assertTrue(unsplit > 0 && split >= 0 && newFill >= 0);
                    assertTrue(split == 0 || canSplit && newFill > 0);
                    assertEquals(fill * count, unsplit * newFill + split * (newFill - 1) + plan.removed());
                    int requested = Math.min(limit, fill * count);
                    assertEquals(canSplit ? requested : requested / count * count, plan.removed());
                }
            }
        }
    }

    @Test void excessiveTargetsAndZeroNeverOverflowOrRoundUp() {
        assertEquals(640_000, SourceStackDebit.plan(10_000, 64, Long.MAX_VALUE, true).removed());
        assertEquals(0, SourceStackDebit.plan(10_000, 64, 63, false).removed());
        assertEquals(0, SourceStackDebit.plan(10_000, 0, 100, false).removed());
        assertEquals(0, SourceStackDebit.plan(10_000, 64, -1, false).removed());
    }
}
