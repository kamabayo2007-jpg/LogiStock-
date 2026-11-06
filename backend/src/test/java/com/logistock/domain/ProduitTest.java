package com.logistock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.Produit;
import com.logistock.domain.model.TypeMouvement;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProduitTest {

    private Produit produit(int stock, int seuil) {
        return new Produit(1L, "PAP-001", "Ramette A4", null, new BigDecimal("3500"),
                stock, seuil, 1L, "Papeterie");
    }

    @Test
    void entreeAugmenteLeStock() {
        assertEquals(15, produit(10, 5).appliquer(TypeMouvement.ENTREE, 5).quantite());
    }

    @Test
    void sortieDiminueLeStock() {
        assertEquals(6, produit(10, 5).appliquer(TypeMouvement.SORTIE, 4).quantite());
    }

    @Test
    void sortieDuStockExactEstAutorisee() {
        assertEquals(0, produit(10, 5).appliquer(TypeMouvement.SORTIE, 10).quantite());
    }

    @Test
    void sortieSuperieureAuStockEstRefusee() {
        assertThrows(BusinessRuleException.class,
                () -> produit(10, 5).appliquer(TypeMouvement.SORTIE, 11));
    }

    @Test
    void quantiteNulleOuNegativeEstRefusee() {
        assertThrows(BusinessRuleException.class, () -> produit(10, 5).appliquer(TypeMouvement.ENTREE, 0));
        assertThrows(BusinessRuleException.class, () -> produit(10, 5).appliquer(TypeMouvement.SORTIE, -3));
    }

    @Test
    void alerteQuandQuantiteInferieureOuEgaleAuSeuil() {
        assertTrue(produit(5, 5).enAlerte());
        assertTrue(produit(2, 5).enAlerte());
        assertFalse(produit(6, 5).enAlerte());
    }

    @Test
    void ruptureQuandQuantiteEstZero() {
        assertTrue(produit(0, 5).enRupture());
        assertFalse(produit(1, 5).enRupture());
    }
}
