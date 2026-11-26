package tn.esprit.universiteservice.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.universiteservice.entities.Universite;

import java.util.Optional;

@Repository
public interface UniversiteRepository extends CrudRepository<Universite, Long> {
    Universite findByNomUniversite(String nomUniversite);

    Optional<Universite> findByIdFoyer(Long idFoyer);
}
