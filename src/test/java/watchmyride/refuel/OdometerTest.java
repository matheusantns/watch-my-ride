package watchmyride.refuel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OdometerTest {

    @Nested 
    @DisplayName("acceptsInsert")
    class AcceptsInsert {
        
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

    @Nested 
    @DisplayName("acceptsUpdate")
    class AcceptsUpdate {
        @Test 
        @DisplayName("km below 0 raises 'invalid_odometer")
        void kmBelowZero() {
            RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsUpdate(1000, -2, 1200));

            assertEquals("invalid_odometer", error.code());
        }

        @Test 
        @DisplayName("km above 9999999 raises 'invalid_odometer'")
        void kmAboveLimit() {
            RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsUpdate(1000, 99999999, 1200));

            assertEquals("invalid_odometer", error.code());
        }

        @Test 
        @DisplayName("km 11500 between 10000 and 12000 should pass")
        void kmValid() {
            assertDoesNotThrow(() -> Odometer.acceptsUpdate(10000, 11500, 12000));
        }

        @Test 
        @DisplayName("next km equal or below raises 'regressive_odometer'")
        void regressiveOdometerBelow() {
            RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsUpdate(10000, 10000, 12000));

            assertEquals("regressive_odometer", error.code());
        }

        @Test 
        @DisplayName("next km equal or above raises 'regressive_odometer'")
        void regressiveOdometerAbove() {
            RuleViolationException error = assertThrows(RuleViolationException.class, () -> Odometer.acceptsUpdate(10000, 12000, 12000));

            assertEquals("regressive_odometer", error.code());
        }

        @Test 
        @DisplayName("two null neighbors should pass")
        void twoNullNeighbors() {
            assertDoesNotThrow(() -> Odometer.acceptsUpdate(null, 123, null));
        }
    }

}
