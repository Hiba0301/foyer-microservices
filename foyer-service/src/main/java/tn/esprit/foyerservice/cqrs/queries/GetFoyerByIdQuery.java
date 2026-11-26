package tn.esprit.foyerservice.cqrs.queries;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetFoyerByIdQuery {
    private String foyerId;
}
