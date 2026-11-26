package tn.esprit.blocservice.configurations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI blocOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("bloc-service")
                .description("Microservice Bloc : affectation à un foyer avec contrôle de capacité via OpenFeign + Resilience4j")
                .version("1.0")
                .contact(new Contact().name("Hiba Chaari")));
    }
}
