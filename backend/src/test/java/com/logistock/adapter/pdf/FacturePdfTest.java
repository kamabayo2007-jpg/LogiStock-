package com.logistock.adapter.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.Facture;
import com.logistock.domain.model.LigneCommande;
import com.logistock.domain.model.StatutCommande;
import com.logistock.usecase.FactureUseCase;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FacturePdfTest {

    @Test
    void montantEstFormateAvecEspaces() {
        assertEquals("125 000 FCFA", FacturePdf.montant(new BigDecimal("125000.00")));
        assertEquals("950 FCFA", FacturePdf.montant(new BigDecimal("950")));
        assertEquals("1 250 000 FCFA", FacturePdf.montant(new BigDecimal("1250000")));
    }

    @Test
    void lePdfGenereEstValideEtContientLesInformations() {
        Commande commande = new Commande(1L, LocalDateTime.now(), StatutCommande.VALIDEE, 1L, "Sahel Services", 3L,
                List.of(new LigneCommande(1L, "PAP-001", "Stylo à bille (bleu)", 30, new BigDecimal("3500"))));
        Facture facture = new Facture(1L, "FAC-2026-0001", LocalDate.of(2026, 10, 27), new BigDecimal("105000"), 1L);
        Client client = new Client(1L, "Sahel Services", null, "+223 20 22 10 10", null, "ACI 2000, Bamako");

        byte[] pdf = FacturePdf.generer(new FactureUseCase.Detail(facture, commande, client));
        String texte = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue(texte.startsWith("%PDF-1.4"));
        assertTrue(texte.endsWith("%%EOF\n"));
        assertTrue(texte.contains("FAC-2026-0001"));
        assertTrue(texte.contains("Sahel Services"));
        assertTrue(texte.contains("105 000 FCFA"));
    }
}
