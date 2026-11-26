package tn.esprit.foyerservice.cqrs.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupprimerFoyerCommand {
    @TargetAggregateIdentifier
    private String foyerId;
}
