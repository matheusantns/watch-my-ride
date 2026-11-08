package watchmyride.refuel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private static final BigDecimal MIN_LITERS = new BigDecimal("0.001");
    private static final BigDecimal MAX_LITERS = new BigDecimal("200");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.001");
    private static final BigDecimal MAX_PRICE = new BigDecimal("99.999");
    private static final BigDecimal MIN_TOTAL = new BigDecimal("0.01");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("20000.00");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private Money() {
    }

    public static Amounts complete(BigDecimal liters, BigDecimal pricePerLiter, BigDecimal totalAmount) {
        if (liters == null || (pricePerLiter == null && totalAmount == null)) {
            throw new RuleViolationException("required_field");
        }

        BigDecimal scaledLiters = scale(liters, 3);
        requireInRange(scaledLiters, MIN_LITERS, MAX_LITERS, "invalid_liters");

        if (pricePerLiter == null) {
            BigDecimal scaledTotal = scale(totalAmount, 2);
            requireInRange(scaledTotal, MIN_TOTAL, MAX_TOTAL, "invalid_amount");
            BigDecimal price = scaledTotal.divide(scaledLiters, 3, RoundingMode.HALF_UP);
            requireInRange(price, MIN_PRICE, MAX_PRICE, "invalid_price");
            return new Amounts(scaledLiters, price, scaledTotal);
        }

        BigDecimal scaledPrice = scale(pricePerLiter, 3);
        requireInRange(scaledPrice, MIN_PRICE, MAX_PRICE, "invalid_price");

        if (totalAmount == null) {
            BigDecimal total = scaledLiters.multiply(scaledPrice).setScale(2, RoundingMode.HALF_UP);
            requireInRange(total, MIN_TOTAL, MAX_TOTAL, "invalid_amount");
            return new Amounts(scaledLiters, scaledPrice, total);
        }

        BigDecimal scaledTotal = scale(totalAmount, 2);
        requireInRange(scaledTotal, MIN_TOTAL, MAX_TOTAL, "invalid_amount");
        BigDecimal expected = scaledLiters.multiply(scaledPrice).setScale(2, RoundingMode.HALF_UP);
        if (scaledTotal.subtract(expected).abs().compareTo(TOLERANCE) > 0) {
            throw new RuleViolationException("inconsistent_amount");
        }
        return new Amounts(scaledLiters, scaledPrice, scaledTotal);
    }

    private static BigDecimal scale(BigDecimal value, int digits) {
        return value.setScale(digits, RoundingMode.HALF_UP);
    }

    private static void requireInRange(BigDecimal value, BigDecimal min, BigDecimal max, String code) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new RuleViolationException(code);
        }
    }

    public record Amounts(BigDecimal liters, BigDecimal pricePerLiter, BigDecimal totalAmount) {
    }
}
