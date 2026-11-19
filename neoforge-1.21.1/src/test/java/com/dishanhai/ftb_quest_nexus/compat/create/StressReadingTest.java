package com.dishanhai.ftb_quest_nexus.compat.create;

import org.junit.jupiter.api.Test;

import static com.dishanhai.ftb_quest_nexus.compat.create.StressReading.Metric.*;
import static org.junit.jupiter.api.Assertions.*;

class StressReadingTest {
    @Test void distinguishCapacityLoadAndHeadroomWithoutRoundingUp() {
        var reading = StressReading.active(-32, 256, 128.25, true, false, false);
        assertNotNull(reading);
        assertTrue(reading.reaches(CAPACITY, 256));
        assertFalse(reading.reaches(CAPACITY, 257));
        assertTrue(reading.reaches(LOAD, 128));
        assertFalse(reading.reaches(HEADROOM, 128));
        assertTrue(reading.reaches(HEADROOM, 127));
    }

    @Test void rejectsInactiveTransientAndMalformedSamples() {
        assertNull(StressReading.active(0, 256, 1, true, false, false));
        assertNull(StressReading.active(32, 256, 1, false, false, false));
        assertNull(StressReading.active(32, 256, 1, true, true, false));
        assertNull(StressReading.active(32, 256, 1, true, false, true));
        assertNull(StressReading.active(32, 256, 257, true, false, false));
        assertNull(StressReading.active(32, 0, 0, true, false, false));
        assertNull(StressReading.active(32, Double.POSITIVE_INFINITY, 0, true, false, false));
        assertNull(StressReading.active(32, 256, Double.NaN, true, false, false));
        assertNull(StressReading.active(32, 256, -1, true, false, false));
    }

    @Test void zeroLoadAndFullyLoadedAreActiveButDoNotPassPositiveTargetsForThatMetric() {
        var unloaded = StressReading.active(32, 256, 0, true, false, false);
        var full = StressReading.active(32, 256, 256, true, false, false);
        assertFalse(unloaded.reaches(LOAD, 1));
        assertFalse(full.reaches(HEADROOM, 1));
    }
}
