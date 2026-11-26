package tn.esprit.universiteservice.services;

import tn.esprit.universiteservice.dto.UniversiteDTO;
import tn.esprit.universiteservice.entities.Universite;

import java.util.List;

public interface IUniversiteService {
    UniversiteDTO addUniversite(Universite universite);
    List<UniversiteDTO> getAllUniversites();
    UniversiteDTO getUniversiteById(long idUniversite);
    void deleteUniversite(long idUniversite);
    UniversiteDTO updateUniversite(Universite universite);
    UniversiteDTO affecterFoyerAUniversite(long idFoyer, String nomUniversite);
    UniversiteDTO affecterFoyerParId(long idUniversite, long idFoyer);
    UniversiteDTO desaffecterFoyerAUniversite(long idUniversite);
}
