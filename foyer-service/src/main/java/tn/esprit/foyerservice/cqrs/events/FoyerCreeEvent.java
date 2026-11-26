package tn.esprit.foyerservice.cqrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Événement (fait passé, immuable dans l'event store) : un foyer a été créé. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoyerCreeEvent {
    private String foyerId;
    private String nomFoyer;
    private long capaciteFoyer;
}
