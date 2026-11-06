package com.logistock.adapter.persistence;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Produit;
import com.logistock.domain.port.ProduitRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ProduitRepositoryAdapter implements ProduitRepository {

    private final ProduitJpaRepository jpa;
    private final CategorieJpaRepository categories;

    public ProduitRepositoryAdapter(ProduitJpaRepository jpa, CategorieJpaRepository categories) {
        this.jpa = jpa;
        this.categories = categories;
    }

    @Override
    public List<Produit> findAll(String recherche) {
        List<ProduitEntity> resultat = (recherche == null || recherche.isBlank())
                ? jpa.findAllByOrderByDesignationAsc()
                : jpa.rechercher(recherche.trim());
        return resultat.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Produit> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Produit> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<Produit> findByReference(String reference) {
        return jpa.findByReference(reference).map(this::toDomain);
    }

    @Override
    public List<Produit> findEnAlerte() {
        return jpa.findEnAlerte().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Produit> findEnRupture() {
        return jpa.findByQuantiteOrderByDesignationAsc(0).stream().map(this::toDomain).toList();
    }

    @Override
    public Produit save(Produit p) {
        CategorieEntity categorie = categories.findById(p.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        ProduitEntity entite = new ProduitEntity(p.id(), p.reference(), p.designation(), p.description(),
                p.prix(), p.quantite(), p.seuilAlerte(), categorie);
        return toDomain(jpa.save(entite));
    }

    @Override
    public void deleteById(Long id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException(
                    "Produit utilisé dans des mouvements de stock ou des commandes : suppression impossible");
        }
    }

    private Produit toDomain(ProduitEntity e) {
        return new Produit(e.getId(), e.getReference(), e.getDesignation(), e.getDescription(),
                e.getPrix(), e.getQuantite(), e.getSeuilAlerte(),
                e.getCategorie().getId(), e.getCategorie().getNom());
    }
}
