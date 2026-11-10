package com.logistock.adapter.persistence;

import com.logistock.domain.model.StatutCommande;
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
@Table(name = "commande")
public class CommandeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commande")
    private Long id;

    @Column(name = "date_commande", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    @Column(name = "id_client", nullable = false)
    private Long clientId;

    @Column(name = "id_utilisateur", nullable = false)
    private Long utilisateurId;

    protected CommandeEntity() {
    }

    public CommandeEntity(Long id, LocalDateTime date, StatutCommande statut, Long clientId, Long utilisateurId) {
        this.id = id;
        this.date = date;
        this.statut = statut;
        this.clientId = clientId;
        this.utilisateurId = utilisateurId;
    }

    public Long getId() { return id; }
    public LocalDateTime getDate() { return date; }
    public StatutCommande getStatut() { return statut; }
    public Long getClientId() { return clientId; }
    public Long getUtilisateurId() { return utilisateurId; }
}
