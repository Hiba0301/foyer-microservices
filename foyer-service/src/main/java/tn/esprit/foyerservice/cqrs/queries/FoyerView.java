package tn.esprit.foyerservice.cqrs.queries;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Côté LECTURE (Query) : modèle de lecture stocké dans MySQL (table foyer_view),
 * mis à jour par la projection à partir des événements.
 */
@Entity
@Table(name = "foyer_view")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoyerView {
    @Id
    private String foyerId;
    private String nomFoyer;
    private long capaciteFoyer;
    /** Dates ISO-8601 (texte) : sérialisation simple, quel que soit le sérialiseur Axon. */
    private String dateCreation;
    private String derniereModification;
    private int nombreModifications;
}
