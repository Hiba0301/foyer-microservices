package tn.esprit.blocservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Foyer tel que renvoyé par foyer-service (copie locale, pas d'entité partagée entre services). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoyerDTO {
    private Long idFoyer;
    private String nomFoyer;
    private long capaciteFoyer;
    /** false quand la valeur vient d'un fallback (foyer-service indisponible). Absent de la réponse de foyer-service => true. */
    private boolean disponible = true;

    /** Valeur "dégradée" renvoyée par les fallbacks. */
    public static FoyerDTO indisponible(Long idFoyer, String raison) {
        return new FoyerDTO(idFoyer, "Foyer #" + idFoyer + " (indisponible : " + raison + ")", 0, false);
    }
}
