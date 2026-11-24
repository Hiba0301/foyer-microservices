package tn.esprit.universiteservice.exceptions;

/** Limite du Rate Limiter atteinte (5 appels/s vers foyer-service). */
public class TropDeRequetesException extends RuntimeException {
    public TropDeRequetesException(String message) {
        super(message);
    }
}
