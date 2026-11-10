package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.Facture;
import com.logistock.domain.model.LigneCommande;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.StatutCommande;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.model.User;
import com.logistock.domain.port.ClientRepository;
import com.logistock.domain.port.CommandeRepository;
import com.logistock.domain.port.FactureRepository;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import com.logistock.usecase.CommandeUseCase.LigneDemande;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommandeUseCaseTest {

    private static final String VENDEUR = "vente@logistock.ml";

    private final CommandeRepository commandes = mock(CommandeRepository.class);
    private final ClientRepository clients = mock(ClientRepository.class);
    private final ProduitRepository produits = mock(ProduitRepository.class);
    private final MouvementStockRepository mouvements = mock(MouvementStockRepository.class);
    private final FactureRepository factures = mock(FactureRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CommandeUseCase useCase =
            new CommandeUseCase(commandes, clients, produits, mouvements, factures, users);

    private final Produit stylo = new Produit(1L, "PAP-001", "Stylo", null, new BigDecimal("3500"), 100, 5, 1L, "Papeterie");
    private final Produit agrafeuse = new Produit(2L, "PAP-004", "Agrafeuse", null, new BigDecimal("4000"), 3, 5, 1L, "Papeterie");

    @BeforeEach
    void preparer() {
        when(users.findByEmail(VENDEUR)).thenReturn(Optional.of(
                new User(3L, "Commercial", VENDEUR, "hash", true, RoleNom.COMMERCIAL)));
        when(clients.findById(1L)).thenReturn(Optional.of(new Client(1L, "Sahel Services", null, null, null, null)));
        when(produits.findById(1L)).thenReturn(Optional.of(stylo));
        when(produits.findById(2L)).thenReturn(Optional.of(agrafeuse));
        when(produits.findByIdForUpdate(1L)).thenReturn(Optional.of(stylo));
        when(produits.findByIdForUpdate(2L)).thenReturn(Optional.of(agrafeuse));
        when(commandes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(factures.compterPourAnnee(anyInt())).thenReturn(2L);
    }

    private Commande commande(StatutCommande statut, LigneCommande... lignes) {
        return new Commande(10L, LocalDateTime.now(), statut, 1L, "Sahel Services", 3L, List.of(lignes));
    }

    private LigneCommande ligne(Produit p, int quantite) {
        return new LigneCommande(p.id(), p.reference(), p.designation(), quantite, p.prix());
    }

    @Test
    void creationRegroupeLesLignesEtFigeLePrix() {
        Commande c = useCase.creer(1L,
                List.of(new LigneDemande(1L, 10), new LigneDemande(1L, 5), new LigneDemande(2L, 1)), VENDEUR);

        assertEquals(StatutCommande.EN_COURS, c.statut());
        assertEquals(2, c.lignes().size());
        assertEquals(15, c.lignes().get(0).quantite());
        assertEquals(new BigDecimal("3500"), c.lignes().get(0).prixUnitaire());
        assertEquals(new BigDecimal("56500"), c.total());
        verify(produits, never()).save(any());
    }

    @Test
    void commandeSansLigneRefusee() {
        assertThrows(BusinessRuleException.class, () -> useCase.creer(1L, List.of(), VENDEUR));
        verify(commandes, never()).save(any());
    }

    @Test
    void validationDiminueLeStockCreeLesSortiesEtLaFacture() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(
                commande(StatutCommande.EN_COURS, ligne(stylo, 30), ligne(agrafeuse, 3))));

        Commande validee = useCase.valider(10L, VENDEUR);

        assertEquals(StatutCommande.VALIDEE, validee.statut());

        ArgumentCaptor<Produit> prods = ArgumentCaptor.forClass(Produit.class);
        verify(produits, times(2)).save(prods.capture());
        assertEquals(70, prods.getAllValues().get(0).quantite());
        assertEquals(0, prods.getAllValues().get(1).quantite());

        ArgumentCaptor<MouvementStock> mvts = ArgumentCaptor.forClass(MouvementStock.class);
        verify(mouvements, times(2)).save(mvts.capture());
        assertEquals(TypeMouvement.SORTIE, mvts.getValue().type());
        assertEquals(10L, mvts.getValue().commandeId());

        ArgumentCaptor<Facture> fact = ArgumentCaptor.forClass(Facture.class);
        verify(factures).save(fact.capture());
        assertEquals(String.format("FAC-%d-0003", LocalDate.now().getYear()), fact.getValue().numero());
        assertEquals(new BigDecimal("117000"), fact.getValue().montantTotal());
    }

    @Test
    void stockInsuffisantNeModifieRien() {
        // 4 agrafeuses demandées, 3 en stock : la commande entière est refusée.
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(
                commande(StatutCommande.EN_COURS, ligne(stylo, 30), ligne(agrafeuse, 4))));

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> useCase.valider(10L, VENDEUR));

        assertEquals("Stock insuffisant pour « Agrafeuse » : disponible 3, demandé 4", e.getMessage());
        verify(produits, never()).save(any());
        verify(mouvements, never()).save(any());
        verify(factures, never()).save(any());
        verify(commandes, never()).save(any());
    }

    @Test
    void commandeDejaValideeNePeutPasEtreRevalidee() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(
                commande(StatutCommande.VALIDEE, ligne(stylo, 1))));

        assertThrows(BusinessRuleException.class, () -> useCase.valider(10L, VENDEUR));
        verify(produits, never()).save(any());
        verify(factures, never()).save(any());
    }

    @Test
    void annulationPossibleSeulementEnCours() {
        when(commandes.findByIdForUpdate(10L)).thenReturn(Optional.of(
                commande(StatutCommande.EN_COURS, ligne(stylo, 1))));
        assertEquals(StatutCommande.ANNULEE, useCase.annuler(10L).statut());

        when(commandes.findByIdForUpdate(11L)).thenReturn(Optional.of(
                commande(StatutCommande.VALIDEE, ligne(stylo, 1))));
        assertThrows(BusinessRuleException.class, () -> useCase.annuler(11L));
    }
}
