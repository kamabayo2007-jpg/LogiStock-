package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.CategorieRequest;
import com.logistock.domain.model.Categorie;
import com.logistock.usecase.CategorieUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieUseCase useCase;

    public CategorieController(CategorieUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<Categorie> lister() {
        return useCase.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categorie creer(@Valid @RequestBody CategorieRequest requete) {
        return useCase.creer(requete.nom());
    }

    @PutMapping("/{id}")
    public Categorie modifier(@PathVariable Long id, @Valid @RequestBody CategorieRequest requete) {
        return useCase.modifier(id, requete.nom());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        useCase.supprimer(id);
    }
}
