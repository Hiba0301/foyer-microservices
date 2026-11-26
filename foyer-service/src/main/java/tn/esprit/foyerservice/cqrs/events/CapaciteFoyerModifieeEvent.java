package tn.esprit.foyerservice.cqrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapaciteFoyerModifieeEvent {
    private String foyerId;
    private long ancienneCapacite;
    private long nouvelleCapacite;
}
