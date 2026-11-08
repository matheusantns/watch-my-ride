package watchmyride.refuel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    @DisplayName("complete returns liters price and total at scales 3 3 and 2")
    void scales() {
        Money.Amounts amounts = Money.complete(new BigDecimal("40"), new BigDecimal("5.899"), null);

        assertEquals(3, amounts.liters().scale());
        assertEquals(3, amounts.pricePerLiter().scale());
        assertEquals(2, amounts.totalAmount().scale());
        assertEquals(new BigDecimal("40.000"), amounts.liters());
        assertEquals(new BigDecimal("5.899"), amounts.pricePerLiter());
    }

    @Test
    @DisplayName("40 liters at 5.899 with no total returns 235.96")
    void priceFillsTotal() {
        Money.Amounts amounts = Money.complete(new BigDecimal("40.000"), new BigDecimal("5.899"), null);

        assertEquals(new BigDecimal("235.96"), amounts.totalAmount());
        assertEquals(new BigDecimal("5.899"), amounts.pricePerLiter());
    }

    @Test
    @DisplayName("total 235.96 with no price returns 5.899")
    void totalFillsPrice() {
        Money.Amounts amounts = Money.complete(new BigDecimal("40.000"), null, new BigDecimal("235.96"));

        assertEquals(new BigDecimal("5.899"), amounts.pricePerLiter());
        assertEquals(new BigDecimal("235.96"), amounts.totalAmount());
    }

    @Test
    @DisplayName("total 235.97 with price 5.899 stays 235.97")
    void keepsTotalInsideOneCent() {
        Money.Amounts amounts = Money.complete(
                new BigDecimal("40.000"), new BigDecimal("5.899"), new BigDecimal("235.97"));

        assertEquals(new BigDecimal("235.97"), amounts.totalAmount());
        assertEquals(new BigDecimal("5.899"), amounts.pricePerLiter());
    }

    @Test
    @DisplayName("total 235.98 raises inconsistent_amount")
    void rejectsTotalPastOneCent() {
        RuleViolationException error = assertThrows(RuleViolationException.class, () -> Money.complete(
                new BigDecimal("40.000"), new BigDecimal("5.899"), new BigDecimal("235.98")));

        assertEquals("inconsistent_amount", error.code());
    }

    @Test
    @DisplayName("a missing money pair raises required_field")
    void rejectsMissingPair() {
        RuleViolationException error = assertThrows(RuleViolationException.class,
                () -> Money.complete(new BigDecimal("40.000"), null, null));

        assertEquals("required_field", error.code());
    }

    @Test
    @DisplayName("out of range zero and negative money raise invalid_liters invalid_price or invalid_amount")
    void rejectsOutOfRange() {
        assertEquals("invalid_liters", code(new BigDecimal("0"), new BigDecimal("5.899"), null));
        assertEquals("invalid_liters", code(new BigDecimal("-1"), new BigDecimal("5.899"), null));
        assertEquals("invalid_liters", code(new BigDecimal("200.001"), new BigDecimal("5.899"), null));
        assertEquals("invalid_price", code(new BigDecimal("40"), new BigDecimal("0"), null));
        assertEquals("invalid_price", code(new BigDecimal("40"), new BigDecimal("-1"), null));
        assertEquals("invalid_price", code(new BigDecimal("40"), new BigDecimal("100"), null));
        assertEquals("invalid_amount", code(new BigDecimal("40"), null, new BigDecimal("0")));
        assertEquals("invalid_amount", code(new BigDecimal("40"), null, new BigDecimal("-1")));
        assertEquals("invalid_amount", code(new BigDecimal("40"), null, new BigDecimal("20000.01")));
    }

    @Test
    @DisplayName("3 liters and total 10.00 return price 3.333")
    void roundsPriceHalfUp() {
        Money.Amounts amounts = Money.complete(new BigDecimal("3"), null, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("3.333"), amounts.pricePerLiter());
        assertEquals(new BigDecimal("10.00"), amounts.totalAmount());
        assertEquals(new BigDecimal("3.000"), amounts.liters());
    }

    private static String code(BigDecimal liters, BigDecimal price, BigDecimal total) {
        return assertThrows(RuleViolationException.class, () -> Money.complete(liters, price, total)).code();
    }
}
