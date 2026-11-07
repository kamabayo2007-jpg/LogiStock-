package com.logistock.usecase;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Categorie;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.model.User;
import com.logistock.domain.port.CategorieRepository;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : catalogue produits (US5) et alertes de stock (US8). */
@Service
@Transactional
public class ProduitUseCase {

    /** Données saisies pour créer ou modifier un produit. */
    public record Donnees(
            String reference,
            String designation,
            String description,
            BigDecimal prix,
            int quantite,
            int seuilAlerte,
            Long categorieId) {
    }

    private final ProduitRepository produits;
    private final CategorieRepository categories;
    private final MouvementStockRepository mouvements;
    private final UserRepository users;

    public ProduitUseCase(ProduitRepository produits,
                          CategorieRepository categories,
                          MouvementStockRepository mouvements,
                          UserRepository users) {
        this.produits = produits;
        this.categories = categories;
        this.mouvements = mouvements;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Produit> lister(String recherche) {
        return produits.findAll(recherche);
    }

    @Transactional(readOnly = true)
    public Produit trouver(Long id) {
        return produits.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    @Transactional(readOnly = true)
    public List<Produit> enAlerte() {
        return produits.findEnAlerte();
    }

    @Transactional(readOnly = true)
    public List<Produit> enRupture() {
        return produits.findEnRupture();
    }

    /** Crée un produit. Une quantité initiale > 0 est tracée par une ENTREE « Stock initial ». */
    public Produit creer(Donnees d, String emailUtilisateur) {
        Categorie categorie = categorieExistante(d.categorieId());
        verifierReferenceLibre(d.reference(), null);

        Produit cree = produits.save(new Produit(null, d.reference().trim(), d.designation().trim(),
                d.description(), d.prix(), d.quantite(), d.seuilAlerte(), categorie.id(), categorie.nom()));

        if (d.quantite() > 0) {
            User user = users.findByEmail(emailUtilisateur)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
            mouvements.save(new MouvementStock(null, LocalDateTime.now(), TypeMouvement.ENTREE,
                    d.quantite(), "Stock initial", cree.id(), user.id(), null));
        }
        return cree;
    }

    /** Modifie les informations du produit. La quantité ne change que par les mouvements de stock. */
    public Produit modifier(Long id, Donnees d) {
        Produit actuel = trouver(id);
        Categorie categorie = categorieExistante(d.categorieId());
        verifierReferenceLibre(d.reference(), id);

        return produits.save(new Produit(id, d.reference().trim(), d.designation().trim(),
                d.description(), d.prix(), actuel.quantite(), d.seuilAlerte(), categorie.id(), categorie.nom()));
    }

    /** Refusée si le produit apparaît dans des mouvements ou des commandes (contrôlé via la clé étrangère). */
    public void supprimer(Long id) {
        trouver(id);
        produits.deleteById(id);
    }

    private Categorie categorieExistante(Long categorieId) {
        return categories.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
    }

    private void verifierReferenceLibre(String reference, Long idCourant) {
        produits.findByReference(reference.trim())
                .filter(p -> !p.id().equals(idCourant))
                .ifPresent(p -> {
                    throw new BusinessRuleException("Cette référence est déjà utilisée");
                });
    }
}
