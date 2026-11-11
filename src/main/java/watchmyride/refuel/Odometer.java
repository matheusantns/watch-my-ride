package watchmyride.refuel;

public final class Odometer {

    private static final Integer MIN_KM_INTEGER = 0;
    private static final Integer MAX_KM_INTEGER = 9999999;

    private Odometer() {
    }   
    
    public static void acceptsInsert(Integer highestStored, Integer next) {
        if(next < MIN_KM_INTEGER || next > MAX_KM_INTEGER) {
            throw new RuleViolationException("invalid_odometer");
        }

        if(highestStored == null) {
            return; 
        }

        if(next < highestStored) {
            throw new RuleViolationException("regressive_odometer");
        }

        if(next == highestStored) {
            throw new RuleViolationException("duplicate_odometer");
        }

    }

    
}
