package tn.esprit.foyerservice.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Réponses d'erreur JSON homogènes : { status, erreur, message, path, timestamp }. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> introuvable(NoSuchElementException e, HttpServletRequest req) {
        return reponse(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(AggregateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> agregatIntrouvable(AggregateNotFoundException e, HttpServletRequest req) {
        return reponse(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler({IllegalArgumentException.class, CommandExecutionException.class})
    public ResponseEntity<Map<String, Object>> requeteInvalide(RuntimeException e, HttpServletRequest req) {
        return reponse(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(ServiceIndisponibleException.class)
    public ResponseEntity<Map<String, Object>> indisponible(ServiceIndisponibleException e, HttpServletRequest req) {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erreurServeur(Exception e, HttpServletRequest req) {
        // exceptions Spring MVC (paramètre manquant, méthode non supportée...) : on garde leur code HTTP
        if (e instanceof ErrorResponse erreurSpring) {
            return reponse(HttpStatus.valueOf(erreurSpring.getStatusCode().value()), e.getMessage(), req);
        }
        log.error("Erreur interne sur {}", req.getRequestURI(), e);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), req);
    }

    private ResponseEntity<Map<String, Object>> reponse(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("status", status.value());
        corps.put("erreur", status.getReasonPhrase());
        corps.put("message", message);
        corps.put("path", req.getRequestURI());
        corps.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(corps);
    }
}
