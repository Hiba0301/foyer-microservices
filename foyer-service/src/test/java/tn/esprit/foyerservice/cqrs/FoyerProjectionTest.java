package tn.esprit.foyerservice.cqrs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.foyerservice.cqrs.events.CapaciteFoyerModifieeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerCreeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerSupprimeEvent;
import tn.esprit.foyerservice.cqrs.queries.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Côté LECTURE du CQRS : chaque événement met à jour la table foyer_view, les requêtes ne lisent que cette table. */
@ExtendWith(MockitoExtension.class)
class FoyerProjectionTest {

    private static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-21T10:00:00Z");

    @Mock
    private FoyerViewRepository repository;

    @InjectMocks
    private FoyerProjection projection;

    @Test
    void foyerCreeAjouteUneLigneDansLeModeleDeLecture() {
        projection.on(new FoyerCreeEvent("f-1", "Foyer Ennasr", 120), T0);

        ArgumentCaptor<FoyerView> vue = ArgumentCaptor.forClass(FoyerView.class);
        verify(repository).save(vue.capture());
        assertEquals("f-1", vue.getValue().getFoyerId());
        assertEquals("Foyer Ennasr", vue.getValue().getNomFoyer());
        assertEquals(120, vue.getValue().getCapaciteFoyer());
        assertEquals(T0.toString(), vue.getValue().getDateCreation());
        assertEquals(0, vue.getValue().getNombreModifications());
    }

    @Test
    void capaciteModifieeMetAJourLaLigneExistante() {
        FoyerView existante = FoyerView.builder().foyerId("f-1").nomFoyer("Foyer Ennasr").capaciteFoyer(120)
                .dateCreation(T0.toString()).derniereModification(T0.toString()).nombreModifications(0).build();
        when(repository.findById("f-1")).thenReturn(Optional.of(existante));

        projection.on(new CapaciteFoyerModifieeEvent("f-1", 120, 180), T1);

        verify(repository).save(existante);
        assertEquals(180, existante.getCapaciteFoyer());
        assertEquals(1, existante.getNombreModifications());
        assertEquals(T1.toString(), existante.getDerniereModification());
    }

    @Test
    void capaciteModifieeSurUnFoyerAbsentNeFaitRien() {
        when(repository.findById("inconnu")).thenReturn(Optional.empty());

        projection.on(new CapaciteFoyerModifieeEvent("inconnu", 0, 50), T1);

        verify(repository, never()).save(any());
    }

    @Test
    void foyerSupprimeRetireLaLigne() {
        projection.on(new FoyerSupprimeEvent("f-1"));

        verify(repository).deleteById("f-1");
    }

    @Test
    void requeteParIdLitLeModeleDeLecture() {
        FoyerView vue = FoyerView.builder().foyerId("f-1").nomFoyer("Foyer Ennasr").build();
        when(repository.findById("f-1")).thenReturn(Optional.of(vue));
        when(repository.findById("absent")).thenReturn(Optional.empty());

        assertSame(vue, projection.handle(new GetFoyerByIdQuery("f-1")));
        assertNull(projection.handle(new GetFoyerByIdQuery("absent")));
    }

    @Test
    void requeteListeFiltreParCapaciteMinimale() {
        List<FoyerView> grands = List.of(FoyerView.builder().foyerId("f-2").capaciteFoyer(200).build());
        when(repository.findByCapaciteFoyerGreaterThanEqualOrderByNomFoyerAsc(150)).thenReturn(grands);

        FoyersView resultat = projection.handle(new GetAllFoyersQuery(150));

        assertEquals(grands, resultat.getFoyers());
    }

    @Test
    void reinitialisationAvantUnReplayVideLaTable() {
        projection.reinitialiser();

        verify(repository).deleteAll();
    }
}
