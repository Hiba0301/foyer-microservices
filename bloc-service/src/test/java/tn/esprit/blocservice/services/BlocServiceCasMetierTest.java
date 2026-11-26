package tn.esprit.blocservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.blocservice.dto.BlocDTO;
import tn.esprit.blocservice.dto.BlocResponse;
import tn.esprit.blocservice.dto.FoyerDTO;
import tn.esprit.blocservice.dto.FoyersResultat;
import tn.esprit.blocservice.entities.Bloc;
import tn.esprit.blocservice.exceptions.CapaciteDepasseeException;
import tn.esprit.blocservice.repositories.BlocRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cas métier de BlocService non couverts par BlocServiceTest : liste enrichie avec les foyers,
 * modification d'un bloc (règle de capacité revérifiée), blocs d'un foyer, suppression.
 */
@ExtendWith(MockitoExtension.class)
class BlocServiceCasMetierTest {

    @Mock
    private BlocRepository blocRepository;

    @Mock
    private FoyerRemoteService foyerRemoteService;

    @InjectMocks
    private BlocService blocService;

    // ------------------------------------------------------------ liste et lecture

    @Test
    void getAllSansAucunFoyerAffecteNAppellePasFoyerService() {
        when(blocRepository.findAll()).thenReturn(List.of(bloc(1L, "Bloc A", 50, null)));

        List<BlocResponse> blocs = blocService.getAllBlocs();

        assertEquals(1, blocs.size());
        assertNull(blocs.get(0).foyer());
        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void getAllEnrichitLesBlocsAvecUnSeulAppelAFoyerService() {
        when(blocRepository.findAll()).thenReturn(List.of(
                bloc(1L, "Bloc A", 150, 10L), bloc(2L, "Bloc B", 100, null), bloc(3L, "Bloc C", 80, 20L)));
        when(foyerRemoteService.getAllFoyers()).thenReturn(FoyersResultat.disponibles(List.of(foyer(10L, 300))));

        List<BlocResponse> blocs = blocService.getAllBlocs();

        assertEquals("Foyer 10", blocs.get(0).foyer().getNomFoyer());
        assertNull(blocs.get(1).foyer());     // bloc sans foyer
        assertNull(blocs.get(2).foyer());     // foyer 20 supprimé dans foyer-service
        verify(foyerRemoteService, times(1)).getAllFoyers();
    }

    @Test
    void getBlocAvecSonFoyer() {
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc(1L, "Bloc A", 150, 10L)));
        when(foyerRemoteService.getFoyer(10L)).thenReturn(foyer(10L, 300));

        BlocResponse resultat = blocService.getBlocById(1L);

        assertEquals(300, resultat.foyer().getCapaciteFoyer());
    }

    @Test
    void getBlocSansFoyerNAppellePasFoyerService() {
        when(blocRepository.findById(2L)).thenReturn(Optional.of(bloc(2L, "Bloc B", 100, null)));

        assertNull(blocService.getBlocById(2L).foyer());
        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void blocsDUnFoyerAvecUnSeulAppelAFoyerService() {
        when(blocRepository.findByIdFoyer(10L)).thenReturn(List.of(bloc(1L, "Bloc A", 150, 10L), bloc(3L, "Bloc C", 80, 10L)));
        when(foyerRemoteService.getFoyer(10L)).thenReturn(foyer(10L, 300));

        List<BlocResponse> blocs = blocService.getBlocsParFoyer(10L);

        assertEquals(2, blocs.size());
        assertTrue(blocs.stream().allMatch(b -> b.foyer().getIdFoyer() == 10L));
        verify(foyerRemoteService, times(1)).getFoyer(10L);
    }

    @Test
    void blocsDUnFoyerVideNAppellePasFoyerService() {
        when(blocRepository.findByIdFoyer(99L)).thenReturn(List.of());

        assertTrue(blocService.getBlocsParFoyer(99L).isEmpty());
        verifyNoInteractions(foyerRemoteService);
    }

    // ------------------------------------------------------------ modification

    @Test
    void modifierUnBlocSansFoyerNeVerifiePasLaCapacite() {
        Bloc bloc = bloc(2L, "Bloc B", 100, null);
        when(blocRepository.findById(2L)).thenReturn(Optional.of(bloc));
        when(blocRepository.save(bloc)).thenReturn(bloc);

        BlocResponse resultat = blocService.updateBloc(new BlocDTO(2L, "Bloc B2", 120));

        assertEquals("Bloc B2", resultat.nomBloc());
        assertEquals(120, resultat.capaciteBloc());
        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void modifierUnBlocAffecteRespecteLaCapaciteDuFoyer() {
        Bloc bloc = bloc(1L, "Bloc A", 150, 10L);
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc));
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, 300));
        // le bloc lui-même (id 1) n'est pas compté deux fois : 100 (bloc 3) + 180 = 280 <= 300
        when(blocRepository.findByIdFoyer(10L)).thenReturn(List.of(bloc, bloc(3L, "Bloc C", 100, 10L)));
        when(blocRepository.save(bloc)).thenReturn(bloc);

        BlocResponse resultat = blocService.updateBloc(new BlocDTO(1L, "Bloc A", 180));

        assertEquals(180, resultat.capaciteBloc());
        assertEquals(10L, resultat.foyer().getIdFoyer());
    }

    @Test
    void modifierUnBlocAffecteQuiDepasseraitLaCapaciteEstRefuse() {
        Bloc bloc = bloc(1L, "Bloc A", 150, 10L);
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc));
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, 300));
        when(blocRepository.findByIdFoyer(10L)).thenReturn(List.of(bloc, bloc(3L, "Bloc C", 150, 10L)));
        BlocDTO tropGrand = new BlocDTO(1L, "Bloc A", 200);

        assertThrows(CapaciteDepasseeException.class, () -> blocService.updateBloc(tropGrand));
        verify(blocRepository, never()).save(any());
        assertEquals(150, bloc.getCapaciteBloc());
    }

    @Test
    void modifierAvecUneCapaciteNegativeEstRefuse() {
        BlocDTO negatif = new BlocDTO(1L, "Bloc A", -1);

        assertThrows(IllegalArgumentException.class, () -> blocService.updateBloc(negatif));
        verifyNoInteractions(blocRepository, foyerRemoteService);
    }

    // ------------------------------------------------------------ suppression

    @Test
    void supprimerUnBlocExistant() {
        when(blocRepository.existsById(1L)).thenReturn(true);

        blocService.deleteBloc(1L);

        verify(blocRepository).deleteById(1L);
    }

    @Test
    void supprimerUnBlocInexistantNeLevePasDErreur() {
        when(blocRepository.existsById(99L)).thenReturn(false);

        assertDoesNotThrow(() -> blocService.deleteBloc(99L));
        verify(blocRepository).deleteById(99L);
    }

    // ------------------------------------------------------------ outils

    private static Bloc bloc(long id, String nom, long capacite, Long idFoyer) {
        return Bloc.builder().idBloc(id).nomBloc(nom).capaciteBloc(capacite).idFoyer(idFoyer).build();
    }

    private static FoyerDTO foyer(Long id, long capacite) {
        return new FoyerDTO(id, "Foyer " + id, capacite, true);
    }
}
