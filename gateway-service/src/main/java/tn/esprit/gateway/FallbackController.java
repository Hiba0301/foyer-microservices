package tn.esprit.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Réponses renvoyées par la gateway quand le circuit breaker d'une route est ouvert
 * ou que le microservice ne répond pas (filtre CircuitBreaker, fallbackUri=forward:/fallback/...).
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/foyers")
    public ResponseEntity<Map<String, Object>> foyersFallback() {
        return reponse("foyer-service");
    }

    @RequestMapping("/universites")
    public ResponseEntity<Map<String, Object>> universitesFallback() {
        return reponse("universite-service");
    }

    @RequestMapping("/blocs")
    public ResponseEntity<Map<String, Object>> blocsFallback() {
        return reponse("bloc-service");
    }

    private ResponseEntity<Map<String, Object>> reponse(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "service", service,
                "message", "Le service " + service + " est momentanément indisponible (fallback de l'API Gateway). Réessayez plus tard.",
                "timestamp", Instant.now().toString()));
    }
}
