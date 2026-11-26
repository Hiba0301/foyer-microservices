package tn.esprit.universiteservice.clients;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import tn.esprit.universiteservice.exceptions.FoyerIntrouvableException;
import tn.esprit.universiteservice.exceptions.FoyerServiceException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Traduction des codes HTTP de foyer-service : 404 = erreur métier, le reste = panne technique (retry / circuit breaker). */
class FoyerClientConfigTest {

    private final ErrorDecoder decodeur = new FoyerClientConfig().foyerErrorDecoder();

    @Test
    void reponse404DevientFoyerIntrouvable() {
        assertInstanceOf(FoyerIntrouvableException.class, decodeur.decode("FoyerClient#getFoyer(long)", reponse(404)));
    }

    @Test
    void reponse500Ou503DevientUnePanneTechnique() {
        assertInstanceOf(FoyerServiceException.class, decodeur.decode("FoyerClient#getFoyer(long)", reponse(500)));
        assertInstanceOf(FoyerServiceException.class, decodeur.decode("FoyerClient#getAllFoyers()", reponse(503)));
    }

    private static Response reponse(int status) {
        Request requete = Request.create(Request.HttpMethod.GET, "http://foyer-service/api/foyers/get",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).reason("test").request(requete).headers(Map.of()).build();
    }
}
