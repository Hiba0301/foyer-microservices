package tn.esprit.blocservice.dto;

/** Réponse de l'API : le bloc et son foyer (lu dans foyer-service via Feign, ou valeur de secours). */
public record BlocResponse(long idBloc, String nomBloc, long capaciteBloc, FoyerDTO foyer) {
}
