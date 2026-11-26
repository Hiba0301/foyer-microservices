package tn.esprit.universiteservice.services;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import tn.esprit.universiteservice.exceptions.ReponseInvalideException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Vérifie les messages affichés en mode dégradé selon le type de défaillance. */
class FoyerRemoteServiceRaisonTest {

    @Test
    void circuitOuvert() {
        CallNotPermittedException e = CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("test"));
        assertEquals("circuit breaker ouvert", FoyerRemoteService.raison(e));
    }

    @Test
    void limiteDeDebit() {
        RequestNotPermitted e = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("test"));
        assertEquals("limite de 5 requêtes/s atteinte", FoyerRemoteService.raison(e));
    }

    @Test
    void reponseIncorrecte() {
        assertEquals("réponse incorrecte", FoyerRemoteService.raison(new ReponseInvalideException("données corrompues")));
    }
}
