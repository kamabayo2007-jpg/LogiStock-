package com.logistock.domain.model;

import com.logistock.domain.exception.BusinessRuleException;
import java.math.BigDecimal;

/** Entité métier : un produit du catalogue, avec sa quantité en stock. */
public record Produit(
        Long id,
        String reference,
        String designation,
        String description,
        BigDecimal prix,
        int quantite,
        int seuilAlerte,
        Long categorieId,
        String categorieNom) {

    /** Alerte quand la quantité est inférieure ou égale au seuil (US8). */
    public boolean enAlerte() {
        return quantite <= seuilAlerte;
    }

    public boolean enRupture() {
        return quantite == 0;
    }

    /**
     * Règle de gestion centrale : le stock ne peut jamais devenir négatif.
     * Renvoie un nouveau produit avec la quantité mise à jour (l'objet d'origine ne change pas).
     */
    public Produit appliquer(TypeMouvement type, int quantiteMouvement) {
        if (quantiteMouvement <= 0) {
            throw new BusinessRuleException("La quantité doit être supérieure à 0");
        }
        int nouvelle;
        if (type == TypeMouvement.ENTREE) {
            nouvelle = quantite + quantiteMouvement;
        } else {
            if (quantiteMouvement > quantite) {
                throw new BusinessRuleException("Stock insuffisant pour « " + designation
                        + " » : disponible " + quantite + ", demandé " + quantiteMouvement);
            }
            nouvelle = quantite - quantiteMouvement;
        }
        return new Produit(id, reference, designation, description, prix,
                nouvelle, seuilAlerte, categorieId, categorieNom);
    }
}
