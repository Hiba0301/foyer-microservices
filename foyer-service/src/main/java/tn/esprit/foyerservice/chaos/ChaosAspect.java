package tn.esprit.foyerservice.chaos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import tn.esprit.foyerservice.entities.Foyer;

import java.util.List;

/** Applique les pannes simulées aux lectures de FoyerController (get, getAll). */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ChaosAspect {

    private final ChaosState chaos;

    @Around("execution(* tn.esprit.foyerservice.controllers.FoyerController.getting*(..))")
    public Object appliquerPannes(ProceedingJoinPoint pjp) throws Throwable {
        if (chaos.getLatenceMs() > 0) {
            log.warn("[CHAOS] latence simulée de {} ms", chaos.getLatenceMs());
            Thread.sleep(chaos.getLatenceMs());
        }
        if (chaos.isErreurServeur()) {
            log.warn("[CHAOS] erreur serveur simulée");
            throw new IllegalStateException("Panne simulée de foyer-service");
        }
        Object resultat = pjp.proceed();
        if (!chaos.isReponsesCorrompues()) {
            return resultat;
        }
        log.warn("[CHAOS] réponse corrompue renvoyée");
        if (resultat instanceof Foyer foyer) {
            return corrompre(foyer);
        }
        if (resultat instanceof List<?> liste) {
            return liste.stream().map(o -> o instanceof Foyer f ? corrompre(f) : o).toList();
        }
        return resultat;
    }

    private Foyer corrompre(Foyer foyer) {
        return Foyer.builder()
                .idFoyer(foyer.getIdFoyer())
                .nomFoyer(null)
                .capaciteFoyer(-1)
                .build();
    }
}
