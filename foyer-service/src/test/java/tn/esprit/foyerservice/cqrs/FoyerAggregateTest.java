package tn.esprit.foyerservice.cqrs;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.foyerservice.cqrs.aggregate.FoyerAggregate;
import tn.esprit.foyerservice.cqrs.commands.CreerFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.ModifierCapaciteFoyerCommand;
import tn.esprit.foyerservice.cqrs.commands.SupprimerFoyerCommand;
import tn.esprit.foyerservice.cqrs.events.CapaciteFoyerModifieeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerCreeEvent;
import tn.esprit.foyerservice.cqrs.events.FoyerSupprimeEvent;

/** Tests "given / when / then" de l'agrégat, sans base ni Axon Server (Axon Test Fixture). */
class FoyerAggregateTest {

    private FixtureConfiguration<FoyerAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(FoyerAggregate.class);
    }

    @Test
    void creerFoyerPublieFoyerCreeEvent() {
        fixture.givenNoPriorActivity()
                .when(new CreerFoyerCommand("f1", "Foyer El Amal", 300))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new FoyerCreeEvent("f1", "Foyer El Amal", 300));
    }

    @Test
    void creerFoyerAvecCapaciteNegativeEstRefuse() {
        fixture.givenNoPriorActivity()
                .when(new CreerFoyerCommand("f1", "Foyer", -5))
                .expectException(IllegalArgumentException.class)
                .expectNoEvents();
    }

    @Test
    void creerFoyerSansNomEstRefuse() {
        fixture.givenNoPriorActivity()
                .when(new CreerFoyerCommand("f1", " ", 100))
                .expectException(IllegalArgumentException.class);
    }

    @Test
    void modifierCapacitePublieLAncienneEtLaNouvelleValeur() {
        fixture.given(new FoyerCreeEvent("f1", "Foyer El Amal", 300))
                .when(new ModifierCapaciteFoyerCommand("f1", 350))
                .expectEvents(new CapaciteFoyerModifieeEvent("f1", 300, 350));
    }

    @Test
    void modifierAvecLaMemeCapaciteEstRefuse() {
        fixture.given(new FoyerCreeEvent("f1", "Foyer El Amal", 300))
                .when(new ModifierCapaciteFoyerCommand("f1", 300))
                .expectException(IllegalArgumentException.class);
    }

    @Test
    void supprimerMarqueLAgregatCommeSupprime() {
        fixture.given(new FoyerCreeEvent("f1", "Foyer El Amal", 300))
                .when(new SupprimerFoyerCommand("f1"))
                .expectEvents(new FoyerSupprimeEvent("f1"))
                .expectMarkedDeleted();
    }
}
