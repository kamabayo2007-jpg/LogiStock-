package com.logistock.domain.model;

import java.math.BigDecimal;

/** Une ligne de commande : le prix unitaire est celui du jour de la commande (il ne change plus ensuite). */
public record LigneCommande(
        Long produitId,
        String reference,
        String designation,
        int quantite,
        BigDecimal prixUnitaire) {

    public BigDecimal total() {
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
}
