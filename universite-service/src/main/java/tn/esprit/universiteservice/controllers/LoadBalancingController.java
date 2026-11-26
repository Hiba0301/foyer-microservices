package tn.esprit.universiteservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.universiteservice.clients.FoyerClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Démonstration de l'équilibrage de charge (Spring Cloud LoadBalancer, remplaçant de Ribbon) :
 * N appels Feign à foyer-service, on note quelle instance (port) a répondu à chacun.
 */
@RestController
@RequestMapping("api/universites")
@RequiredArgsConstructor
public class LoadBalancingController {

    private final FoyerClient foyerClient;

    @GetMapping("lb-test")
    public Map<String, Object> testerEquilibrage(@RequestParam(defaultValue = "6") int appels) {
        List<String> instances = new ArrayList<>();
        Map<String, Integer> repartition = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(appels, 50); i++) {
            String instance = foyerClient.instance().get("instanceId");
            instances.add(instance);
            repartition.merge(instance, 1, Integer::sum);
        }
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("appels", instances.size());
        resultat.put("ordre", instances);
        resultat.put("repartition", repartition);
        return resultat;
    }
}
