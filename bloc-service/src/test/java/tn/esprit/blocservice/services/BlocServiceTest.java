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
import tn.esprit.blocservice.exceptions.FoyerIntrouvableException;
import tn.esprit.blocservice.exceptions.ServiceIndisponibleException;
import tn.esprit.blocservice.repositories.BlocRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlocServiceTest {

    @Mock
    private BlocRepository blocRepository;

    @Mock
    private FoyerRemoteService foyerRemoteService;

    @InjectMocks
    private BlocService blocService;

    @Test
    void addBlocEnregistreSansFoyer() {
        when(blocRepository.save(any(Bloc.class))).thenAnswer(inv -> inv.getArgument(0));

        BlocResponse resultat = blocService.addBloc(new BlocDTO(0, "Bloc A", 50));

        assertEquals("Bloc A", resultat.nomBloc());
        assertNull(resultat.foyer());
        verifyNoInteractions(foyerRemoteService);
    }

    @Test
    void addBlocAvecCapaciteNulleEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> blocService.addBloc(new BlocDTO(0, "Bloc A", 0)));
        verify(blocRepository, never()).save(any());
    }

    @Test
    void affecterBlocRespecteLaCapaciteDuFoyer() {
        Bloc blocA = bloc(1, "Bloc A", 100, null);
        when(blocRepository.findById(1L)).thenReturn(Optional.of(blocA));
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, "Foyer El Amal", 300));
        when(blocRepository.findByIdFoyer(10L)).thenReturn(List.of(bloc(2, "Bloc B", 150, 10L)));
        when(blocRepository.save(blocA)).thenReturn(blocA);

        BlocResponse resultat = blocService.affecterBlocAFoyer(1L, 10L);

        assertEquals(10L, blocA.getIdFoyer());
        assertEquals("Foyer El Amal", resultat.foyer().getNomFoyer());
    }

    @Test
    void affecterBlocQuiDepasseLaCapaciteEstRefuse() {
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc(1, "Bloc A", 200, null)));
        when(foyerRemoteService.verifierFoyer(10L)).thenReturn(foyer(10L, "Foyer El Amal", 300));
        when(blocRepository.findByIdFoyer(10L)).thenReturn(List.of(bloc(2, "Bloc B", 150, 10L)));

        assertThrows(CapaciteDepasseeException.class, () -> blocService.affecterBlocAFoyer(1L, 10L));
        verify(blocRepository, never()).save(any());
    }

    @Test
    void affecterEchoueProprementSiFoyerServiceEstIndisponible() {
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc(1, "Bloc A", 100, null)));
        when(foyerRemoteService.verifierFoyer(10L)).thenThrow(new ServiceIndisponibleException("indisponible"));

        assertThrows(ServiceIndisponibleException.class, () -> blocService.affecterBlocAFoyer(1L, 10L));
        verify(blocRepository, never()).save(any());
    }

    @Test
    void getAllPasseEnModeDegradeQuandFoyerServiceEstIndisponible() {
        when(blocRepository.findAll()).thenReturn(List.of(bloc(1, "Bloc A", 100, 10L), bloc(2, "Bloc B", 80, null)));
        when(foyerRemoteService.getAllFoyers()).thenReturn(FoyersResultat.indisponibles("circuit breaker ouvert"));

        List<BlocResponse> resultat = blocService.getAllBlocs();

        assertFalse(resultat.get(0).foyer().isDisponible());
        assertNull(resultat.get(1).foyer());
    }

    @Test
    void getBlocDontLeFoyerAEteSupprimeRenvoieUnFoyerNull() {
        when(blocRepository.findById(1L)).thenReturn(Optional.of(bloc(1, "Bloc A", 100, 10L)));
        when(foyerRemoteService.getFoyer(10L)).thenThrow(new FoyerIntrouvableException("404"));

        assertNull(blocService.getBlocById(1L).foyer());
    }

    @Test
    void getBlocInconnuLeveUneException() {
        when(blocRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> blocService.getBlocById(99L));
    }

    @Test
    void desaffecterRetireLeFoyer() {
        Bloc blocA = bloc(1, "Bloc A", 100, 10L);
        when(blocRepository.findById(1L)).thenReturn(Optional.of(blocA));
        when(blocRepository.save(blocA)).thenReturn(blocA);

        assertNull(blocService.desaffecterBlocDuFoyer(1L).foyer());
        assertNull(blocA.getIdFoyer());
    }

    private Bloc bloc(long id, String nom, long capacite, Long idFoyer) {
        return Bloc.builder().idBloc(id).nomBloc(nom).capaciteBloc(capacite).idFoyer(idFoyer).build();
    }

    private FoyerDTO foyer(Long id, String nom, long capacite) {
        return new FoyerDTO(id, nom, capacite, true);
    }
}
