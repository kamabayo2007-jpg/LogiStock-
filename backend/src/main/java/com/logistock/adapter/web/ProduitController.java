package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.ProduitRequest;
import com.logistock.domain.model.Produit;
import com.logistock.usecase.ProduitUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    private final ProduitUseCase useCase;

    public ProduitController(ProduitUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<Produit> lister(@RequestParam(required = false) String q) {
        return useCase.lister(q);
    }

    @GetMapping("/alertes")
    public List<Produit> alertes() {
        return useCase.enAlerte();
    }

    @GetMapping("/ruptures")
    public List<Produit> ruptures() {
        return useCase.enRupture();
    }

    @GetMapping("/{id}")
    public Produit trouver(@PathVariable Long id) {
        return useCase.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produit creer(@Valid @RequestBody ProduitRequest requete, Authentication authentication) {
        return useCase.creer(requete.toDonnees(), authentication.getName());
    }

    @PutMapping("/{id}")
    public Produit modifier(@PathVariable Long id, @Valid @RequestBody ProduitRequest requete) {
        return useCase.modifier(id, requete.toDonnees());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        useCase.supprimer(id);
    }
}
