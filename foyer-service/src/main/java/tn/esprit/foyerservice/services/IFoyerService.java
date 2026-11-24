package tn.esprit.foyerservice.services;

import tn.esprit.foyerservice.entities.Foyer;

import java.util.List;

public interface IFoyerService {
    Foyer addFoyer(Foyer foyer);
    List<Foyer> getAllFoyers();
    Foyer getFoyerById(long idFoyer);
    void deleteFoyer(long idFoyer);
    Foyer updateFoyer(Foyer foyer);
    Foyer ajouterFoyerEtAffecterAUniversite(Foyer foyer, long idUniversite);
}
