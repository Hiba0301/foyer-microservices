package tn.esprit.universiteservice.exceptions;

/** Défaillance "réponse incorrecte" : foyer-service a répondu, mais avec des données corrompues. */
public class ReponseInvalideException extends FoyerServiceException {
    public ReponseInvalideException(String message) {
        super(message);
    }
}
