package tn.esprit.foyerservice.chaos;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.foyerservice.entities.Foyer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Simulation de pannes (étape 7) : activation par l'API et effet de l'aspect sur les lectures. */
class ChaosTest {

    private ChaosState etat;
    private ChaosController controller;
    private ChaosAspect aspect;
    private ProceedingJoinPoint appel;

    @BeforeEach
    void setUp() {
        etat = new ChaosState();
        controller = new ChaosController(etat);
        aspect = new ChaosAspect(etat);
        appel = mock(ProceedingJoinPoint.class);
    }

    // ------------------------------------------------------------ ChaosController

    @Test
    void lesPannesSontDesactiveesParDefaut() {
        Map<String, Object> resultat = controller.etat();

        assertEquals(0L, resultat.get("latenceMs"));
        assertEquals(false, resultat.get("reponsesCorrompues"));
        assertEquals(false, resultat.get("erreurServeur"));
    }

    @Test
    void activationPuisReinitialisation() {
        controller.configurer(6000L, true, true);
        assertEquals(6000L, etat.getLatenceMs());
        assertTrue(etat.isReponsesCorrompues());
        assertTrue(etat.isErreurServeur());

        controller.reinitialiser();
        assertEquals(0L, etat.getLatenceMs());
        assertFalse(etat.isReponsesCorrompues());
        assertFalse(etat.isErreurServeur());
    }

    @Test
    void uneLatenceNegativeEstRameneeAZero() {
        controller.configurer(-500L, null, null);

        assertEquals(0L, etat.getLatenceMs());
    }

    // ------------------------------------------------------------ ChaosAspect

    @Test
    void sansPanneLaReponseEstInchangee() throws Throwable {
        Foyer foyer = foyer(1L, "Foyer A", 300);
        when(appel.proceed()).thenReturn(foyer);

        assertSame(foyer, aspect.appliquerPannes(appel));
    }

    @Test
    void erreurServeurSimulee() throws Throwable {
        etat.setErreurServeur(true);

        assertThrows(IllegalStateException.class, () -> aspect.appliquerPannes(appel));
        verify(appel, never()).proceed();
    }

    @Test
    void latenceSimulee() throws Throwable {
        etat.setLatenceMs(50);
        when(appel.proceed()).thenReturn("ok");

        long debut = System.currentTimeMillis();
        assertEquals("ok", aspect.appliquerPannes(appel));
        assertTrue(System.currentTimeMillis() - debut >= 50);
    }

    @Test
    void reponseCorrompuePourUnFoyer() throws Throwable {
        etat.setReponsesCorrompues(true);
        when(appel.proceed()).thenReturn(foyer(1L, "Foyer A", 300));

        Foyer resultat = (Foyer) aspect.appliquerPannes(appel);

        assertEquals(1L, resultat.getIdFoyer());
        assertNull(resultat.getNomFoyer());
        assertEquals(-1, resultat.getCapaciteFoyer());
    }

    @Test
    void reponseCorrompuePourUneListe() throws Throwable {
        etat.setReponsesCorrompues(true);
        when(appel.proceed()).thenReturn(List.of(foyer(1L, "Foyer A", 300), foyer(2L, "Foyer B", 200)));

        List<?> resultat = (List<?>) aspect.appliquerPannes(appel);

        assertEquals(2, resultat.size());
        assertTrue(resultat.stream().allMatch(f -> ((Foyer) f).getNomFoyer() == null));
    }

    @Test
    void uneReponseQuiNestPasUnFoyerNestPasModifiee() throws Throwable {
        etat.setReponsesCorrompues(true);
        when(appel.proceed()).thenReturn("autre");

        assertEquals("autre", aspect.appliquerPannes(appel));
    }

    private static Foyer foyer(long id, String nom, long capacite) {
        return Foyer.builder().idFoyer(id).nomFoyer(nom).capaciteFoyer(capacite).build();
    }
}
