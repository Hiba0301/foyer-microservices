package tn.esprit.foyerservice.cqrs.queries;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import tn.esprit.foyerservice.cqrs.events.CapaciteFoyerModifieeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerCreeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerSupprimeEvent;

import java.time.Instant;
import java.util.List;

/**
 * Projecteur : écoute les événements (@EventHandler) pour tenir à jour la table foyer_view,
 * et répond aux requêtes (@QueryHandler) en lisant uniquement cette table.
 * NB : le TP ne contenait que le @QueryHandler ; sans @EventHandler la table de lecture resterait vide.
 */
@Component
@ProcessingGroup("foyer-projection")
@RequiredArgsConstructor
@Slf4j
public class FoyerProjection {

    private final FoyerViewRepository repository;

    @EventHandler
    public void on(FoyerCreeEvent event, @Timestamp Instant timestamp) {
        log.info("Projection : foyer créé {}", event.getFoyerId());
        repository.save(FoyerView.builder()
                .foyerId(event.getFoyerId())
                .nomFoyer(event.getNomFoyer())
                .capaciteFoyer(event.getCapaciteFoyer())
                .dateCreation(timestamp.toString())
                .derniereModification(timestamp.toString())
                .nombreModifications(0)
                .build());
    }

    @EventHandler
    public void on(CapaciteFoyerModifieeEvent event, @Timestamp Instant timestamp) {
        log.info("Projection : capacité du foyer {} -> {}", event.getFoyerId(), event.getNouvelleCapacite());
        repository.findById(event.getFoyerId()).ifPresent(vue -> {
            vue.setCapaciteFoyer(event.getNouvelleCapacite());
            vue.setDerniereModification(timestamp.toString());
            vue.setNombreModifications(vue.getNombreModifications() + 1);
            repository.save(vue);
        });
    }

    @EventHandler
    public void on(FoyerSupprimeEvent event) {
        log.info("Projection : foyer supprimé {}", event.getFoyerId());
        repository.deleteById(event.getFoyerId());
    }

    @QueryHandler
    public FoyerView handle(GetFoyerByIdQuery query) {
        return repository.findById(query.getFoyerId()).orElse(null);
    }

    /** Renvoie un objet "enveloppe" plutôt qu'une List : la liste garde son type après sérialisation Jackson. */
    @QueryHandler
    public FoyersView handle(GetAllFoyersQuery query) {
        return new FoyersView(repository.findByCapaciteFoyerGreaterThanEqualOrderByNomFoyerAsc(query.getCapaciteMin()));
    }

    /** Appelé avant un "replay" : on vide la table puis on la reconstruit depuis les événements. */
    @ResetHandler
    public void reinitialiser() {
        repository.deleteAll();
    }
}
