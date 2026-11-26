package tn.esprit.foyerservice.cqrs.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifierCapaciteFoyerCommand {
    @TargetAggregateIdentifier
    private String foyerId;
    private long nouvelleCapacite;
}
