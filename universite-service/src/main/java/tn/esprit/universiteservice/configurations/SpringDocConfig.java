package tn.esprit.universiteservice.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI universiteOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("universite-service")
                .description("Microservice Université : appelle foyer-service via OpenFeign, protégé par Resilience4j")
                .version("1.0")
                .contact(new Contact().name("Hiba Chaari")));
    }
}
