package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.LigneCommande;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.StatutCommande;
import com.logistock.domain.port.ClientRepository;
import com.logistock.domain.port.CommandeRepository;
import com.logistock.domain.port.FactureRepository;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import com.logistock.usecase.CommandeUseCase.LigneDemande;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Modification d'une commande en cours : mêmes contrôles qu'à la création, prix figés conservés. */
class CommandeModificationTest {

    private final CommandeRepository commandes = mock(CommandeRepository.class);
    private final ClientRepository clients = mock(ClientRepository.class);
    private final ProduitRepository produits = mock(ProduitRepository.class);
    private final CommandeUseCase useCase = new CommandeUseCase(commandes, clients, produits,
            mock(MouvementStockRepository.class), mock(FactureRepository.class), mock(UserRepository.class));

    // Le prix du stylo a augmenté depuis la création de la commande (3500 -> 4000).
    private final Produit stylo = new Produit(1L, "PAP-001", "Stylo", null, new BigDecimal("4000"), 100, 5, 1L, "Papeterie");
    private final Produit agrafeuse = new Produit(2L, "PAP-004", "Agrafeuse", null, new BigDecimal("4000"), 3, 5, 1L, "Papeterie");

    private Commande commande(StatutCommande statut) {
        return new Commande(10L, LocalDateTime.now(), statut, 1L, "Sahel Services", 3L,
                List.of(new LigneCommande(1L, "PAP-001", "Stylo", 10, new BigDecimal("3500"))));
    }

    @BeforeEach
    void preparer() {
        when(clients.findById(1L)).thenReturn(Optional.of(new Client(1L, "Sahel Services", null, null, null, null)));
        when(produits.findById(1L)).thenReturn(Optional.of(stylo));
        when(produits.findById(2L)).thenReturn(Optional.of(agrafeuse));
        when(commandes.modifierContenu(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void modificationCorrigeLesQuantitesEtGardeLePrixFige() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(commande(StatutCommande.EN_COURS)));

        Commande c = useCase.modifier(10L, 1L, List.of(new LigneDemande(1L, 4), new LigneDemande(2L, 2)));

        assertEquals(StatutCommande.EN_COURS, c.statut());
        assertEquals(2, c.lignes().size());
        assertEquals(4, c.lignes().get(0).quantite());
        assertEquals(new BigDecimal("3500"), c.lignes().get(0).prixUnitaire()); // prix d'origine conservé
        assertEquals(new BigDecimal("4000"), c.lignes().get(1).prixUnitaire()); // produit ajouté : prix du jour
        assertEquals(new BigDecimal("22000"), c.total());
    }

    @Test
    void uneCommandeValideeNePeutPasEtreModifiee() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(commande(StatutCommande.VALIDEE)));

        assertThrows(BusinessRuleException.class, () -> useCase.modifier(10L, 1L, List.of(new LigneDemande(1L, 1))));
        verify(commandes, never()).modifierContenu(any());
    }

    @Test
    void uneCommandeAnnuleeNePeutPasEtreModifiee() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(commande(StatutCommande.ANNULEE)));

        assertThrows(BusinessRuleException.class, () -> useCase.modifier(10L, 1L, List.of(new LigneDemande(1L, 1))));
        verify(commandes, never()).modifierContenu(any());
    }

    @Test
    void modificationSansLigneOuQuantiteNulleRefusee() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(commande(StatutCommande.EN_COURS)));

        assertThrows(BusinessRuleException.class, () -> useCase.modifier(10L, 1L, List.of()));
        assertThrows(BusinessRuleException.class, () -> useCase.modifier(10L, 1L, List.of(new LigneDemande(1L, 0))));
        verify(commandes, never()).modifierContenu(any());
    }
}
