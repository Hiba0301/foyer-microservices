package tn.esprit.blocservice.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.blocservice.entities.Bloc;

import java.util.List;

@Repository
public interface BlocRepository extends CrudRepository<Bloc, Long> {
    List<Bloc> findByIdFoyer(Long idFoyer);
}
