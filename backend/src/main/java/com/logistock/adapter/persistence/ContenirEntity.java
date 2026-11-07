package com.logistock.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Une ligne de commande (table contenir). */
@Entity
@Table(name = "contenir")
@IdClass(ContenirId.class)
public class ContenirEntity {

    @Id
    @Column(name = "id_commande")
    private Long commandeId;

    @Id
    @Column(name = "id_produit")
    private Long produitId;

    @Column(nullable = false)
    private int quantite;

    @Column(name = "prix_unitaire_vente", nullable = false)
    private BigDecimal prixUnitaire;

    protected ContenirEntity() {
    }

    public ContenirEntity(Long commandeId, Long produitId, int quantite, BigDecimal prixUnitaire) {
        this.commandeId = commandeId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public Long getCommandeId() { return commandeId; }
    public Long getProduitId() { return produitId; }
    public int getQuantite() { return quantite; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
}
