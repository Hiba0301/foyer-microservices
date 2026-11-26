package tn.esprit.foyerservice.cqrs.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tous les foyers dont la capacité est au moins égale à capaciteMin (0 = tous). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAllFoyersQuery {
    private long capaciteMin;
}
