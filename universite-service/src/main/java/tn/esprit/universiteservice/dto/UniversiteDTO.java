package tn.esprit.universiteservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Réponse de l'API : même forme qu'avant (le frontend Angular lit toujours universite.foyer.nomFoyer). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversiteDTO {
    private long idUniversite;
    private String nomUniversite;
    private String adresse;
    private FoyerDTO foyer;
}
