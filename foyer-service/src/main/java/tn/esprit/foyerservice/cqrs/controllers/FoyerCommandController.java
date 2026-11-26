package tn.esprit.foyerservice.cqrs.controllers;

import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.foyerservice.cqrs.commands.CreerFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.ModifierCapaciteFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.SupprimerFoyerCommand;

import java.util.Map;
import java.util.UUID;

/** API des COMMANDES (écriture) : envoie des commandes sur le CommandGateway d'Axon. */
@RestController
@RequestMapping("api/foyers/cqrs/commands")
@RequiredArgsConstructor
public class FoyerCommandController {

    private static final String FOYER_ID = "foyerId";
    private static final String STATUT = "statut";

    private final CommandGateway commandGateway;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> creer(@RequestBody CreerFoyerRequest request) {
        String foyerId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new CreerFoyerCommand(foyerId, request.getNomFoyer(), request.getCapaciteFoyer()));
        return reponse(foyerId, "CreerFoyerCommand");
    }

    @PutMapping("{foyerId}/capacite")
    public Map<String, String> modifierCapacite(@PathVariable String foyerId, @RequestParam long valeur) {
        commandGateway.sendAndWait(new ModifierCapaciteFoyerCommand(foyerId, valeur));
        return reponse(foyerId, "ModifierCapaciteFoyerCommand");
    }

    @DeleteMapping("{foyerId}")
    public Map<String, String> supprimer(@PathVariable String foyerId) {
        commandGateway.sendAndWait(new SupprimerFoyerCommand(foyerId));
        return reponse(foyerId, "SupprimerFoyerCommand");
    }

    /** Même réponse JSON qu'avant : { "foyerId": ..., "statut": "commande ... traitée" }. */
    private static Map<String, String> reponse(String foyerId, String commande) {
        return Map.of(FOYER_ID, foyerId, STATUT, "commande " + commande + " traitée");
    }
}
