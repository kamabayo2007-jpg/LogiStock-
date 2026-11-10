package com.logistock.usecase;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.Facture;
import com.logistock.domain.model.LigneCommande;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.StatutCommande;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.model.User;
import com.logistock.domain.port.ClientRepository;
import com.logistock.domain.port.CommandeRepository;
import com.logistock.domain.port.FactureRepository;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : commandes (US11, US12, US13) et historique d'un client (US14). */
@Service
@Transactional
public class CommandeUseCase {

    public record LigneDemande(Long produitId, int quantite) {
    }

    private final CommandeRepository commandes;
    private final ClientRepository clients;
    private final ProduitRepository produits;
    private final MouvementStockRepository mouvements;
    private final FactureRepository factures;
    private final UserRepository users;

    public CommandeUseCase(CommandeRepository commandes, ClientRepository clients,
                           ProduitRepository produits, MouvementStockRepository mouvements,
                           FactureRepository factures, UserRepository users) {
        this.commandes = commandes;
        this.clients = clients;
        this.produits = produits;
        this.mouvements = mouvements;
        this.factures = factures;
        this.users = users;
    }

    /**
     * Crée une commande EN_COURS. Le stock n'est pas touché : il ne diminue qu'à la validation.
     * Le prix de chaque ligne est le prix du produit à cet instant. Un même produit demandé
     * plusieurs fois est regroupé sur une seule ligne.
     */
    public Commande creer(Long clientId, List<LigneDemande> demandes, String emailUtilisateur) {
        User user = utilisateur(emailUtilisateur);
        Client client = clients.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
        if (demandes == null || demandes.isEmpty()) {
            throw new BusinessRuleException("Une commande doit contenir au moins un produit");
        }

        Map<Long, Integer> parProduit = new LinkedHashMap<>();
        for (LigneDemande d : demandes) {
            if (d.quantite() <= 0) {
                throw new BusinessRuleException("La quantité doit être supérieure à 0");
            }
            parProduit.merge(d.produitId(), d.quantite(), Integer::sum);
        }

        List<LigneCommande> lignes = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : parProduit.entrySet()) {
            Produit p = produits.findById(e.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + e.getKey()));
            lignes.add(new LigneCommande(p.id(), p.reference(), p.designation(), e.getValue(), p.prix()));
        }

        return commandes.save(new Commande(null, LocalDateTime.now(), StatutCommande.EN_COURS,
                client.id(), client.nomComplet(), user.id(), lignes));
    }

    /**
     * Valide la commande, dans une seule transaction : vérifie le stock de TOUTES les lignes,
     * puis diminue les stocks, crée un mouvement de SORTIE par ligne et émet la facture.
     * Si un seul produit manque, rien n'est modifié.
     */
    public Commande valider(Long id, String emailUtilisateur) {
        User user = utilisateur(emailUtilisateur);
        Commande commande = commandes.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
        Commande validee = commande.avecStatut(StatutCommande.VALIDEE);

        // Produits verrouillés dans l'ordre des identifiants : pas de blocage croisé entre deux ventes.
        List<LigneCommande> triees = new ArrayList<>(commande.lignes());
        triees.sort(Comparator.comparing(LigneCommande::produitId));

        // 1) Tout vérifier d'abord (lève « Stock insuffisant ... » sans rien enregistrer).
        List<Produit> misAJour = new ArrayList<>();
        for (LigneCommande l : triees) {
            Produit p = produits.findByIdForUpdate(l.produitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + l.designation()));
            misAJour.add(p.appliquer(TypeMouvement.SORTIE, l.quantite()));
        }

        // 2) Puis enregistrer.
        LocalDateTime maintenant = LocalDateTime.now();
        for (int i = 0; i < triees.size(); i++) {
            LigneCommande l = triees.get(i);
            produits.save(misAJour.get(i));
            mouvements.save(new MouvementStock(null, maintenant, TypeMouvement.SORTIE, l.quantite(),
                    "Vente commande #" + id, l.produitId(), user.id(), id));
        }
        Commande resultat = commandes.save(validee);
        emettreFacture(resultat);
        return resultat;
    }

    public Commande annuler(Long id) {
        Commande commande = commandes.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
        return commandes.save(commande.avecStatut(StatutCommande.ANNULEE));
    }

    @Transactional(readOnly = true)
    public Commande trouver(Long id) {
        return commandes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
    }

    @Transactional(readOnly = true)
    public List<Commande> lister(StatutCommande statut, Long clientId) {
        return commandes.findAll(statut, clientId);
    }

    /** Historique d'achats d'un client (US14). */
    @Transactional(readOnly = true)
    public List<Commande> historiqueClient(Long clientId) {
        clients.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
        return commandes.findAll(null, clientId);
    }

    private void emettreFacture(Commande commande) {
        LocalDate aujourdhui = LocalDate.now();
        long rang = factures.compterPourAnnee(aujourdhui.getYear()) + 1;
        String numero = String.format("FAC-%d-%04d", aujourdhui.getYear(), rang);
        factures.save(new Facture(null, numero, aujourdhui, commande.total(), commande.id()));
    }

    private User utilisateur(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
