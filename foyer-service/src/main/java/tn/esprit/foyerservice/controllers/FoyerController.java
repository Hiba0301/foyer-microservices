package tn.esprit.foyerservice.controllers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import tn.esprit.foyerservice.entities.Foyer;
import tn.esprit.foyerservice.services.IFoyerService;

import java.util.List;

/**
 * Mêmes endpoints que dans le projet DevOps (le frontend Angular reste compatible).
 * Le CORS est désormais géré une seule fois par l'API Gateway.
 */
@RestController
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("api/foyers")
public class FoyerController {
    IFoyerService foyerService;

    @PostMapping("add")
    public Foyer addingFoyer(@RequestBody Foyer foyer) {
        return foyerService.addFoyer(foyer);
    }

    @GetMapping("getAll")
    public List<Foyer> gettingAllFoyer() {
        return foyerService.getAllFoyers();
    }

    @GetMapping("get")
    public Foyer gettingFoyer(@RequestParam("idFoyer") long idFoyer) {
        return foyerService.getFoyerById(idFoyer);
    }

    @DeleteMapping("delete/{idFoyer}")
    public void deletingFoyer(@PathVariable("idFoyer") long idFoyer) {
        foyerService.deleteFoyer(idFoyer);
    }

    @PutMapping("update")
    public Foyer updatingFoyer(@RequestBody Foyer foyer) {
        return foyerService.updateFoyer(foyer);
    }

    @PostMapping("ajouter-affecter")
    public Foyer ajouterFoyerEtAffecterAUniversite(@RequestBody Foyer foyer, @RequestParam("idUniversite") long idUniversite) {
        return foyerService.ajouterFoyerEtAffecterAUniversite(foyer, idUniversite);
    }
}
