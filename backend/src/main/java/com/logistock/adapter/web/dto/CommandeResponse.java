package com.logistock.adapter.web.dto;

import com.logistock.domain.model.Commande;
import com.logistock.domain.model.LigneCommande;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommandeResponse(
        Long id,
        LocalDateTime date,
        String statut,
        Long clientId,
        String clientNom,
        BigDecimal total,
        List<Ligne> lignes) {

    public record Ligne(Long produitId, String reference, String designation,
                        int quantite, BigDecimal prixUnitaire, BigDecimal total) {
        static Ligne from(LigneCommande l) {
            return new Ligne(l.produitId(), l.reference(), l.designation(), l.quantite(),
                    l.prixUnitaire(), l.total());
        }
    }

    public static CommandeResponse from(Commande c) {
        return new CommandeResponse(c.id(), c.date(), c.statut().name(), c.clientId(), c.clientNom(),
                c.total(), c.lignes().stream().map(Ligne::from).toList());
    }
}
