package tn.esprit.universiteservice.clients;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import tn.esprit.universiteservice.exceptions.FoyerIntrouvableException;
import tn.esprit.universiteservice.exceptions.FoyerServiceException;

/**
 * Configuration propre au client Feign foyer-service (volontairement SANS @Configuration
 * pour ne pas s'appliquer aux autres clients).
 * Traduit les codes HTTP en exceptions métier / techniques.
 */
public class FoyerClientConfig {

    @Bean
    public ErrorDecoder foyerErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new FoyerIntrouvableException("Foyer introuvable dans foyer-service (" + methodKey + ")");
            }
            return new FoyerServiceException("foyer-service a répondu HTTP " + response.status() + " (" + methodKey + ")");
        };
    }
}
