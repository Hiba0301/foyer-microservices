package tn.esprit.blocservice.controllers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import tn.esprit.blocservice.dto.BlocDTO;
import tn.esprit.blocservice.dto.BlocResponse;
import tn.esprit.blocservice.services.IBlocService;

import java.util.List;

/**
 * Endpoints du projet DevOps (add, getAll, get, delete, update) + affectation à un foyer.
 * "affecter-chambre-bloc" est retiré : les chambres appartiennent à un autre module.
 */
@RestController
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("api/blocs")
public class BlocController {
    IBlocService blocService;

    @PostMapping("add")
    public BlocResponse addingBloc(@RequestBody BlocDTO blocDTO) {
        return blocService.addBloc(blocDTO);
    }

    @GetMapping("getAll")
    public List<BlocResponse> gettingAllBloc() {
        return blocService.getAllBlocs();
    }

    @GetMapping("get")
    public BlocResponse gettingBloc(@RequestParam("idBloc") long idBloc) {
        return blocService.getBlocById(idBloc);
    }

    @DeleteMapping("delete/{idBloc}")
    public void deletingBloc(@PathVariable("idBloc") long idBloc) {
        blocService.deleteBloc(idBloc);
    }

    @PutMapping("update")
    public BlocResponse updatingBloc(@RequestBody BlocDTO blocDTO) {
        return blocService.updateBloc(blocDTO);
    }

    @PutMapping("affecter-bloc-foyer")
    public BlocResponse affecterBlocAFoyer(@RequestParam("idBloc") long idBloc, @RequestParam("idFoyer") long idFoyer) {
        return blocService.affecterBlocAFoyer(idBloc, idFoyer);
    }

    @PutMapping("desaffecter-bloc-foyer")
    public BlocResponse desaffecterBlocDuFoyer(@RequestParam("idBloc") long idBloc) {
        return blocService.desaffecterBlocDuFoyer(idBloc);
    }

    @GetMapping("par-foyer/{idFoyer}")
    public List<BlocResponse> blocsParFoyer(@PathVariable("idFoyer") long idFoyer) {
        return blocService.getBlocsParFoyer(idFoyer);
    }
}
