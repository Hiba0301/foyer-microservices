package tn.esprit.blocservice.exceptions;

/** Panne technique de foyer-service (erreur 5xx, service absent d'Eureka...) : déclenche Retry et Circuit Breaker. */
public class FoyerServiceException extends RuntimeException {
    public FoyerServiceException(String message) {
        super(message);
    }
}
