package tn.esprit.universiteservice.services;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.universiteservice.dto.FoyerDTO;
import tn.esprit.universiteservice.dto.FoyersResultat;
import tn.esprit.universiteservice.dto.UniversiteDTO;
import tn.esprit.universiteservice.entities.Universite;
import tn.esprit.universiteservice.exceptions.FoyerDejaAffecteException;
import tn.esprit.universiteservice.exceptions.FoyerIntrouvableException;
import tn.esprit.universiteservice.repositories.UniversiteRepository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UniversiteService implements IUniversiteService {

    UniversiteRepository universiteRepository;
    FoyerRemoteService foyerRemoteService;

    @Override
    public UniversiteDTO addUniversite(Universite universite) {
        log.info("Ajout d'une nouvelle université : {}", universite.getNomUniversite());
        Universite saved = universiteRepository.save(universite);
        return versDto(saved);
    }

    /** Un seul appel à foyer-service pour toute la liste (et non un appel par université). */
    @Override
    public List<UniversiteDTO> getAllUniversites() {
        log.info("Récupération de toutes les universités");
        List<Universite> universites = StreamSupport.stream(universiteRepository.findAll().spliterator(), false).toList();
        boolean auMoinsUnFoyer = universites.stream().anyMatch(u -> u.getIdFoyer() != null);
        if (!auMoinsUnFoyer) {
            return universites.stream().map(u -> versDto(u, null)).toList();
        }

        FoyersResultat resultat = foyerRemoteService.getAllFoyers();
        Map<Long, FoyerDTO> foyersParId = resultat.foyers().stream()
                .collect(Collectors.toMap(FoyerDTO::getIdFoyer, Function.identity(), (a, b) -> a));

        return universites.stream().map(u -> {
            if (u.getIdFoyer() == null) {
                return versDto(u, null);
            }
            FoyerDTO foyer = resultat.disponible()
                    ? foyersParId.get(u.getIdFoyer())                         // null si le foyer a été supprimé
                    : FoyerDTO.indisponible(u.getIdFoyer(), resultat.raison()); // mode dégradé (fallback)
            return versDto(u, foyer);
        }).toList();
    }

    @Override
    public UniversiteDTO getUniversiteById(long idUniversite) {
        return versDto(trouverUniversite(idUniversite));
    }

    @Override
    public void deleteUniversite(long idUniversite) {
        if (!universiteRepository.existsById(idUniversite)) {
            log.warn("Suppression demandée pour une université inexistante, id {}", idUniversite);
        }
        universiteRepository.deleteById(idUniversite);
        log.info("Suppression de l'université avec l'id {}", idUniversite);
    }

    /** Ne modifie que le nom et l'adresse : l'affectation du foyer passe par les endpoints dédiés. */
    @Override
    public UniversiteDTO updateUniversite(Universite universite) {
        log.info("Mise à jour de l'université avec l'id {}", universite.getIdUniversite());
        Universite existante = trouverUniversite(universite.getIdUniversite());
        existante.setNomUniversite(universite.getNomUniversite());
        existante.setAdresse(universite.getAdresse());
        return versDto(universiteRepository.save(existante));
    }

    @Override
    public UniversiteDTO affecterFoyerAUniversite(long idFoyer, String nomUniversite) {
        log.info("Affectation du foyer {} à l'université {}", idFoyer, nomUniversite);
        Universite universite = universiteRepository.findByNomUniversite(nomUniversite);
        if (universite == null) {
            throw new NoSuchElementException("Université introuvable avec le nom " + nomUniversite);
        }
        return affecter(universite, idFoyer);
    }

    /** Appelé par foyer-service (RestTemplate) lors de "ajouter-affecter". */
    @Override
    public UniversiteDTO affecterFoyerParId(long idUniversite, long idFoyer) {
        log.info("Affectation du foyer {} à l'université d'id {}", idFoyer, idUniversite);
        return affecter(trouverUniversite(idUniversite), idFoyer);
    }

    @Override
    public UniversiteDTO desaffecterFoyerAUniversite(long idUniversite) {
        log.info("Désaffectation du foyer de l'université {}", idUniversite);
        Universite universite = trouverUniversite(idUniversite);
        universite.setIdFoyer(null);
        return versDto(universiteRepository.save(universite), null);
    }

    // ------------------------------------------------------------------ Outils

    private UniversiteDTO affecter(Universite universite, long idFoyer) {
        // appel synchrone (Feign) protégé par Resilience4j : le foyer doit exister dans foyer-service
        FoyerDTO foyer = foyerRemoteService.verifierFoyer(idFoyer);
        universiteRepository.findByIdFoyer(idFoyer)
                .filter(autre -> autre.getIdUniversite() != universite.getIdUniversite())
                .ifPresent(autre -> {
                    throw new FoyerDejaAffecteException(
                            "Le foyer " + idFoyer + " est déjà affecté à l'université " + autre.getNomUniversite());
                });
        universite.setIdFoyer(idFoyer);
        return versDto(universiteRepository.save(universite), foyer);
    }

    private Universite trouverUniversite(long idUniversite) {
        return universiteRepository.findById(idUniversite).orElseThrow(() -> {
            log.error("Université introuvable avec l'id {}", idUniversite);
            return new NoSuchElementException("Université introuvable avec l'id " + idUniversite);
        });
    }

    /** Ajoute les informations du foyer (lues dans foyer-service) à l'université. */
    private UniversiteDTO versDto(Universite universite) {
        if (universite.getIdFoyer() == null) {
            return versDto(universite, null);
        }
        try {
            return versDto(universite, foyerRemoteService.getFoyer(universite.getIdFoyer()));
        } catch (FoyerIntrouvableException e) {
            // le foyer a été supprimé dans foyer-service entre-temps
            log.warn("Le foyer {} de l'université {} n'existe plus", universite.getIdFoyer(), universite.getIdUniversite());
            return versDto(universite, null);
        }
    }

    private UniversiteDTO versDto(Universite universite, FoyerDTO foyer) {
        return UniversiteDTO.builder()
                .idUniversite(universite.getIdUniversite())
                .nomUniversite(universite.getNomUniversite())
                .adresse(universite.getAdresse())
                .foyer(foyer)
                .build();
    }
}
