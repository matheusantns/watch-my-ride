package watchmyride.refuel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class OdometerTest {
    @Test 
    @DisplayName("number smaller than the current km raises 'regressive_odometer'")
    void smallerNumber() {
        RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsInsert(999, 888));

        assertEquals("regressive_odometer", error.code());
    }

    @Test
    @DisplayName("number smaller than the limit raises 'invalid_odometer'") 
    void belowZero() {
        RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsInsert(5, -2));

        assertEquals("invalid_odometer", error.code());
    }

    @Test
    @DisplayName("number bigger than the limit raises 'invalid_odometer'") 
    void aboveLimit() {
        RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsInsert(5, 999999999));

        assertEquals("invalid_odometer", error.code());
    }

    @Test 
    @DisplayName("first refuel with null should work") 
    void firstFuel () {
        assertDoesNotThrow(() -> Odometer.acceptsInsert(null, 1));
    }

    @Test
    @DisplayName("next equals highestStored raises 'duplicate_odometer'") 
    void isEqual() {
        RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsInsert(5, 5));

        assertEquals("duplicate_odometer", error.code());
    }

    @Test 
    @DisplayName("valid next km")
    void validNextKm() {
        assertDoesNotThrow(() -> Odometer.acceptsInsert(123, 1234));
    }
}
