package com.logistock.usecase;

import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.model.User;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : entrées et sorties de stock (US7) et historique (US9). */
@Service
@Transactional
public class MouvementUseCase {

    private final ProduitRepository produits;
    private final MouvementStockRepository mouvements;
    private final UserRepository users;

    public MouvementUseCase(ProduitRepository produits,
                            MouvementStockRepository mouvements,
                            UserRepository users) {
        this.produits = produits;
        this.mouvements = mouvements;
        this.users = users;
    }

    /**
     * Enregistre un mouvement et met à jour la quantité du produit, dans une seule transaction.
     * Le produit est verrouillé pendant l'opération : le stock ne peut pas devenir négatif
     * même si deux sorties arrivent en même temps.
     */
    public MouvementStock enregistrer(Long produitId, TypeMouvement type, int quantite,
                                      String motif, String emailUtilisateur) {
        User user = users.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Produit produit = produits.findByIdForUpdate(produitId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        // Lève BusinessRuleException si le stock est insuffisant : rien n'est alors enregistré.
        produits.save(produit.appliquer(type, quantite));

        return mouvements.save(new MouvementStock(null, LocalDateTime.now(), type, quantite,
                motif, produitId, user.id(), null));
    }

    @Transactional(readOnly = true)
    public List<MouvementStock> lister(Long produitId, TypeMouvement type) {
        return mouvements.findAll(produitId, type);
    }
}
