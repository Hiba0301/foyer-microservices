package tn.esprit.gateway;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Réponse de la gateway quand un microservice est absent ou que le circuit breaker de sa route est ouvert
 * (filtre CircuitBreaker + fallbackUri=forward:/fallback/...). WebTestClient "lié au contrôleur" :
 * pas de contexte Spring, pas d'Eureka, pas de Config Server.
 */
class FallbackControllerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FallbackController()).build();

    @ParameterizedTest
    @CsvSource({"foyers, foyer-service", "universites, universite-service", "blocs, bloc-service"})
    void chaqueRouteRenvoieUn503Propre(String route, String service) {
        client.get().uri("/fallback/" + route)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.service").isEqualTo(service)
                .jsonPath("$.message").value(message ->
                        org.junit.jupiter.api.Assertions.assertTrue(message.toString().contains(service)))
                .jsonPath("$.timestamp").exists();
    }
}
