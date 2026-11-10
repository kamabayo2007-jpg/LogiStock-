package com.logistock.usecase;

import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.Facture;
import com.logistock.domain.port.ClientRepository;
import com.logistock.domain.port.CommandeRepository;
import com.logistock.domain.port.FactureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use case : consulter la facture d'une commande validée (US13). */
@Service
@Transactional(readOnly = true)
public class FactureUseCase {

    /** Tout ce qu'il faut pour afficher ou imprimer une facture. */
    public record Detail(Facture facture, Commande commande, Client client) {
    }

    private final FactureRepository factures;
    private final CommandeRepository commandes;
    private final ClientRepository clients;

    public FactureUseCase(FactureRepository factures, CommandeRepository commandes, ClientRepository clients) {
        this.factures = factures;
        this.commandes = commandes;
        this.clients = clients;
    }

    public Detail parCommande(Long commandeId) {
        Commande commande = commandes.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
        Facture facture = factures.findByCommandeId(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune facture : la commande n'est pas validée"));
        Client client = clients.findById(commande.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
        return new Detail(facture, commande, client);
    }
}
