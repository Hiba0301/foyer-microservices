package tn.esprit.foyerservice.exceptions;

/** Un autre microservice (universite-service) n'a pas pu être joint. */
public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
