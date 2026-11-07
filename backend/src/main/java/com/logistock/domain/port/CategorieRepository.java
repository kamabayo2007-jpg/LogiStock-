package com.logistock.domain.port;

import com.logistock.domain.model.Categorie;
import java.util.List;
import java.util.Optional;

public interface CategorieRepository {

    List<Categorie> findAll();

    Optional<Categorie> findById(Long id);

    Optional<Categorie> findByNom(String nom);

    Categorie save(Categorie categorie);

    void deleteById(Long id);
}
