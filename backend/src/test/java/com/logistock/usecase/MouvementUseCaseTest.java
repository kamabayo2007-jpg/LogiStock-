package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.model.User;
import com.logistock.domain.port.MouvementStockRepository;
import com.logistock.domain.port.ProduitRepository;
import com.logistock.domain.port.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MouvementUseCaseTest {

    private static final String EMAIL = "stock@logistock.ml";

    private final ProduitRepository produits = mock(ProduitRepository.class);
    private final MouvementStockRepository mouvements = mock(MouvementStockRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MouvementUseCase useCase = new MouvementUseCase(produits, mouvements, users);

    private Produit produit(int stock) {
        return new Produit(1L, "PAP-001", "Ramette A4", null, new BigDecimal("3500"),
                stock, 5, 1L, "Papeterie");
    }

    @BeforeEach
    void preparer() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(
                new User(2L, "Gestionnaire", EMAIL, "hash", true, RoleNom.GESTIONNAIRE_STOCK)));
        when(mouvements.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void sortieSuperieureAuStockRefuseeEtRienEnregistre() {
        when(produits.findByIdForUpdate(1L)).thenReturn(Optional.of(produit(5)));

        assertThrows(BusinessRuleException.class,
                () -> useCase.enregistrer(1L, TypeMouvement.SORTIE, 6, "Vente", EMAIL));

        verify(produits, never()).save(any());
        verify(mouvements, never()).save(any());
    }

    @Test
    void sortieValideDiminueLeStockEtTraceLeMouvement() {
        when(produits.findByIdForUpdate(1L)).thenReturn(Optional.of(produit(10)));

        MouvementStock m = useCase.enregistrer(1L, TypeMouvement.SORTIE, 4, "Vente", EMAIL);

        ArgumentCaptor<Produit> sauve = ArgumentCaptor.forClass(Produit.class);
        verify(produits).save(sauve.capture());
        assertEquals(6, sauve.getValue().quantite());
        assertEquals(TypeMouvement.SORTIE, m.type());
        assertEquals(4, m.quantite());
        assertEquals(2L, m.utilisateurId());
    }

    @Test
    void entreeAugmenteLeStock() {
        when(produits.findByIdForUpdate(1L)).thenReturn(Optional.of(produit(10)));

        useCase.enregistrer(1L, TypeMouvement.ENTREE, 5, "Réapprovisionnement", EMAIL);

        ArgumentCaptor<Produit> sauve = ArgumentCaptor.forClass(Produit.class);
        verify(produits).save(sauve.capture());
        assertEquals(15, sauve.getValue().quantite());
    }

    @Test
    void produitInconnuDonneUneErreur404() {
        when(produits.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.enregistrer(99L, TypeMouvement.ENTREE, 5, "Test", EMAIL));
    }
}
