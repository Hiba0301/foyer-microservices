package tn.esprit.foyerservice.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Indique quelle instance a répondu : sert à montrer l'équilibrage de charge (8081 / 8083). */
@RestController
public class InstanceController {

    @Value("${spring.application.name}")
    private String application;

    @Value("${server.port}")
    private String port;

    @Value("${eureka.instance.instance-id:inconnu}")
    private String instanceId;

    @GetMapping("api/foyers/instance")
    public Map<String, String> instance() {
        return Map.of("application", application, "port", port, "instanceId", instanceId);
    }
}
