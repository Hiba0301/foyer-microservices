package tn.esprit.foyerservice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entité reprise du projet DevOps.
 * Dans l'architecture microservices, les relations JPA vers Universite et Bloc disparaissent :
 * chaque microservice a sa propre base. C'est universite-service qui garde l'identifiant du foyer (idFoyer).
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Foyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    long idFoyer;

    String nomFoyer;

    long capaciteFoyer;
}
