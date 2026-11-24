package tn.esprit.foyerservice.configurations;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Équivalent du "RibbonConfig" du TP4 : Ribbon n'existe plus avec Spring Boot 3,
 * @LoadBalanced utilise Spring Cloud LoadBalancer (round-robin entre les instances Eureka).
 * Construit avec RestTemplateBuilder => les appels sont tracés (Zipkin) automatiquement.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
