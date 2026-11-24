package tn.esprit.blocservice.exceptions;

/** foyer-service ne répond pas et l'opération ne peut pas se faire en mode dégradé (fallback d'une écriture). */
public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message) {
        super(message);
    }
}
