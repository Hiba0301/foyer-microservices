package tn.esprit.foyerservice.services;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.foyerservice.entities.Foyer;
import tn.esprit.foyerservice.exceptions.ServiceIndisponibleException;
import tn.esprit.foyerservice.repositories.FoyerRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoyerServiceTest {

    private static final String URL_UNIVERSITE = "http://universite-service";

    @Mock
    private FoyerRepository foyerRepository;

    @Mock
    private RestTemplate restTemplate;

    private SimpleMeterRegistry meterRegistry;
    private FoyerService foyerService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        foyerService = new FoyerService(foyerRepository, restTemplate, meterRegistry, URL_UNIVERSITE);
    }

    @Test
    void addFoyerEnregistreEtIncrementeLaMetrique() {
        Foyer foyer = creerFoyer(1, "Foyer Mockito", 500);
        when(foyerRepository.save(foyer)).thenReturn(foyer);

        Foyer resultat = foyerService.addFoyer(foyer);

        assertEquals("Foyer Mockito", resultat.getNomFoyer());
        assertEquals(1.0, meterRegistry.get("foyers.crees").counter().count());
        verify(foyerRepository).save(foyer);
    }

    @Test
    void getAllFoyersRenvoieTousLesFoyers() {
        when(foyerRepository.findAll()).thenReturn(List.of(creerFoyer(1, "F1", 100), creerFoyer(2, "F2", 200)));

        assertEquals(2, foyerService.getAllFoyers().size());
    }

    @Test
    void getFoyerByIdRenvoieLeFoyer() {
        Foyer foyer = creerFoyer(1, "Foyer recherche", 300);
        when(foyerRepository.findById(1L)).thenReturn(Optional.of(foyer));

        assertEquals(foyer, foyerService.getFoyerById(1L));
    }

    @Test
    void getFoyerByIdLeveUneExceptionSiIntrouvable() {
        when(foyerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> foyerService.getFoyerById(99L));
    }

    @Test
    void deleteFoyerSupprimeParId() {
        when(foyerRepository.existsById(1L)).thenReturn(true);

        foyerService.deleteFoyer(1L);

        verify(foyerRepository).deleteById(1L);
    }

    @Test
    void updateFoyerEnregistre() {
        Foyer foyer = creerFoyer(1, "Foyer modifié", 120);
        when(foyerRepository.save(foyer)).thenReturn(foyer);

        assertEquals(foyer, foyerService.updateFoyer(foyer));
    }

    @Test
    void ajouterEtAffecterAppelleUniversiteService() {
        Foyer foyer = creerFoyer(5, "Foyer affecté", 250);
        when(foyerRepository.save(foyer)).thenReturn(foyer);

        Foyer resultat = foyerService.ajouterFoyerEtAffecterAUniversite(foyer, 1L);

        assertEquals(foyer, resultat);
        verify(restTemplate).put(eq(URL_UNIVERSITE + "/api/universites/{idUniversite}/foyer/{idFoyer}"), isNull(), eq(1L), eq(5L));
        verify(foyerRepository, never()).deleteById(anyLong());
    }

    @Test
    void ajouterEtAffecterCompenseSiUniversiteIntrouvable() {
        Foyer foyer = creerFoyer(5, "Foyer orphelin", 250);
        when(foyerRepository.save(foyer)).thenReturn(foyer);
        doThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null))
                .when(restTemplate).put(anyString(), any(), any(Object[].class));

        assertThrows(NoSuchElementException.class, () -> foyerService.ajouterFoyerEtAffecterAUniversite(foyer, 42L));
        verify(foyerRepository).deleteById(5L);
    }

    @Test
    void ajouterEtAffecterCompenseSiUniversiteServiceIndisponible() {
        Foyer foyer = creerFoyer(5, "Foyer orphelin", 250);
        when(foyerRepository.save(foyer)).thenReturn(foyer);
        doThrow(new ResourceAccessException("Connection refused"))
                .when(restTemplate).put(anyString(), any(), any(Object[].class));

        assertThrows(ServiceIndisponibleException.class, () -> foyerService.ajouterFoyerEtAffecterAUniversite(foyer, 1L));
        verify(foyerRepository).deleteById(5L);
    }

    private Foyer creerFoyer(long id, String nom, long capacite) {
        return Foyer.builder().idFoyer(id).nomFoyer(nom).capaciteFoyer(capacite).build();
    }
}
