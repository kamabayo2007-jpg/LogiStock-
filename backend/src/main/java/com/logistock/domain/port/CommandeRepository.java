package com.logistock.domain.port;

import com.logistock.domain.model.Commande;
import com.logistock.domain.model.StatutCommande;
import java.util.List;
import java.util.Optional;

public interface CommandeRepository {

    /** Du plus récent au plus ancien. Chaque filtre est facultatif (null = pas de filtre). */
    List<Commande> findAll(StatutCommande statut, Long clientId);

    Optional<Commande> findById(Long id);

    /** Charge la commande en la verrouillant : elle ne peut pas être validée deux fois en même temps. */
    Optional<Commande> findByIdForUpdate(Long id);

    /** Enregistre la commande et ses lignes. */
    Commande save(Commande commande);

    /** Remplace le client et toutes les lignes d'une commande existante (modification d'une commande en cours). */
    Commande modifierContenu(Commande commande);
}
