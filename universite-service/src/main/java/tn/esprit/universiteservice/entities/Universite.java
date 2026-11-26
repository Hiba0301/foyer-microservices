package tn.esprit.universiteservice.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entité reprise du projet DevOps.
 * Avant : @OneToOne Foyer foyer (même base).
 * Maintenant : le foyer vit dans foyer-service (autre base) -> on ne garde que son identifiant.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Universite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    long idUniversite;

    String nomUniversite;

    String adresse;

    /** Référence vers un foyer de foyer-service (un foyer ne peut appartenir qu'à une université). */
    @Column(unique = true)
    Long idFoyer;
}
