package com.logistock.usecase;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Categorie;
import com.logistock.domain.port.CategorieRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : gestion des catégories de produits (US6). */
@Service
@Transactional
public class CategorieUseCase {

    private final CategorieRepository categories;

    public CategorieUseCase(CategorieRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<Categorie> lister() {
        return categories.findAll();
    }

    public Categorie creer(String nom) {
        String n = nom.trim();
        if (categories.findByNom(n).isPresent()) {
            throw new BusinessRuleException("Cette catégorie existe déjà");
        }
        return categories.save(new Categorie(null, n));
    }

    public Categorie modifier(Long id, String nom) {
        categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        String n = nom.trim();
        categories.findByNom(n)
                .filter(c -> !c.id().equals(id))
                .ifPresent(c -> {
                    throw new BusinessRuleException("Cette catégorie existe déjà");
                });
        return categories.save(new Categorie(id, n));
    }

    /** Refusée si des produits utilisent la catégorie (contrôlé par l'adaptateur via la clé étrangère). */
    public void supprimer(Long id) {
        categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        categories.deleteById(id);
    }
}
