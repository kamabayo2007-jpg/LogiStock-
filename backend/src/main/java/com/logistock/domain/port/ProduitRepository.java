package com.logistock.domain.port;

import com.logistock.domain.model.Produit;
import java.util.List;
import java.util.Optional;

public interface ProduitRepository {

    /** Tous les produits, ou ceux dont la désignation ou la référence contient « recherche ». */
    List<Produit> findAll(String recherche);

    Optional<Produit> findById(Long id);

    /** Charge le produit en le verrouillant : deux mouvements simultanés ne peuvent pas se croiser. */
    Optional<Produit> findByIdForUpdate(Long id);

    Optional<Produit> findByReference(String reference);

    /** Produits dont la quantité est inférieure ou égale au seuil d'alerte. */
    List<Produit> findEnAlerte();

    /** Produits dont la quantité est 0. */
    List<Produit> findEnRupture();

    Produit save(Produit produit);

    void deleteById(Long id);
}
