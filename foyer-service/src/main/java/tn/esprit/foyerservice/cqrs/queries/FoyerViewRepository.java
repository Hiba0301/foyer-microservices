package tn.esprit.foyerservice.cqrs.queries;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoyerViewRepository extends JpaRepository<FoyerView, String> {
    List<FoyerView> findByCapaciteFoyerGreaterThanEqualOrderByNomFoyerAsc(long capaciteMin);
}
