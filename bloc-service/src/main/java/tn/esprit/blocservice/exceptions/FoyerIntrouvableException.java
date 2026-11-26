package tn.esprit.blocservice.exceptions;

/** Le foyer demandé n'existe pas (404) : erreur métier, ni retry ni circuit breaker. */
public class FoyerIntrouvableException extends RuntimeException {
    public FoyerIntrouvableException(String message) {
        super(message);
    }
}
