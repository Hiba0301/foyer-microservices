package tn.esprit.universiteservice.controllers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import tn.esprit.universiteservice.dto.UniversiteDTO;
import tn.esprit.universiteservice.entities.Universite;
import tn.esprit.universiteservice.services.IUniversiteService;

import java.util.List;

/**
 * Endpoints du projet DevOps (chemin corrigé : "univeristes" -> "universites").
 * Le CORS est géré par l'API Gateway.
 */
@RestController
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("api/universites")
public class UniversiteController {
    IUniversiteService universiteService;

    @PostMapping("add")
    public UniversiteDTO addingUniversite(@RequestBody Universite universite) {
        return universiteService.addUniversite(universite);
    }

    @GetMapping("getAll")
    public List<UniversiteDTO> gettingAllUniversite() {
        return universiteService.getAllUniversites();
    }

    @GetMapping("get")
    public UniversiteDTO gettingUniversite(@RequestParam("idUniversite") long idUniversite) {
        return universiteService.getUniversiteById(idUniversite);
    }

    @DeleteMapping("delete/{idUniversite}")
    public void deletingUniversite(@PathVariable("idUniversite") long idUniversite) {
        universiteService.deleteUniversite(idUniversite);
    }

    @PutMapping("update")
    public UniversiteDTO updatingUniversite(@RequestBody Universite universite) {
        return universiteService.updateUniversite(universite);
    }

    @PutMapping("affecter-foyer-universite")
    public UniversiteDTO affecterFoyerAUniversite(@RequestParam("idFoyer") long idFoyer,
                                                  @RequestParam("nomUniversite") String nomUniversite) {
        return universiteService.affecterFoyerAUniversite(idFoyer, nomUniversite);
    }

    @PutMapping("desaffecter-foyer-universite")
    public UniversiteDTO desaffecterFoyerAUniversite(@RequestParam("idUniversite") long idUniversite) {
        return universiteService.desaffecterFoyerAUniversite(idUniversite);
    }

    /** Utilisé par foyer-service (RestTemplate @LoadBalanced) pour "ajouter-affecter". */
    @PutMapping("{idUniversite}/foyer/{idFoyer}")
    public UniversiteDTO affecterFoyerParId(@PathVariable("idUniversite") long idUniversite,
                                            @PathVariable("idFoyer") long idFoyer) {
        return universiteService.affecterFoyerParId(idUniversite, idFoyer);
    }
}
