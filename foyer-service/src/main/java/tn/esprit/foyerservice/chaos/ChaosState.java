package tn.esprit.foyerservice.chaos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Pannes simulées sur CETTE instance de foyer-service (en mémoire, remises à zéro au redémarrage).
 * Sert à démontrer la résilience de universite-service (Retry, Circuit Breaker, Fallback).
 */
@Component
@Getter
@Setter
public class ChaosState {
    /** Défaillance de temps de réponse : délai ajouté avant chaque réponse (ms). */
    private volatile long latenceMs = 0;
    /** Réponses incorrectes : les foyers renvoyés ont un nom vide et une capacité négative. */
    private volatile boolean reponsesCorrompues = false;
    /** Le service répond par une erreur HTTP 500. */
    private volatile boolean erreurServeur = false;

    public void reinitialiser() {
        latenceMs = 0;
        reponsesCorrompues = false;
        erreurServeur = false;
    }
}
