package tn.esprit.foyerservice.chaos;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Active / désactive les pannes simulées. À appeler DIRECTEMENT sur l'instance visée
 * (ex. http://localhost:8081/api/foyers/chaos?latenceMs=6000), pas via la gateway.
 */
@RestController
@RequestMapping("api/foyers/chaos")
@RequiredArgsConstructor
public class ChaosController {

    private final ChaosState chaos;

    @GetMapping
    public Map<String, Object> etat() {
        return Map.of(
                "latenceMs", chaos.getLatenceMs(),
                "reponsesCorrompues", chaos.isReponsesCorrompues(),
                "erreurServeur", chaos.isErreurServeur());
    }

    @PostMapping
    public Map<String, Object> configurer(@RequestParam(required = false) Long latenceMs,
                                          @RequestParam(required = false) Boolean corrompre,
                                          @RequestParam(required = false) Boolean erreur) {
        if (latenceMs != null) {
            chaos.setLatenceMs(Math.max(0, latenceMs));
        }
        if (corrompre != null) {
            chaos.setReponsesCorrompues(corrompre);
        }
        if (erreur != null) {
            chaos.setErreurServeur(erreur);
        }
        return etat();
    }

    @DeleteMapping
    public Map<String, Object> reinitialiser() {
        chaos.reinitialiser();
        return etat();
    }
}
