package tn.esprit.foyerservice.cqrs.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Réponse de GetAllFoyersQuery. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoyersView {
    private List<FoyerView> foyers;
}
