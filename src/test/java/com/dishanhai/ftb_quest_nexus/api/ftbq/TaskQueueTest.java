package com.dishanhai.ftb_quest_nexus.api.ftbq;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TaskQueueTest {
    @Test void unsignedIdsRoundTripWithoutChangingOrder() {
        TaskQueue queue = new TaskQueue();
        assertTrue(queue.add(-1L));
        assertTrue(queue.add(Long.MIN_VALUE));
        assertTrue(queue.add(1));
        assertEquals("ffffffffffffffff,8000000000000000,1", queue.encode());
        TaskQueue restored = new TaskQueue();
        restored.load(queue.encode());
        assertEquals(List.of(-1L, Long.MIN_VALUE, 1L), restored.list());
    }

    @Test void zeroAndDuplicatesNeverEnterQueue() {
        TaskQueue queue = new TaskQueue();
        assertFalse(queue.add(0));
        assertTrue(queue.add(12));
        assertFalse(queue.add(12));
        assertEquals(1, queue.size());
    }

    @Test void damagedPersistedEntriesAreIgnoredAndDeduplicated() {
        TaskQueue queue = new TaskQueue();
        queue.add(99);
        queue.load("0, 1,garbage,FFFFFFFFFFFFFFFF,1,,10000000000000000");
        assertEquals(List.of(1L, -1L), queue.list());
        assertEquals("1,ffffffffffffffff", queue.encode());
    }

    @Test void snapshotsCannotMutateOrFollowLaterChanges() {
        TaskQueue queue = new TaskQueue();
        queue.add(10);
        List<Long> snapshot = queue.list();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(11L));
        queue.add(12);
        assertEquals(List.of(10L), snapshot);
    }

    @Test void clearRemoveAndEmptyLoadHaveExplicitResults() {
        TaskQueue queue = new TaskQueue();
        queue.add(1);
        assertFalse(queue.remove(2));
        assertTrue(queue.remove(1));
        assertTrue(queue.isEmpty());
        queue.add(3);
        queue.clear();
        assertEquals(0, queue.size());
        queue.add(4);
        queue.load(null);
        assertTrue(queue.isEmpty());
        queue.load("   ");
        assertEquals("", queue.encode());
    }
}
