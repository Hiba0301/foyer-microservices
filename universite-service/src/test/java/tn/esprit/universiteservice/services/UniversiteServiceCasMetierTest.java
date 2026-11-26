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
import tn.esprit.universiteservice.repositories.UniversiteRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Cas métier de UniversiteService non couverts par UniversiteServiceTest : ajout, lecture,
 * suppression, affectation appelée par foyer-service (RestTemplate) et réaffectation du même foyer.
 */
@ExtendWith(MockitoExtension.class)
class UniversiteServiceCasMetierTest {

    @Mock
    private UniversiteRepository universiteRepository;

    @Mock
    private FoyerRemoteService foyerRemoteService;

    @InjectMocks
    private UniversiteService universiteService;

    @Test
    void ajouterUneUniversiteSansFoyerNAppellePasFoyerService() {
        Universite nouvelle = universite(1L, "ESPRIT", null);
        when(universiteRepository.save(nouvelle)).thenReturn(nouvelle);

        UniversiteDTO resultat = universiteService.addUniversite(nouvelle);

        assertEquals("ESPRIT", resultat.getNomUniversite());
        assertNull(resultat.getFoyer());
        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void lireUneUniversiteAvecSonFoyer() {
        when(universiteRepository.findById(1L)).thenReturn(Optional.of(universite(1L, "ESPRIT", 10L)));
        when(foyerRemoteService.getFoyer(10L)).thenReturn(foyer(10L));

        UniversiteDTO resultat = universiteService.getUniversiteById(1L);

        assertEquals("Foyer 10", resultat.getFoyer().getNomFoyer());
    }

    @Test
    void lireUneUniversiteInexistante() {
        when(universiteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> universiteService.getUniversiteById(99L));
    }

    @Test
    void getAllAvecUnFoyerSupprimeEtUneUniversiteSansFoyer() {
        when(universiteRepository.findAll()).thenReturn(List.of(
                universite(1L, "ESPRIT", 10L), universite(2L, "FST", 20L), universite(3L, "ENIT", null)));
        when(foyerRemoteService.getAllFoyers()).thenReturn(FoyersResultat.disponibles(List.of(foyer(10L))));

        List<UniversiteDTO> resultat = universiteService.getAllUniversites();

        assertEquals("Foyer 10", resultat.get(0).getFoyer().getNomFoyer());
        assertNull(resultat.get(1).getFoyer());   // foyer 20 supprimé dans foyer-service
        assertNull(resultat.get(2).getFoyer());   // université sans foyer
    }

    @Test
    void supprimerUneUniversiteExistanteOuNon() {
        when(universiteRepository.existsById(1L)).thenReturn(true);
        when(universiteRepository.existsById(99L)).thenReturn(false);

        universiteService.deleteUniversite(1L);
        assertDoesNotThrow(() -> universiteService.deleteUniversite(99L));

        verify(universiteRepository).deleteById(1L);
        verify(universiteRepository).deleteById(99L);
    }

    @Test
    void affecterParIdDepuisFoyerService() {
        Universite fst = universite(2L, "FST", null);
        when(universiteRepository.findById(2L)).thenReturn(Optional.of(fst));
        when(foyerRemoteService.verifierFoyer(5L)).thenReturn(foyer(5L));
        when(universiteRepository.findByIdFoyer(5L)).thenReturn(Optional.empty());
        when(universiteRepository.save(fst)).thenReturn(fst);

        UniversiteDTO resultat = universiteService.affecterFoyerParId(2L, 5L);

        assertEquals(5L, fst.getIdFoyer());
        assertEquals(5L, resultat.getFoyer().getIdFoyer());
    }

    @Test
    void reaffecterLeMemeFoyerALaMemeUniversiteEstAccepte() {
        Universite esprit = universite(1L, "ESPRIT", 10L);
        when(universiteRepository.findByNomUniversite("ESPRIT")).thenReturn(esprit);
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L));
        // le foyer 10 est déjà affecté... à cette même université : pas de conflit
        when(universiteRepository.findByIdFoyer(10L)).thenReturn(Optional.of(esprit));
        when(universiteRepository.save(esprit)).thenReturn(esprit);

        UniversiteDTO resultat = assertDoesNotThrow(() -> universiteService.affecterFoyerAUniversite(10L, "ESPRIT"));

        assertEquals(10L, resultat.getFoyer().getIdFoyer());
    }

    private static Universite universite(long id, String nom, Long idFoyer) {
        return Universite.builder().idUniversite(id).nomUniversite(nom).adresse("Tunis").idFoyer(idFoyer).build();
    }

    private static FoyerDTO foyer(Long id) {
        return new FoyerDTO(id, "Foyer " + id, 100, true);
    }
}
