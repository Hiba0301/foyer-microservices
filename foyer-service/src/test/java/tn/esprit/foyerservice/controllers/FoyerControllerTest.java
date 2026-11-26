package tn.esprit.foyerservice.controllers;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.foyerservice.entities.Foyer;
import tn.esprit.foyerservice.exceptions.GlobalExceptionHandler;
import tn.esprit.foyerservice.exceptions.ServiceIndisponibleException;
import tn.esprit.foyerservice.services.IFoyerService;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API REST de foyer-service avec MockMvc en mode "standalone" : pas de contexte Spring, pas de base,
 * pas d'Eureka. On vérifie les routes, le JSON et les codes HTTP renvoyés par GlobalExceptionHandler.
 */
class FoyerControllerTest {

    private IFoyerService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(IFoyerService.class);
        InstanceController instance = new InstanceController();
        ReflectionTestUtils.setField(instance, "application", "foyer-service");
        ReflectionTestUtils.setField(instance, "port", "8081");
        ReflectionTestUtils.setField(instance, "instanceId", "foyer-service:8081");
        mvc = MockMvcBuilders.standaloneSetup(new FoyerController(service), instance)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ajouter() throws Exception {
        when(service.addFoyer(any(Foyer.class))).thenReturn(foyer(1L, "Foyer A", 300));

        mvc.perform(post("/api/foyers/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomFoyer\":\"Foyer A\",\"capaciteFoyer\":300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFoyer").value(1))
                .andExpect(jsonPath("$.nomFoyer").value("Foyer A"));
    }

    @Test
    void listerEtLire() throws Exception {
        when(service.getAllFoyers()).thenReturn(List.of(foyer(1L, "Foyer A", 300), foyer(2L, "Foyer B", 200)));
        when(service.getFoyerById(2L)).thenReturn(foyer(2L, "Foyer B", 200));

        mvc.perform(get("/api/foyers/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/foyers/get").param("idFoyer", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaciteFoyer").value(200));
    }

    @Test
    void modifierEtSupprimer() throws Exception {
        when(service.updateFoyer(any(Foyer.class))).thenReturn(foyer(1L, "Foyer A", 350));

        mvc.perform(put("/api/foyers/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idFoyer\":1,\"nomFoyer\":\"Foyer A\",\"capaciteFoyer\":350}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaciteFoyer").value(350));
        mvc.perform(delete("/api/foyers/delete/1"))
                .andExpect(status().isOk());
        verify(service).deleteFoyer(1L);
    }

    @Test
    void ajouterEtAffecterAUneUniversite() throws Exception {
        when(service.ajouterFoyerEtAffecterAUniversite(any(Foyer.class), eq(2L))).thenReturn(foyer(5L, "Foyer FST", 200));

        mvc.perform(post("/api/foyers/ajouter-affecter").param("idUniversite", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomFoyer\":\"Foyer FST\",\"capaciteFoyer\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFoyer").value(5));
    }

    @Test
    void instanceQuiARepondu() throws Exception {
        mvc.perform(get("/api/foyers/instance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.port").value("8081"))
                .andExpect(jsonPath("$.instanceId").value("foyer-service:8081"));
    }

    // ------------------------------------------------------------ GlobalExceptionHandler

    @Test
    void foyerInexistant404() throws Exception {
        when(service.getFoyerById(99L)).thenThrow(new NoSuchElementException("Foyer introuvable avec l'id 99"));

        mvc.perform(get("/api/foyers/get").param("idFoyer", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Foyer introuvable avec l'id 99"))
                .andExpect(jsonPath("$.path").value("/api/foyers/get"));
    }

    @Test
    void agregatInexistant404() throws Exception {
        when(service.getFoyerById(98L)).thenThrow(new AggregateNotFoundException("f-98", "agrégat introuvable"));

        mvc.perform(get("/api/foyers/get").param("idFoyer", "98"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requeteInvalide400() throws Exception {
        when(service.getFoyerById(1L)).thenThrow(new IllegalArgumentException("capacité négative"));
        when(service.getFoyerById(2L)).thenThrow(new CommandExecutionException("règle métier", null));

        mvc.perform(get("/api/foyers/get").param("idFoyer", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/foyers/get").param("idFoyer", "2")).andExpect(status().isBadRequest());
    }

    @Test
    void parametreManquantGardeLeCodeDeSpring() throws Exception {
        mvc.perform(get("/api/foyers/get"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void universiteServiceIndisponible503() throws Exception {
        when(service.ajouterFoyerEtAffecterAUniversite(any(Foyer.class), eq(3L)))
                .thenThrow(new ServiceIndisponibleException("universite-service ne répond pas", null));

        mvc.perform(post("/api/foyers/ajouter-affecter").param("idUniversite", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomFoyer\":\"Foyer X\",\"capaciteFoyer\":10}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void erreurInattendue500() throws Exception {
        when(service.getAllFoyers()).thenThrow(new IllegalStateException("panne"));

        mvc.perform(get("/api/foyers/getAll"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("panne"));
    }

    private static Foyer foyer(long id, String nom, long capacite) {
        return Foyer.builder().idFoyer(id).nomFoyer(nom).capaciteFoyer(capacite).build();
    }
}
