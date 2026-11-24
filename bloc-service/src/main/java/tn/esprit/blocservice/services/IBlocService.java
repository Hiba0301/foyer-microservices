package tn.esprit.blocservice.services;

import tn.esprit.blocservice.dto.BlocDTO;
import tn.esprit.blocservice.dto.BlocResponse;

import java.util.List;

public interface IBlocService {
    BlocResponse addBloc(BlocDTO bloc);
    List<BlocResponse> getAllBlocs();
    BlocResponse getBlocById(long idBloc);
    void deleteBloc(long idBloc);
    BlocResponse updateBloc(BlocDTO bloc);
    BlocResponse affecterBlocAFoyer(long idBloc, long idFoyer);
    BlocResponse desaffecterBlocDuFoyer(long idBloc);
    List<BlocResponse> getBlocsParFoyer(long idFoyer);
}
