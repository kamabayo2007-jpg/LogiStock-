package com.logistock.domain.model;

import com.logistock.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Une commande client. Elle naît EN_COURS, puis devient VALIDEE (stock diminué) ou ANNULEE. */
public record Commande(
        Long id,
        LocalDateTime date,
        StatutCommande statut,
        Long clientId,
        String clientNom,
        Long utilisateurId,
        List<LigneCommande> lignes) {

    public BigDecimal total() {
        return lignes.stream().map(LigneCommande::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Règle : seule une commande EN_COURS peut changer d'état. */
    public Commande avecStatut(StatutCommande nouveau) {
        if (statut != StatutCommande.EN_COURS) {
            throw new BusinessRuleException("Seule une commande en cours peut être "
                    + (nouveau == StatutCommande.VALIDEE ? "validée" : "annulée")
                    + " (cette commande est " + statut + ")");
        }
        return new Commande(id, date, nouveau, clientId, clientNom, utilisateurId, lignes);
    }
}
