package tn.esprit.foyerservice.cqrs.controllers;

import lombok.RequiredArgsConstructor;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.foyerservice.cqrs.queries.FoyerView;
import tn.esprit.foyerservice.cqrs.queries.FoyersView;
import tn.esprit.foyerservice.cqrs.queries.GetAllFoyersQuery;
import tn.esprit.foyerservice.cqrs.queries.GetFoyerByIdQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API des REQUÊTES (lecture) : passe par le QueryGateway d'Axon, qui interroge la projection. */
@RestController
@RequestMapping("api/foyers/cqrs/queries")
@RequiredArgsConstructor
public class FoyerQueryController {

    private final QueryGateway queryGateway;
    private final EventStore eventStore;

    @GetMapping
    public List<FoyerView> lister(@RequestParam(defaultValue = "0") long capaciteMin) {
        return queryGateway.query(new GetAllFoyersQuery(capaciteMin),
                ResponseTypes.instanceOf(FoyersView.class)).join().getFoyers();
    }

    @GetMapping("{foyerId}")
    public ResponseEntity<FoyerView> trouver(@PathVariable String foyerId) {
        FoyerView vue = queryGateway.query(new GetFoyerByIdQuery(foyerId),
                ResponseTypes.instanceOf(FoyerView.class)).join();
        return vue == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(vue);
    }

    /** Historique complet du foyer lu dans l'event store (Event Sourcing). */
    @GetMapping("{foyerId}/evenements")
    public List<Map<String, Object>> historique(@PathVariable String foyerId) {
        return eventStore.readEvents(foyerId).asStream()
                .map(message -> {
                    Map<String, Object> evenement = new LinkedHashMap<>();
                    evenement.put("sequence", message.getSequenceNumber());
                    evenement.put("type", message.getPayloadType().getSimpleName());
                    evenement.put("date", message.getTimestamp().toString());
                    evenement.put("donnees", message.getPayload());
                    return evenement;
                })
                .toList();
    }
}
