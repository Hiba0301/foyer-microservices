package tn.esprit.blocservice.services;

import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.blocservice.clients.FoyerClient;
import tn.esprit.blocservice.dto.FoyerDTO;
import tn.esprit.blocservice.dto.FoyersResultat;
import tn.esprit.blocservice.exceptions.*;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Tous les appels vers foyer-service passent ici, protégés par Resilience4j.
 * Ordre d'exécution : Retry( CircuitBreaker( RateLimiter( appel Feign ) ) )
 *  - RateLimiter : 5 appels / seconde maximum
 *  - CircuitBreaker : s'ouvre si >= 50 % d'échecs sur les 10 derniers appels
 *  - Retry : 3 tentatives espacées de 500 ms, puis méthode de fallback
 * Configuration : config-repo/bloc-service.yml (Config Server).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoyerRemoteService {

    static final String FOYER_SERVICE = "foyerService";

    private final FoyerClient foyerClient;
    private final MeterRegistry meterRegistry;

    /** Lecture d'un foyer : en cas de panne on renvoie un foyer "dégradé" (l'université reste affichée). */
    @Retry(name = FOYER_SERVICE, fallbackMethod = "foyerDegrade")
    @CircuitBreaker(name = FOYER_SERVICE)
    @RateLimiter(name = FOYER_SERVICE)
    public FoyerDTO getFoyer(long idFoyer) {
        log.info("Appel Feign -> foyer-service : foyer {}", idFoyer);
        FoyerDTO foyer = foyerClient.getFoyer(idFoyer);
        valider(foyer, idFoyer);
        return foyer;
    }

    /** Lecture de tous les foyers (un seul appel pour enrichir la liste des universités). */
    @Retry(name = FOYER_SERVICE, fallbackMethod = "foyersDegrades")
    @CircuitBreaker(name = FOYER_SERVICE)
    @RateLimiter(name = FOYER_SERVICE)
    public FoyersResultat getAllFoyers() {
        log.info("Appel Feign -> foyer-service : tous les foyers");
        List<FoyerDTO> foyers = foyerClient.getAllFoyers();
        foyers.forEach(foyer -> valider(foyer, foyer.getIdFoyer()));
        return FoyersResultat.disponibles(foyers);
    }

    /** Vérification avant une écriture (affectation) : pas de mode dégradé possible -> erreur claire. */
    @Retry(name = FOYER_SERVICE, fallbackMethod = "verificationImpossible")
    @CircuitBreaker(name = FOYER_SERVICE)
    @RateLimiter(name = FOYER_SERVICE)
    public FoyerDTO verifierFoyer(long idFoyer) {
        log.info("Appel Feign -> foyer-service : vérification du foyer {}", idFoyer);
        FoyerDTO foyer = foyerClient.getFoyer(idFoyer);
        valider(foyer, idFoyer);
        return foyer;
    }

    // ------------------------------------------------------------------ Fallbacks
    // Visibilité "package" (et non private) : ces méthodes ne sont appelées que par Resilience4j
    // (attribut fallbackMethod, par réflexion) ; un analyseur statique les croirait inutilisées si elles étaient privées.

    FoyerDTO foyerDegrade(long idFoyer, Throwable cause) {
        if (cause instanceof FoyerIntrouvableException introuvable) {
            throw introuvable; // erreur métier : pas de mode dégradé
        }
        enregistrerFallback("getFoyer", cause);
        return FoyerDTO.indisponible(idFoyer, raison(cause));
    }

    FoyersResultat foyersDegrades(Throwable cause) {
        enregistrerFallback("getAllFoyers", cause);
        return FoyersResultat.indisponibles(raison(cause));
    }

    FoyerDTO verificationImpossible(long idFoyer, Throwable cause) {
        if (cause instanceof FoyerIntrouvableException introuvable) {
            throw introuvable;
        }
        enregistrerFallback("verifierFoyer", cause);
        if (cause instanceof RequestNotPermitted) {
            throw new TropDeRequetesException("Trop de requêtes vers foyer-service (limite : 5 par seconde). Réessayez.");
        }
        throw new ServiceIndisponibleException(
                "Impossible de vérifier le foyer " + idFoyer + " : foyer-service indisponible (" + raison(cause) + ")");
    }

    // ------------------------------------------------------------------ Outils

    /** Défaillance "réponse incorrecte" : on refuse des données corrompues. */
    private void valider(FoyerDTO foyer, Long idAttendu) {
        if (foyer == null || foyer.getIdFoyer() == null || foyer.getNomFoyer() == null
                || foyer.getNomFoyer().isBlank() || foyer.getCapaciteFoyer() < 0) {
            throw new ReponseInvalideException("Réponse incorrecte de foyer-service pour le foyer " + idAttendu + " : " + foyer);
        }
    }

    private void enregistrerFallback(String methode, Throwable cause) {
        log.warn("FALLBACK {} -> {} : {}", methode, cause.getClass().getSimpleName(), cause.getMessage());
        // métrique Prometheus foyer_fallback_total, étiquetée par méthode et par cause (tableau de bord Grafana)
        meterRegistry.counter("foyer.fallback", "methode", methode, "cause", cause.getClass().getSimpleName()).increment();
    }

    static String raison(Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            return "circuit breaker ouvert";
        }
        if (cause instanceof RequestNotPermitted) {
            return "limite de 5 requêtes/s atteinte";
        }
        if (cause instanceof ReponseInvalideException) {
            return "réponse incorrecte";
        }
        if (cause instanceof RetryableException) {
            return cause.getCause() instanceof SocketTimeoutException
                    ? "délai de réponse dépassé (> 5 s)"
                    : "connexion impossible";
        }
        if (cause instanceof FoyerServiceException) {
            return cause.getMessage();
        }
        return cause.getClass().getSimpleName();
    }
}
