package ru.mirea.autosalon.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDriveStatusTest {

    @Test
    void newRequestCanOnlyBeConfirmedOrCancelled() {
        assertEquals(EnumSet.of(TestDriveStatus.CONFIRMED, TestDriveStatus.CANCELLED),
                TestDriveStatus.NEW.getAllowedTransitions());
        assertFalse(TestDriveStatus.NEW.canTransitionTo(TestDriveStatus.COMPLETED));
    }

    @Test
    void confirmedRequestCanBeClosedInThreeWays() {
        assertTrue(TestDriveStatus.CONFIRMED.canTransitionTo(TestDriveStatus.COMPLETED));
        assertTrue(TestDriveStatus.CONFIRMED.canTransitionTo(TestDriveStatus.NO_SHOW));
        assertTrue(TestDriveStatus.CONFIRMED.canTransitionTo(TestDriveStatus.CANCELLED));
        assertFalse(TestDriveStatus.CONFIRMED.canTransitionTo(TestDriveStatus.NEW));
    }

    @Test
    void closedRequestsHaveNoTransitions() {
        for (TestDriveStatus status : TestDriveStatus.values()) {
            assertEquals(status.isFinal(), status.getAllowedTransitions().isEmpty(), status.name());
        }
    }
}
