package com.dishanhai.ftb_quest_nexus.compat.pneumatic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirStorageTest {
    @Test void countsOnlyVerifiedDebitWithoutIntSaturation() {
        assertEquals(1500, AirStorage.acceptedDebit(8192, 6692, 1500));
        assertEquals(800, AirStorage.acceptedDebit(8192, 7392, 1500));
        assertEquals(250, AirStorage.acceptedDebit(5_000_000_000L, 4_999_999_750L, 250));
        assertEquals(0, AirStorage.acceptedDebit(8192, 8192, 1500));
        assertEquals(0, AirStorage.acceptedDebit(8192, 100, 1500));
        assertEquals(0, AirStorage.acceptedDebit(8192, -1, 1500));
        assertEquals(0, AirStorage.acceptedDebit(Long.MIN_VALUE, 50, 1500));
    }

    @Test void onlyDedicatedContainersAreCounted() {
        assertTrue(AirStorage.dedicatedContainer("pneumaticcraft", "air_canister"));
        assertTrue(AirStorage.dedicatedContainer("pneumaticcraft", "reinforced_air_canister_array"));
        assertTrue(AirStorage.dedicatedContainer("appliedpneumatics", "portable_air_cell_256m"));
        assertFalse(AirStorage.dedicatedContainer("pneumaticcraft", "pneumatic_helmet"));
        assertFalse(AirStorage.dedicatedContainer("pneumaticcraft", "minigun"));
        assertFalse(AirStorage.dedicatedContainer("appliedpneumatics", "portable_air_cell_creative"));
        assertFalse(AirStorage.dedicatedContainer("example", "air_canister"));
    }
}
