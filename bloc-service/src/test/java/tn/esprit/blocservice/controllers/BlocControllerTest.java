package tn.esprit.blocservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.blocservice.dto.BlocDTO;
import tn.esprit.blocservice.dto.BlocResponse;
import tn.esprit.blocservice.dto.FoyerDTO;
import tn.esprit.blocservice.exceptions.*;
import tn.esprit.blocservice.services.IBlocService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API REST de bloc-service avec MockMvc "standalone" : routes, JSON et codes HTTP
 * de GlobalExceptionHandler, dont le 409 quand la capacité du foyer serait dépassée.
 */
class BlocControllerTest {

    private IBlocService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(IBlocService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BlocController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ajouterListerLire() throws Exception {
        when(service.addBloc(any(BlocDTO.class))).thenReturn(bloc(1L, null));
        when(service.getAllBlocs()).thenReturn(List.of(bloc(1L, foyer(true))));
        when(service.getBlocById(1L)).thenReturn(bloc(1L, foyer(true)));

        mvc.perform(post("/api/blocs/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomBloc\":\"Bloc A\",\"capaciteBloc\":150}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idBloc").value(1));
        mvc.perform(get("/api/blocs/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foyer.nomFoyer").value("Foyer A"));
        mvc.perform(get("/api/blocs/get").param("idBloc", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaciteBloc").value(150));
    }

    @Test
    void modifierSupprimer() throws Exception {
        when(service.updateBloc(any(BlocDTO.class))).thenReturn(bloc(1L, null));

        mvc.perform(put("/api/blocs/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idBloc\":1,\"nomBloc\":\"Bloc A\",\"capaciteBloc\":150}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/blocs/delete/1")).andExpect(status().isOk());
        verify(service).deleteBloc(1L);
    }

    @Test
    void affectationsEtBlocsParFoyer() throws Exception {
        when(service.affecterBlocAFoyer(1L, 1L)).thenReturn(bloc(1L, foyer(true)));
        when(service.desaffecterBlocDuFoyer(1L)).thenReturn(bloc(1L, null));
        when(service.getBlocsParFoyer(1L)).thenReturn(List.of(bloc(1L, foyer(true)), bloc(2L, foyer(true))));

        mvc.perform(put("/api/blocs/affecter-bloc-foyer").param("idBloc", "1").param("idFoyer", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foyer.idFoyer").value(1));
        mvc.perform(put("/api/blocs/desaffecter-bloc-foyer").param("idBloc", "1"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/blocs/par-foyer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ------------------------------------------------------------ GlobalExceptionHandler

    @Test
    void capaciteDepassee409() throws Exception {
        when(service.affecterBlocAFoyer(3L, 1L)).thenThrow(new CapaciteDepasseeException("300 déjà occupés + 100 > 300"));

        mvc.perform(put("/api/blocs/affecter-bloc-foyer").param("idBloc", "3").param("idFoyer", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("300 déjà occupés + 100 > 300"));
    }

    @Test
    void foyerOuBlocIntrouvable404() throws Exception {
        when(service.affecterBlocAFoyer(3L, 99L)).thenThrow(new FoyerIntrouvableException("Foyer 99 introuvable"));

        mvc.perform(put("/api/blocs/affecter-bloc-foyer").param("idBloc", "3").param("idFoyer", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void capaciteInvalide400() throws Exception {
        when(service.addBloc(any(BlocDTO.class))).thenThrow(new IllegalArgumentException("capacité négative"));

        mvc.perform(post("/api/blocs/add").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomBloc\":\"Bloc X\",\"capaciteBloc\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimiter429EtFoyerServiceIndisponible503() throws Exception {
        when(service.affecterBlocAFoyer(1L, 1L)).thenThrow(new TropDeRequetesException("limite atteinte"));
        when(service.affecterBlocAFoyer(2L, 1L)).thenThrow(new ServiceIndisponibleException("foyer-service indisponible"));

        mvc.perform(put("/api/blocs/affecter-bloc-foyer").param("idBloc", "1").param("idFoyer", "1"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(put("/api/blocs/affecter-bloc-foyer").param("idBloc", "2").param("idFoyer", "1"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void parametreManquant400EtErreurInattendue500() throws Exception {
        when(service.getAllBlocs()).thenThrow(new IllegalStateException("panne"));

        mvc.perform(get("/api/blocs/get")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/blocs/getAll")).andExpect(status().isInternalServerError());
    }

    private static BlocResponse bloc(long id, FoyerDTO foyer) {
        return new BlocResponse(id, "Bloc A", 150, foyer);
    }

    private static FoyerDTO foyer(boolean disponible) {
        return new FoyerDTO(1L, "Foyer A", 300, disponible);
    }
}
