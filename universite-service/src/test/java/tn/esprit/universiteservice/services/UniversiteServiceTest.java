package tn.esprit.universiteservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.universiteservice.dto.FoyerDTO;
import tn.esprit.universiteservice.dto.FoyersResultat;
import tn.esprit.universiteservice.dto.UniversiteDTO;
import tn.esprit.universiteservice.entities.Universite;
import tn.esprit.universiteservice.exceptions.FoyerDejaAffecteException;
import tn.esprit.universiteservice.exceptions.FoyerIntrouvableException;
import tn.esprit.universiteservice.exceptions.ServiceIndisponibleException;
import tn.esprit.universiteservice.repositories.UniversiteRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniversiteServiceTest {

    @Mock
    private UniversiteRepository universiteRepository;

    @Mock
    private FoyerRemoteService foyerRemoteService;

    @InjectMocks
    private UniversiteService universiteService;

    @Test
    void getAllEnrichitLesUniversitesAvecUnSeulAppelAFoyerService() {
        when(universiteRepository.findAll()).thenReturn(List.of(
                universite(1, "ESPRIT", 10L), universite(2, "FST", null)));
        when(foyerRemoteService.getAllFoyers()).thenReturn(FoyersResultat.disponibles(List.of(foyer(10L, "Foyer A"))));

        List<UniversiteDTO> resultat = universiteService.getAllUniversites();

        assertEquals("Foyer A", resultat.get(0).getFoyer().getNomFoyer());
        assertNull(resultat.get(1).getFoyer());
        verify(foyerRemoteService, times(1)).getAllFoyers();
    }

    @Test
    void getAllPasseEnModeDegradeQuandFoyerServiceEstIndisponible() {
        when(universiteRepository.findAll()).thenReturn(List.of(universite(1, "ESPRIT", 10L)));
        when(foyerRemoteService.getAllFoyers()).thenReturn(FoyersResultat.indisponibles("circuit breaker ouvert"));

        List<UniversiteDTO> resultat = universiteService.getAllUniversites();

        assertFalse(resultat.get(0).getFoyer().isDisponible());
        assertTrue(resultat.get(0).getFoyer().getNomFoyer().contains("circuit breaker ouvert"));
    }

    @Test
    void getAllNAppellePasFoyerServiceSiAucunFoyerNEstAffecte() {
        when(universiteRepository.findAll()).thenReturn(List.of(universite(1, "ESPRIT", null)));

        universiteService.getAllUniversites();

        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void affecterFoyerVerifieLeFoyerPuisEnregistre() {
        Universite esprit = universite(1, "ESPRIT", null);
        when(universiteRepository.findByNomUniversite("ESPRIT")).thenReturn(esprit);
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, "Foyer A"));
        when(universiteRepository.findByIdFoyer(10L)).thenReturn(Optional.empty());
        when(universiteRepository.save(esprit)).thenReturn(esprit);

        UniversiteDTO resultat = universiteService.affecterFoyerAUniversite(10L, "ESPRIT");

        assertEquals(10L, esprit.getIdFoyer());
        assertEquals("Foyer A", resultat.getFoyer().getNomFoyer());
    }

    @Test
    void affecterUnFoyerDejaPrisEstRefuse() {
        when(universiteRepository.findByNomUniversite("ESPRIT")).thenReturn(universite(1, "ESPRIT", null));
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, "Foyer A"));
        when(universiteRepository.findByIdFoyer(10L)).thenReturn(Optional.of(universite(2, "FST", 10L)));

        assertThrows(FoyerDejaAffecteException.class, () -> universiteService.affecterFoyerAUniversite(10L, "ESPRIT"));
        verify(universiteRepository, never()).save(any());
    }

    @Test
    void affecterEchoueProprementSiFoyerServiceEstIndisponible() {
        when(universiteRepository.findByNomUniversite("ESPRIT")).thenReturn(universite(1, "ESPRIT", null));
        when(foyerRemoteService.verifierFoyer(10L)).thenThrow(new ServiceIndisponibleException("indisponible"));

        assertThrows(ServiceIndisponibleException.class, () -> universiteService.affecterFoyerAUniversite(10L, "ESPRIT"));
        verify(universiteRepository, never()).save(any());
    }

    @Test
    void affecterAUneUniversiteInconnueLeveUneException() {
        when(universiteRepository.findByNomUniversite("Inconnue")).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> universiteService.affecterFoyerAUniversite(10L, "Inconnue"));
    }

    @Test
    void getUniversiteDontLeFoyerAEteSupprimeRenvoieUnFoyerNull() {
        when(universiteRepository.findById(1L)).thenReturn(Optional.of(universite(1, "ESPRIT", 10L)));
        when(foyerRemoteService.getFoyer(10L)).thenThrow(new FoyerIntrouvableException("404"));

        assertNull(universiteService.getUniversiteById(1L).getFoyer());
    }

    @Test
    void updateConserveLeFoyerAffecte() {
        Universite existante = universite(1, "ESPRIT", 10L);
        when(universiteRepository.findById(1L)).thenReturn(Optional.of(existante));
        when(universiteRepository.save(existante)).thenReturn(existante);
        when(foyerRemoteService.getFoyer(10L)).thenReturn(foyer(10L, "Foyer A"));

        UniversiteDTO resultat = universiteService.updateUniversite(universite(1, "ESPRIT Tunis", null));

        assertEquals("ESPRIT Tunis", resultat.getNomUniversite());
        assertEquals(10L, existante.getIdFoyer());
    }

    @Test
    void desaffecterRetireLeFoyer() {
        Universite esprit = universite(1, "ESPRIT", 10L);
        when(universiteRepository.findById(1L)).thenReturn(Optional.of(esprit));
        when(universiteRepository.save(esprit)).thenReturn(esprit);

        assertNull(universiteService.desaffecterFoyerAUniversite(1L).getFoyer());
        assertNull(esprit.getIdFoyer());
    }

    private Universite universite(long id, String nom, Long idFoyer) {
        return Universite.builder().idUniversite(id).nomUniversite(nom).adresse("Tunis").idFoyer(idFoyer).build();
    }

    private FoyerDTO foyer(Long id, String nom) {
        return new FoyerDTO(id, nom, 100, true);
    }
}
