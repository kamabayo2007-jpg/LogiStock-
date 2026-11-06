package com.logistock.adapter.persistence;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.Categorie;
import com.logistock.domain.port.CategorieRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class CategorieRepositoryAdapter implements CategorieRepository {

    private final CategorieJpaRepository jpa;

    public CategorieRepositoryAdapter(CategorieJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Categorie> findAll() {
        return jpa.findAllByOrderByNomAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Categorie> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Categorie> findByNom(String nom) {
        return jpa.findByNom(nom).map(this::toDomain);
    }

    @Override
    public Categorie save(Categorie categorie) {
        return toDomain(jpa.save(new CategorieEntity(categorie.id(), categorie.nom())));
    }

    @Override
    public void deleteById(Long id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Catégorie utilisée par des produits : suppression impossible");
        }
    }

    private Categorie toDomain(CategorieEntity e) {
        return new Categorie(e.getId(), e.getNom());
    }
}
