package tn.esprit.blocservice.dto;

import java.util.List;

/** Liste des foyers + indication "réponse réelle" ou "fallback". */
public record FoyersResultat(List<FoyerDTO> foyers, boolean disponible, String raison) {

    public static FoyersResultat disponibles(List<FoyerDTO> foyers) {
        return new FoyersResultat(foyers, true, null);
    }

    public static FoyersResultat indisponibles(String raison) {
        return new FoyersResultat(List.of(), false, raison);
    }
}
