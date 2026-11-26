package tn.esprit.foyerservice.cqrs.aggregate;

import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import tn.esprit.foyerservice.cqrs.commands.CreerFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.ModifierCapaciteFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.SupprimerFoyerCommand;
import tn.esprit.foyerservice.cqrs.events.CapaciteFoyerModifieeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerCreeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerSupprimeEvent;

/**
 * Côté ÉCRITURE (Command) : l'agrégat vérifie les règles métier puis publie des événements.
 * Il est "event sourcé" : son état est reconstruit en rejouant ses événements (@EventSourcingHandler).
 */
@Aggregate
@NoArgsConstructor // constructeur sans argument requis par Axon
public class FoyerAggregate {

    @AggregateIdentifier
    private String foyerId;
    private String nomFoyer;
    private long capaciteFoyer;

    @CommandHandler
    public FoyerAggregate(CreerFoyerCommand command) {
        if (command.getNomFoyer() == null || command.getNomFoyer().isBlank()) {
            throw new IllegalArgumentException("Le nom du foyer est obligatoire");
        }
        if (command.getCapaciteFoyer() <= 0) {
            throw new IllegalArgumentException("La capacité du foyer doit être strictement positive");
        }
        AggregateLifecycle.apply(new FoyerCreeEvent(
                command.getFoyerId(), command.getNomFoyer().trim(), command.getCapaciteFoyer()));
    }

    @CommandHandler
    public void handle(ModifierCapaciteFoyerCommand command) {
        if (command.getNouvelleCapacite() <= 0) {
            throw new IllegalArgumentException("La capacité du foyer doit être strictement positive");
        }
        if (command.getNouvelleCapacite() == capaciteFoyer) {
            throw new IllegalArgumentException("Le foyer " + nomFoyer + " a déjà une capacité de " + capaciteFoyer);
        }
        AggregateLifecycle.apply(new CapaciteFoyerModifieeEvent(foyerId, capaciteFoyer, command.getNouvelleCapacite()));
    }

    @CommandHandler
    public void handle(SupprimerFoyerCommand command) {
        AggregateLifecycle.apply(new FoyerSupprimeEvent(foyerId));
    }

    @EventSourcingHandler
    public void on(FoyerCreeEvent event) {
        this.foyerId = event.getFoyerId();
        this.nomFoyer = event.getNomFoyer();
        this.capaciteFoyer = event.getCapaciteFoyer();
    }

    @EventSourcingHandler
    public void on(CapaciteFoyerModifieeEvent event) {
        this.capaciteFoyer = event.getNouvelleCapacite();
    }

    @EventSourcingHandler
    public void on(FoyerSupprimeEvent event) {
        // toute commande suivante sur ce foyer sera refusée (AggregateDeletedException)
        AggregateLifecycle.markDeleted();
    }
}
