package tn.esprit.universiteservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.universiteservice.clients.FoyerClient;
import tn.esprit.universiteservice.dto.FoyerDTO;
import tn.esprit.universiteservice.dto.UniversiteDTO;
import tn.esprit.universiteservice.entities.Universite;
import tn.esprit.universiteservice.exceptions.*;
import tn.esprit.universiteservice.services.IUniversiteService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API REST de universite-service avec MockMvc "standalone" (sans contexte Spring, sans base ni Eureka) :
 * routes, JSON et codes HTTP de GlobalExceptionHandler (404, 409, 429, 503, 500).
 */
class UniversiteControllerTest {

    private IUniversiteService service;
    private FoyerClient foyerClient;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(IUniversiteService.class);
        foyerClient = mock(FoyerClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new UniversiteController(service), new LoadBalancingController(foyerClient))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ajouterListerLire() throws Exception {
        when(service.addUniversite(any(Universite.class))).thenReturn(dto(1L, "ESPRIT", null));
        when(service.getAllUniversites()).thenReturn(List.of(dto(1L, "ESPRIT", foyer(true))));
        when(service.getUniversiteById(1L)).thenReturn(dto(1L, "ESPRIT", foyer(true)));

        mvc.perform(post("/api/universites/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomUniversite\":\"ESPRIT\",\"adresse\":\"Ariana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUniversite").value(1));
        mvc.perform(get("/api/universites/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foyer.nomFoyer").value("Foyer A"));
        mvc.perform(get("/api/universites/get").param("idUniversite", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foyer.disponible").value(true));
    }

    @Test
    void modifierSupprimer() throws Exception {
        when(service.updateUniversite(any(Universite.class))).thenReturn(dto(1L, "ESPRIT Ghazela", null));

        mvc.perform(put("/api/universites/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUniversite\":1,\"nomUniversite\":\"ESPRIT Ghazela\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomUniversite").value("ESPRIT Ghazela"));
        mvc.perform(delete("/api/universites/delete/1")).andExpect(status().isOk());
        verify(service).deleteUniversite(1L);
    }

    @Test
    void affectationsDuFoyer() throws Exception {
        when(service.affecterFoyerAUniversite(1L, "ESPRIT")).thenReturn(dto(1L, "ESPRIT", foyer(true)));
        when(service.desaffecterFoyerAUniversite(1L)).thenReturn(dto(1L, "ESPRIT", null));
        when(service.affecterFoyerParId(2L, 5L)).thenReturn(dto(2L, "FST", foyer(true)));

        mvc.perform(put("/api/universites/affecter-foyer-universite").param("idFoyer", "1").param("nomUniversite", "ESPRIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foyer.idFoyer").value(1));
        mvc.perform(put("/api/universites/desaffecter-foyer-universite").param("idUniversite", "1"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/universites/2/foyer/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUniversite").value(2));
    }

    @Test
    void lbTestCompteLesReponsesParInstance() throws Exception {
        when(foyerClient.instance()).thenReturn(
                Map.of("instanceId", "foyer-service:8081"), Map.of("instanceId", "foyer-service:8083"));

        mvc.perform(get("/api/universites/lb-test").param("appels", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appels").value(4))
                .andExpect(jsonPath("$['repartition']['foyer-service:8081']").value(1))
                .andExpect(jsonPath("$['repartition']['foyer-service:8083']").value(3));
    }

    // ------------------------------------------------------------ GlobalExceptionHandler

    @Test
    void foyerIntrouvable404() throws Exception {
        when(service.affecterFoyerAUniversite(99L, "ESPRIT")).thenThrow(new FoyerIntrouvableException("Foyer 99 introuvable"));

        mvc.perform(put("/api/universites/affecter-foyer-universite").param("idFoyer", "99").param("nomUniversite", "ESPRIT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Foyer 99 introuvable"));
    }

    @Test
    void foyerDejaAffecte409() throws Exception {
        when(service.affecterFoyerAUniversite(1L, "FST")).thenThrow(new FoyerDejaAffecteException("déjà affecté"));

        mvc.perform(put("/api/universites/affecter-foyer-universite").param("idFoyer", "1").param("nomUniversite", "FST"))
                .andExpect(status().isConflict());
    }

    @Test
    void rateLimiter429() throws Exception {
        when(service.affecterFoyerAUniversite(1L, "ESPRIT")).thenThrow(new TropDeRequetesException("limite atteinte"));

        mvc.perform(put("/api/universites/affecter-foyer-universite").param("idFoyer", "1").param("nomUniversite", "ESPRIT"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void foyerServiceIndisponible503() throws Exception {
        when(service.affecterFoyerAUniversite(1L, "ESPRIT")).thenThrow(new ServiceIndisponibleException("foyer-service indisponible"));

        mvc.perform(put("/api/universites/affecter-foyer-universite").param("idFoyer", "1").param("nomUniversite", "ESPRIT"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void parametreManquant400EtErreurInattendue500() throws Exception {
        when(service.getAllUniversites()).thenThrow(new IllegalStateException("panne"));

        mvc.perform(get("/api/universites/get")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/universites/getAll")).andExpect(status().isInternalServerError());
    }

    private static UniversiteDTO dto(long id, String nom, FoyerDTO foyer) {
        return UniversiteDTO.builder().idUniversite(id).nomUniversite(nom).adresse("Tunis").foyer(foyer).build();
    }

    private static FoyerDTO foyer(boolean disponible) {
        return new FoyerDTO(1L, "Foyer A", 300, disponible);
    }
}
