package tn.esprit.blocservice.services;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.blocservice.dto.BlocDTO;
import tn.esprit.blocservice.dto.BlocResponse;
import tn.esprit.blocservice.dto.FoyerDTO;
import tn.esprit.blocservice.dto.FoyersResultat;
import tn.esprit.blocservice.entities.Bloc;
import tn.esprit.blocservice.exceptions.CapaciteDepasseeException;
import tn.esprit.blocservice.exceptions.FoyerIntrouvableException;
import tn.esprit.blocservice.repositories.BlocRepository;

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
public class BlocService implements IBlocService {

    BlocRepository blocRepository;
    FoyerRemoteService foyerRemoteService;

    @Override
    public BlocResponse addBloc(BlocDTO dto) {
        log.info("Ajout du bloc : {}", dto.nomBloc());
        verifierCapacitePositive(dto.capaciteBloc());
        Bloc saved = blocRepository.save(Bloc.builder()
                .nomBloc(dto.nomBloc())
                .capaciteBloc(dto.capaciteBloc())
                .build());
        log.debug("Bloc enregistré avec l'id {}", saved.getIdBloc());
        return versReponse(saved, null);
    }

    /** Un seul appel à foyer-service pour toute la liste. */
    @Override
    public List<BlocResponse> getAllBlocs() {
        log.debug("Recherche de tous les blocs");
        List<Bloc> blocs = StreamSupport.stream(blocRepository.findAll().spliterator(), false).toList();
        if (blocs.stream().noneMatch(b -> b.getIdFoyer() != null)) {
            return blocs.stream().map(b -> versReponse(b, null)).toList();
        }
        FoyersResultat resultat = foyerRemoteService.getAllFoyers();
        Map<Long, FoyerDTO> foyersParId = resultat.foyers().stream()
                .collect(Collectors.toMap(FoyerDTO::getIdFoyer, Function.identity(), (a, b) -> a));
        return blocs.stream().map(b -> {
            if (b.getIdFoyer() == null) {
                return versReponse(b, null);
            }
            FoyerDTO foyer = resultat.disponible()
                    ? foyersParId.get(b.getIdFoyer())
                    : FoyerDTO.indisponible(b.getIdFoyer(), resultat.raison());
            return versReponse(b, foyer);
        }).toList();
    }

    @Override
    public BlocResponse getBlocById(long idBloc) {
        Bloc bloc = trouverBloc(idBloc);
        return versReponse(bloc, lireFoyer(bloc.getIdFoyer()));
    }

    @Override
    public void deleteBloc(long idBloc) {
        if (!blocRepository.existsById(idBloc)) {
            log.warn("Suppression demandée pour un bloc inexistant, id {}", idBloc);
        }
        blocRepository.deleteById(idBloc);
        log.info("Suppression du bloc id={}", idBloc);
    }

    /** Modifie le nom et la capacité ; si le bloc est dans un foyer, la règle de capacité est revérifiée. */
    @Override
    public BlocResponse updateBloc(BlocDTO dto) {
        log.info("Modification du bloc id={}", dto.idBloc());
        verifierCapacitePositive(dto.capaciteBloc());
        Bloc bloc = trouverBloc(dto.idBloc());
        FoyerDTO foyer = null;
        if (bloc.getIdFoyer() != null) {
            foyer = foyerRemoteService.verifierFoyer(bloc.getIdFoyer());
            verifierCapaciteFoyer(foyer, bloc.getIdBloc(), dto.capaciteBloc());
        }
        bloc.setNomBloc(dto.nomBloc());
        bloc.setCapaciteBloc(dto.capaciteBloc());
        return versReponse(blocRepository.save(bloc), foyer);
    }

    /**
     * Règle métier qui nécessite foyer-service (appel Feign protégé par Resilience4j) :
     * le foyer doit exister et la somme des capacités de ses blocs ne doit pas dépasser sa capacité.
     */
    @Override
    public BlocResponse affecterBlocAFoyer(long idBloc, long idFoyer) {
        log.info("Affectation du bloc {} au foyer {}", idBloc, idFoyer);
        Bloc bloc = trouverBloc(idBloc);
        FoyerDTO foyer = foyerRemoteService.verifierFoyer(idFoyer);
        verifierCapaciteFoyer(foyer, idBloc, bloc.getCapaciteBloc());
        bloc.setIdFoyer(idFoyer);
        return versReponse(blocRepository.save(bloc), foyer);
    }

    @Override
    public BlocResponse desaffecterBlocDuFoyer(long idBloc) {
        log.info("Désaffectation du bloc {}", idBloc);
        Bloc bloc = trouverBloc(idBloc);
        bloc.setIdFoyer(null);
        return versReponse(blocRepository.save(bloc), null);
    }

    @Override
    public List<BlocResponse> getBlocsParFoyer(long idFoyer) {
        List<Bloc> blocs = blocRepository.findByIdFoyer(idFoyer);
        if (blocs.isEmpty()) {
            return List.of();
        }
        FoyerDTO foyer = lireFoyer(idFoyer);
        return blocs.stream().map(b -> versReponse(b, foyer)).toList();
    }

    // ------------------------------------------------------------------ Outils

    private void verifierCapaciteFoyer(FoyerDTO foyer, long idBloc, long capaciteBloc) {
        long dejaOccupee = blocRepository.findByIdFoyer(foyer.getIdFoyer()).stream()
                .filter(b -> b.getIdBloc() != idBloc)
                .mapToLong(Bloc::getCapaciteBloc)
                .sum();
        if (dejaOccupee + capaciteBloc > foyer.getCapaciteFoyer()) {
            throw new CapaciteDepasseeException("Capacité du foyer « " + foyer.getNomFoyer() + " » dépassée : "
                    + dejaOccupee + " déjà occupés + " + capaciteBloc + " > " + foyer.getCapaciteFoyer());
        }
    }

    private void verifierCapacitePositive(long capacite) {
        if (capacite <= 0) {
            throw new IllegalArgumentException("La capacité du bloc doit être strictement positive");
        }
    }

    private FoyerDTO lireFoyer(Long idFoyer) {
        if (idFoyer == null) {
            return null;
        }
        try {
            return foyerRemoteService.getFoyer(idFoyer);
        } catch (FoyerIntrouvableException e) {
            log.warn("Le foyer {} n'existe plus dans foyer-service", idFoyer);
            return null;
        }
    }

    private Bloc trouverBloc(long idBloc) {
        return blocRepository.findById(idBloc).orElseThrow(() -> {
            log.error("Bloc introuvable : id={}", idBloc);
            return new NoSuchElementException("Bloc introuvable : id=" + idBloc);
        });
    }

    private BlocResponse versReponse(Bloc bloc, FoyerDTO foyer) {
        return new BlocResponse(bloc.getIdBloc(), bloc.getNomBloc(), bloc.getCapaciteBloc(), foyer);
    }
}
