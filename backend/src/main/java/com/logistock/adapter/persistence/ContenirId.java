package com.logistock.adapter.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clé composée de la table contenir : (commande, produit). */
public class ContenirId implements Serializable {

    private Long commandeId;
    private Long produitId;

    public ContenirId() {
    }

    public ContenirId(Long commandeId, Long produitId) {
        this.commandeId = commandeId;
        this.produitId = produitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContenirId other)) return false;
        return Objects.equals(commandeId, other.commandeId) && Objects.equals(produitId, other.produitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandeId, produitId);
    }
}
