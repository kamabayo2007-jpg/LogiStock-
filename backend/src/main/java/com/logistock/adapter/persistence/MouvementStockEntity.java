package com.logistock.adapter.persistence;

import com.logistock.domain.model.TypeMouvement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvement_stock")
public class MouvementStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mouvement")
    private Long id;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private int quantite;

    @Column
    private String motif;

    @Column(name = "id_produit", nullable = false)
    private Long produitId;

    @Column(name = "id_utilisateur", nullable = false)
    private Long utilisateurId;

    @Column(name = "id_commande")
    private Long commandeId;

    protected MouvementStockEntity() {
    }

    public MouvementStockEntity(Long id, LocalDateTime dateMouvement, TypeMouvement typeMouvement,
                                int quantite, String motif, Long produitId, Long utilisateurId,
                                Long commandeId) {
        this.id = id;
        this.dateMouvement = dateMouvement;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.motif = motif;
        this.produitId = produitId;
        this.utilisateurId = utilisateurId;
        this.commandeId = commandeId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public TypeMouvement getTypeMouvement() {
        return typeMouvement;
    }

    public int getQuantite() {
        return quantite;
    }

    public String getMotif() {
        return motif;
    }

    public Long getProduitId() {
        return produitId;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public Long getCommandeId() {
        return commandeId;
    }
}
