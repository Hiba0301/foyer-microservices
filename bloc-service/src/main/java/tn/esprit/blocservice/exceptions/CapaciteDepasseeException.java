package tn.esprit.blocservice.exceptions;

/** La somme des capacités des blocs dépasserait la capacité du foyer. */
public class CapaciteDepasseeException extends RuntimeException {
    public CapaciteDepasseeException(String message) {
        super(message);
    }
}
