package tn.esprit.foyerservice.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.foyerservice.entities.Foyer;
import tn.esprit.foyerservice.exceptions.ServiceIndisponibleException;
import tn.esprit.foyerservice.repositories.FoyerRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class FoyerService implements IFoyerService {

    private final FoyerRepository foyerRepository;
    private final RestTemplate restTemplate;
    private final String universiteServiceUrl;
    /** Métrique métier personnalisée, visible dans Prometheus : foyers_crees_total */
    private final Counter foyersCrees;

    public FoyerService(FoyerRepository foyerRepository,
                        RestTemplate restTemplate,
                        MeterRegistry meterRegistry,
                        @Value("${clients.universite.url:http://universite-service}") String universiteServiceUrl) {
        this.foyerRepository = foyerRepository;
        this.restTemplate = restTemplate;
        this.universiteServiceUrl = universiteServiceUrl;
        this.foyersCrees = Counter.builder("foyers.crees")
                .description("Nombre de foyers créés")
                .register(meterRegistry);
    }

    @Override
    public Foyer addFoyer(Foyer foyer) {
        log.info("Ajout d'un nouveau foyer : {}", foyer.getNomFoyer());
        Foyer saved = foyerRepository.save(foyer);
        foyersCrees.increment();
        log.debug("Foyer ajouté avec l'id {}", saved.getIdFoyer());
        return saved;
    }

    @Override
    public List<Foyer> getAllFoyers() {
        log.info("Récupération de tous les foyers");
        List<Foyer> foyers = (List<Foyer>) foyerRepository.findAll();
        log.debug("Nombre de foyers récupérés : {}", foyers.size());
        return foyers;
    }

    @Override
    public Foyer getFoyerById(long idFoyer) {
        log.debug("Recherche du foyer avec l'id {}", idFoyer);
        return foyerRepository.findById(idFoyer).orElseThrow(() -> {
            log.error("Foyer introuvable avec l'id {}", idFoyer);
            return new NoSuchElementException("Foyer introuvable avec l'id " + idFoyer);
        });
    }

    @Override
    public void deleteFoyer(long idFoyer) {
        if (!foyerRepository.existsById(idFoyer)) {
            log.warn("Suppression demandée pour un foyer inexistant, id {}", idFoyer);
        }
        foyerRepository.deleteById(idFoyer);
        log.info("Suppression du foyer avec l'id {}", idFoyer);
    }

    @Override
    public Foyer updateFoyer(Foyer foyer) {
        log.info("Mise à jour du foyer avec l'id {}", foyer.getIdFoyer());
        return foyerRepository.save(foyer);
    }

    /**
     * Avant (monolithe) : une seule transaction sur une seule base.
     * Maintenant : le foyer est enregistré ici, puis universite-service est appelé
     * (communication synchrone REST via RestTemplate @LoadBalanced).
     * Si l'appel échoue, on annule la création du foyer (compensation).
     */
    @Override
    public Foyer ajouterFoyerEtAffecterAUniversite(Foyer foyer, long idUniversite) {
        log.info("Ajout du foyer {} et affectation à l'université {}", foyer.getNomFoyer(), idUniversite);
        Foyer saved = addFoyer(foyer);
        try {
            restTemplate.put(universiteServiceUrl + "/api/universites/{idUniversite}/foyer/{idFoyer}",
                    null, idUniversite, saved.getIdFoyer());
            log.debug("Foyer {} affecté à l'université {}", saved.getIdFoyer(), idUniversite);
            return saved;
        } catch (HttpClientErrorException.NotFound e) {
            compenser(saved);
            throw new NoSuchElementException("Université introuvable avec l'id " + idUniversite);
        } catch (RestClientException e) {
            compenser(saved);
            throw new ServiceIndisponibleException(
                    "Affectation impossible : universite-service ne répond pas. Le foyer n'a pas été créé.", e);
        }
    }

    private void compenser(Foyer foyer) {
        log.warn("Compensation : suppression du foyer {} créé avant l'échec de l'affectation", foyer.getIdFoyer());
        foyerRepository.deleteById(foyer.getIdFoyer());
    }
}
