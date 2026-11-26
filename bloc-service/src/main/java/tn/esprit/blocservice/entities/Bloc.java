package tn.esprit.blocservice.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entité reprise du projet DevOps.
 * Avant : @ManyToOne Foyer foyer et @OneToMany Set<Chambre> chambres (même base).
 * Maintenant : le foyer vit dans foyer-service -> on garde son identifiant ;
 * les chambres ne font pas partie de ce module (la relation est retirée).
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Bloc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    long idBloc;

    String nomBloc;

    long capaciteBloc;

    /** Référence vers un foyer de foyer-service (plusieurs blocs par foyer). */
    Long idFoyer;
}
