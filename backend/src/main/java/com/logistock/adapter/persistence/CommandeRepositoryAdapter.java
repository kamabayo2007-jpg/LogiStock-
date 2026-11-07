package com.logistock.adapter.persistence;

import com.logistock.domain.model.Commande;
import com.logistock.domain.model.LigneCommande;
import com.logistock.domain.model.StatutCommande;
import com.logistock.domain.port.CommandeRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class CommandeRepositoryAdapter implements CommandeRepository {

    private final CommandeJpaRepository commandes;
    private final ContenirJpaRepository contenir;
    private final ProduitJpaRepository produits;
    private final ClientJpaRepository clients;

    public CommandeRepositoryAdapter(CommandeJpaRepository commandes, ContenirJpaRepository contenir,
                                     ProduitJpaRepository produits, ClientJpaRepository clients) {
        this.commandes = commandes;
        this.contenir = contenir;
        this.produits = produits;
        this.clients = clients;
    }

    @Override
    public List<Commande> findAll(StatutCommande statut, Long clientId) {
        return commandes.rechercher(statut, clientId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Commande> findById(Long id) {
        return commandes.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Commande> findByIdForUpdate(Long id) {
        return commandes.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Commande save(Commande c) {
        boolean nouvelle = c.id() == null;
        CommandeEntity saved = commandes.save(
                new CommandeEntity(c.id(), c.date(), c.statut(), c.clientId(), c.utilisateurId()));
        if (nouvelle) {
            // Les lignes ne s'écrivent qu'à la création : ensuite, seul le statut de la commande change.
            for (LigneCommande l : c.lignes()) {
                contenir.save(new ContenirEntity(saved.getId(), l.produitId(), l.quantite(), l.prixUnitaire()));
            }
        }
        return toDomain(saved);
    }

    private Commande toDomain(CommandeEntity e) {
        List<ContenirEntity> lignesBrutes = contenir.findByCommandeIdOrderByProduitId(e.getId());
        Map<Long, ProduitEntity> parId = produits
                .findAllById(lignesBrutes.stream().map(ContenirEntity::getProduitId).toList()).stream()
                .collect(Collectors.toMap(ProduitEntity::getId, Function.identity()));
        List<LigneCommande> lignes = lignesBrutes.stream().map(l -> {
            ProduitEntity p = parId.get(l.getProduitId());
            return new LigneCommande(l.getProduitId(), p.getReference(), p.getDesignation(),
                    l.getQuantite(), l.getPrixUnitaire());
        }).toList();
        String nomClient = clients.findById(e.getClientId()).map(this::nomComplet).orElse("?");
        return new Commande(e.getId(), e.getDate(), e.getStatut(), e.getClientId(), nomClient,
                e.getUtilisateurId(), lignes);
    }

    private String nomComplet(ClientEntity c) {
        return (c.getPrenom() == null || c.getPrenom().isBlank()) ? c.getNom() : c.getPrenom() + " " + c.getNom();
    }
}
