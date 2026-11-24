package tn.esprit.foyerservice.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI foyerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("foyer-service")
                .description("Microservice Foyer : CRUD (repris du projet DevOps), CQRS/Axon et simulation de pannes")
                .version("1.0")
                .contact(new Contact().name("Hiba Chaari")));
    }

    @Bean
    public GroupedOpenApi foyerCrudApi() {
        return GroupedOpenApi.builder().group("1. Foyer CRUD")
                .pathsToMatch("/api/foyers/**")
                .pathsToExclude("/api/foyers/cqrs/**", "/api/foyers/chaos/**")
                .build();
    }

    @Bean
    public GroupedOpenApi foyerCqrsApi() {
        return GroupedOpenApi.builder().group("2. Foyer CQRS (Axon)")
                .pathsToMatch("/api/foyers/cqrs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chaosApi() {
        return GroupedOpenApi.builder().group("3. Simulation de pannes")
                .pathsToMatch("/api/foyers/chaos/**")
                .build();
    }
}
