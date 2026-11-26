package tn.esprit.foyerservice.cqrs.controllers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreerFoyerRequest {
    private String nomFoyer;
    private long capaciteFoyer;
}
