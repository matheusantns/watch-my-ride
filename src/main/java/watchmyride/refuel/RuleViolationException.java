package watchmyride.refuel;

public class RuleViolationException extends RuntimeException {

    private final String code;

    public RuleViolationException(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
