package tn.esprit.blocservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.blocservice.dto.FoyerDTO;

import java.util.List;
import java.util.Map;

/**
 * Client REST déclaratif vers foyer-service (communication synchrone).
 * name = nom Eureka du service => adresse résolue par Eureka + Spring Cloud LoadBalancer (round-robin).
 * contextId fixe : la config spring.cloud.openfeign.client.config.foyer-service (timeouts) s'applique
 * même quand on change clients.foyer.name pour simuler une URL mal configurée.
 */
@FeignClient(name = "${clients.foyer.name:foyer-service}", contextId = "foyer-service",
        configuration = FoyerClientConfig.class)
public interface FoyerClient {

    @GetMapping("/api/foyers/get")
    FoyerDTO getFoyer(@RequestParam("idFoyer") long idFoyer);

    @GetMapping("/api/foyers/getAll")
    List<FoyerDTO> getAllFoyers();

    @GetMapping("/api/foyers/instance")
    Map<String, String> instance();
}
