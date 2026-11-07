package com.logistock.domain.model;

import java.time.LocalDateTime;

/** Un mouvement de stock : qui, quand, quel produit, combien. commandeId est vide pour une entrée. */
public record MouvementStock(
        Long id,
        LocalDateTime date,
        TypeMouvement type,
        int quantite,
        String motif,
        Long produitId,
        Long utilisateurId,
        Long commandeId) {
}
