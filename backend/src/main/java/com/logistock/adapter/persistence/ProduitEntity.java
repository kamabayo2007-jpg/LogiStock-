package com.logistock.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "produit")
public class ProduitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produit")
    private Long id;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private String designation;

    @Column
    private String description;

    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prix;

    @Column(name = "quantite_stock", nullable = false)
    private int quantite;

    @Column(name = "seuil_alerte", nullable = false)
    private int seuilAlerte;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_categorie", nullable = false)
    private CategorieEntity categorie;

    protected ProduitEntity() {
    }

    public ProduitEntity(Long id, String reference, String designation, String description,
                         BigDecimal prix, int quantite, int seuilAlerte, CategorieEntity categorie) {
        this.id = id;
        this.reference = reference;
        this.designation = designation;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.seuilAlerte = seuilAlerte;
        this.categorie = categorie;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getDesignation() {
        return designation;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public int getQuantite() {
        return quantite;
    }

    public int getSeuilAlerte() {
        return seuilAlerte;
    }

    public CategorieEntity getCategorie() {
        return categorie;
    }
}
