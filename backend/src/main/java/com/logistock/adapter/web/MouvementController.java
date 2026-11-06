package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.MouvementRequest;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.usecase.MouvementUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mouvements")
public class MouvementController {

    private final MouvementUseCase useCase;

    public MouvementController(MouvementUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MouvementStock enregistrer(@Valid @RequestBody MouvementRequest requete,
                                      Authentication authentication) {
        return useCase.enregistrer(requete.produitId(), requete.type(), requete.quantite(),
                requete.motif(), authentication.getName());
    }

    @GetMapping
    public List<MouvementStock> lister(@RequestParam(required = false) Long produitId,
                                       @RequestParam(required = false) TypeMouvement type) {
        return useCase.lister(produitId, type);
    }
}
