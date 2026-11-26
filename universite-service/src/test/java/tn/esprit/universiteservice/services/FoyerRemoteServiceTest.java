package tn.esprit.universiteservice.services;

import feign.Request;
import feign.RetryableException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.universiteservice.clients.FoyerClient;
import tn.esprit.universiteservice.dto.FoyerDTO;
import tn.esprit.universiteservice.dto.FoyersResultat;
import tn.esprit.universiteservice.exceptions.*;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Logique de FoyerRemoteService SANS Spring : validation des réponses (défaillance "réponse incorrecte")
 * et méthodes de fallback (appelées par Resilience4j quand les tentatives ont échoué).
 * Les annotations @Retry / @CircuitBreaker / @RateLimiter, elles, sont démontrées par les scénarios de l'étape 7.
 */
class FoyerRemoteServiceTest {

    private FoyerClient foyerClient;
    private SimpleMeterRegistry meterRegistry;
    private FoyerRemoteService service;

    @BeforeEach
    void setUp() {
        foyerClient = mock(FoyerClient.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new FoyerRemoteService(foyerClient, meterRegistry);
    }

    // ------------------------------------------------------------ appels normaux et validation

    @Test
    void getFoyerRenvoieLeFoyerQuandLaReponseEstCorrecte() {
        when(foyerClient.getFoyer(1L)).thenReturn(foyer(1L, "Foyer A", 300));

        FoyerDTO resultat = service.getFoyer(1L);

        assertEquals("Foyer A", resultat.getNomFoyer());
        assertTrue(resultat.isDisponible());
    }

    @Test
    void getFoyerRefuseUneReponseCorrompue() {
        when(foyerClient.getFoyer(1L)).thenReturn(foyer(1L, null, -1));

        assertThrows(ReponseInvalideException.class, () -> service.getFoyer(1L));
    }

    @Test
    void getFoyerRefuseUneReponseVide() {
        when(foyerClient.getFoyer(1L)).thenReturn(null);

        assertThrows(ReponseInvalideException.class, () -> service.getFoyer(1L));
    }

    @Test
    void getAllFoyersRenvoieUnResultatDisponible() {
        when(foyerClient.getAllFoyers()).thenReturn(List.of(foyer(1L, "Foyer A", 300), foyer(2L, "Foyer B", 200)));

        FoyersResultat resultat = service.getAllFoyers();

        assertTrue(resultat.disponible());
        assertEquals(2, resultat.foyers().size());
    }

    @Test
    void getAllFoyersRefuseUneCapaciteNegative() {
        when(foyerClient.getAllFoyers()).thenReturn(List.of(foyer(1L, "Foyer A", -5)));

        assertThrows(ReponseInvalideException.class, () -> service.getAllFoyers());
    }

    @Test
    void verifierFoyerRefuseUnNomVide() {
        when(foyerClient.getFoyer(3L)).thenReturn(foyer(3L, "  ", 100));

        assertThrows(ReponseInvalideException.class, () -> service.verifierFoyer(3L));
    }

    @Test
    void verifierFoyerRenvoieLeFoyer() {
        when(foyerClient.getFoyer(3L)).thenReturn(foyer(3L, "Foyer C", 100));

        assertEquals(3L, service.verifierFoyer(3L).getIdFoyer());
    }

    // ------------------------------------------------------------ fallbacks

    @Test
    void lectureDegradeeQuandFoyerServiceEstEnPanne() {
        FoyerDTO resultat = ReflectionTestUtils.invokeMethod(service, "foyerDegrade", 1L,
                new FoyerServiceException("foyer-service a répondu HTTP 500"));

        assertNotNull(resultat);
        assertFalse(resultat.isDisponible());
        assertEquals(0, resultat.getCapaciteFoyer());
        assertTrue(resultat.getNomFoyer().contains("indisponible : foyer-service a répondu HTTP 500"));
        // la métrique visible dans Grafana est incrémentée
        assertEquals(1.0, meterRegistry.counter("foyer.fallback",
                "methode", "getFoyer", "cause", "FoyerServiceException").count());
    }

    @Test
    void pasDeModeDegradePourUnFoyerInexistant() {
        FoyerIntrouvableException introuvable = new FoyerIntrouvableException("404");

        assertThrows(FoyerIntrouvableException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "foyerDegrade", 1L, introuvable));
    }

    @Test
    void listeDegradeeQuandFoyerServiceEstEnPanne() {
        FoyersResultat resultat = ReflectionTestUtils.invokeMethod(service, "foyersDegrades",
                new ReponseInvalideException("données corrompues"));

        assertNotNull(resultat);
        assertFalse(resultat.disponible());
        assertTrue(resultat.foyers().isEmpty());
        assertEquals("réponse incorrecte", resultat.raison());
    }

    @Test
    void ecritureRefuseeEn503QuandFoyerServiceEstEnPanne() {
        FoyerServiceException panne = new FoyerServiceException("HTTP 503");

        ServiceIndisponibleException e = assertThrows(ServiceIndisponibleException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "verificationImpossible", 7L, panne));
        assertTrue(e.getMessage().contains("foyer 7"));
    }

    @Test
    void ecritureRefuseeEn429QuandLeRateLimiterBloque() {
        RequestNotPermitted limite = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("test"));

        assertThrows(TropDeRequetesException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "verificationImpossible", 7L, limite));
    }

    @Test
    void ecritureSurUnFoyerInexistantResteUne404() {
        FoyerIntrouvableException introuvable = new FoyerIntrouvableException("404");

        assertThrows(FoyerIntrouvableException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "verificationImpossible", 7L, introuvable));
    }

    // ------------------------------------------------------------ messages de raison

    @Test
    void raisonDelaiDepasse() {
        RetryableException e = retryable(new SocketTimeoutException("Read timed out"));
        assertEquals("délai de réponse dépassé (> 5 s)", FoyerRemoteService.raison(e));
    }

    @Test
    void raisonConnexionImpossible() {
        RetryableException e = retryable(new java.net.ConnectException("Connection refused"));
        assertEquals("connexion impossible", FoyerRemoteService.raison(e));
    }

    @Test
    void raisonErreurHttp() {
        assertEquals("foyer-service a répondu HTTP 500",
                FoyerRemoteService.raison(new FoyerServiceException("foyer-service a répondu HTTP 500")));
    }

    @Test
    void raisonInconnue() {
        assertEquals("IllegalStateException", FoyerRemoteService.raison(new IllegalStateException("?")));
    }

    // ------------------------------------------------------------ outils

    private static FoyerDTO foyer(Long id, String nom, long capacite) {
        return new FoyerDTO(id, nom, capacite, true);
    }

    private static RetryableException retryable(Throwable cause) {
        Request requete = Request.create(Request.HttpMethod.GET, "http://foyer-service/api/foyers/get",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.GET, cause, (Date) null, requete);
    }
}
