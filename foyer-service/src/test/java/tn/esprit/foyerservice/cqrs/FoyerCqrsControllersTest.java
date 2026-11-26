package tn.esprit.foyerservice.cqrs;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import tn.esprit.foyerservice.cqrs.commands.CreerFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.ModifierCapaciteFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.SupprimerFoyerCommand;
import tn.esprit.foyerservice.cqrs.controllers.CreerFoyerRequest;
import tn.esprit.foyerservice.cqrs.controllers.FoyerCommandController;
import tn.esprit.foyerservice.cqrs.controllers.FoyerQueryController;
import tn.esprit.foyerservice.cqrs.events.CapaciteFoyerModifieeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerCreeEvent;
import tn.esprit.foyerservice.cqrs.queries.FoyerView;
import tn.esprit.foyerservice.cqrs.queries.FoyersView;
import tn.esprit.foyerservice.cqrs.queries.GetAllFoyersQuery;
import tn.esprit.foyerservice.cqrs.queries.GetFoyerByIdQuery;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** API CQRS : les commandes partent sur le CommandGateway, les lectures sur le QueryGateway, l'historique vient de l'event store. */
class FoyerCqrsControllersTest {

    private CommandGateway commandGateway;
    private QueryGateway queryGateway;
    private EventStore eventStore;
    private FoyerCommandController commandes;
    private FoyerQueryController requetes;

    @BeforeEach
    void setUp() {
        commandGateway = mock(CommandGateway.class);
        queryGateway = mock(QueryGateway.class);
        eventStore = mock(EventStore.class);
        commandes = new FoyerCommandController(commandGateway);
        requetes = new FoyerQueryController(queryGateway, eventStore);
    }

    // ------------------------------------------------------------ commandes (écriture)

    @Test
    void creerEnvoieUneCommandeAvecUnNouvelIdentifiant() {
        Map<String, String> reponse = commandes.creer(new CreerFoyerRequest("Foyer Ennasr", 120));

        CreerFoyerCommand envoyee = (CreerFoyerCommand) captureCommande();
        assertEquals(reponse.get("foyerId"), envoyee.getFoyerId());
        assertEquals("Foyer Ennasr", envoyee.getNomFoyer());
        assertEquals(120, envoyee.getCapaciteFoyer());
    }

    @Test
    void modifierLaCapacite() {
        commandes.modifierCapacite("f-1", 180);

        ModifierCapaciteFoyerCommand envoyee = (ModifierCapaciteFoyerCommand) captureCommande();
        assertEquals("f-1", envoyee.getFoyerId());
        assertEquals(180, envoyee.getNouvelleCapacite());
    }

    @Test
    void supprimer() {
        Map<String, String> reponse = commandes.supprimer("f-1");

        assertEquals("f-1", ((SupprimerFoyerCommand) captureCommande()).getFoyerId());
        assertEquals("f-1", reponse.get("foyerId"));
    }

    // ------------------------------------------------------------ requêtes (lecture)

    @Test
    void listerPasseParLeQueryGateway() {
        List<FoyerView> vues = List.of(FoyerView.builder().foyerId("f-1").capaciteFoyer(200).build());
        doReturn(CompletableFuture.completedFuture(new FoyersView(vues)))
                .when(queryGateway).query(any(GetAllFoyersQuery.class), any(ResponseType.class));

        assertEquals(vues, requetes.lister(150));
    }

    @Test
    void trouverRenvoie200Ou404() {
        FoyerView vue = FoyerView.builder().foyerId("f-1").build();
        doReturn(CompletableFuture.completedFuture(vue))
                .when(queryGateway).query(eq(new GetFoyerByIdQuery("f-1")), any(ResponseType.class));
        doReturn(CompletableFuture.completedFuture(null))
                .when(queryGateway).query(eq(new GetFoyerByIdQuery("absent")), any(ResponseType.class));

        ResponseEntity<FoyerView> trouve = requetes.trouver("f-1");
        ResponseEntity<FoyerView> absent = requetes.trouver("absent");

        assertEquals(200, trouve.getStatusCode().value());
        assertSame(vue, trouve.getBody());
        assertEquals(404, absent.getStatusCode().value());
    }

    @Test
    void historiqueLuDansLEventStore() {
        when(eventStore.readEvents("f-1")).thenReturn(DomainEventStream.of(
                new GenericDomainEventMessage<>("FoyerAggregate", "f-1", 0, new FoyerCreeEvent("f-1", "Foyer Ennasr", 120)),
                new GenericDomainEventMessage<>("FoyerAggregate", "f-1", 1, new CapaciteFoyerModifieeEvent("f-1", 120, 180))));

        List<Map<String, Object>> historique = requetes.historique("f-1");

        assertEquals(2, historique.size());
        assertEquals(0L, historique.get(0).get("sequence"));
        assertEquals("FoyerCreeEvent", historique.get(0).get("type"));
        assertEquals("CapaciteFoyerModifieeEvent", historique.get(1).get("type"));
    }

    private Object captureCommande() {
        org.mockito.ArgumentCaptor<Object> commande = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(commandGateway).sendAndWait(commande.capture());
        return commande.getValue();
    }

    private static <T> T eq(T valeur) {
        return org.mockito.ArgumentMatchers.eq(valeur);
    }
}
