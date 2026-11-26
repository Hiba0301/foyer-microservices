package tn.esprit.foyerservice.cqrs.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/** Commande (intention d'écriture) : créer un foyer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreerFoyerCommand {
    @TargetAggregateIdentifier
    private String foyerId;
    private String nomFoyer;
    private long capaciteFoyer;
}
