package tn.esprit.universiteservice.exceptions;

/** Le foyer est déjà affecté à une autre université. */
public class FoyerDejaAffecteException extends RuntimeException {
    public FoyerDejaAffecteException(String message) {
        super(message);
    }
}
