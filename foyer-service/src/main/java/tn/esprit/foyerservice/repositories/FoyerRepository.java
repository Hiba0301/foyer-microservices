package tn.esprit.foyerservice.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.foyerservice.entities.Foyer;

@Repository
public interface FoyerRepository extends CrudRepository<Foyer, Long> {
}
