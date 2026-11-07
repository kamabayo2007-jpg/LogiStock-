package com.logistock.adapter.pdf;

import com.logistock.domain.model.Client;
import com.logistock.domain.model.Commande;
import com.logistock.domain.model.Facture;
import com.logistock.domain.model.LigneCommande;
import com.logistock.usecase.FactureUseCase;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Génère la facture au format PDF, sans bibliothèque externe (un PDF simple : une ou plusieurs pages A4,
 * police Helvetica). Les accents français sont conservés ; un caractère hors Latin-1 devient « ? ».
 */
public final class FacturePdf {

    private static final double LARGEUR = 595;
    private static final double HAUTEUR = 842;
    private static final double MARGE = 50;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FacturePdf() {
    }

    public static byte[] generer(FactureUseCase.Detail detail) {
        Facture facture = detail.facture();
        Commande commande = detail.commande();
        Client client = detail.client();

        List<StringBuilder> pages = new ArrayList<>();
        Page page = new Page(pages);

        page.texte(MARGE, 790, "F2", 22, "LogiStock SA");
        page.texte(MARGE, 772, "F1", 10, "Gestion de stock et facturation");
        page.texte(350, 790, "F2", 16, "FACTURE");
        page.texte(350, 772, "F1", 11, "N° " + facture.numero());
        page.texte(350, 757, "F1", 11, "Date : " + facture.date().format(FORMAT_DATE));
        page.texte(350, 742, "F1", 11, "Commande n° " + commande.id());

        page.texte(MARGE, 735, "F2", 11, "Client");
        double y = 720;
        page.texte(MARGE, y, "F1", 11, client.nomComplet());
        for (String ligne : new String[] {client.adresse(), client.telephone(), client.email()}) {
            if (ligne != null && !ligne.isBlank()) {
                y -= 14;
                page.texte(MARGE, y, "F1", 10, ligne);
            }
        }

        y -= 40;
        y = enTeteTableau(page, y);
        for (LigneCommande l : commande.lignes()) {
            if (y < 110) {
                page = page.nouvellePage();
                y = enTeteTableau(page, 790);
            }
            page.texte(MARGE + 4, y, "F1", 10, coupe(l.reference(), 12));
            page.texte(125, y, "F1", 10, coupe(l.designation(), 30));
            page.texteDroite(350, y, "F1", 10, String.valueOf(l.quantite()));
            page.texteDroite(440, y, "F1", 10, montant(l.prixUnitaire()));
            page.texteDroite(LARGEUR - MARGE - 4, y, "F1", 10, montant(l.total()));
            y -= 18;
        }

        if (y < 90) {
            page = page.nouvellePage();
            y = 790;
        }
        page.ligne(MARGE, y + 8, LARGEUR - MARGE, y + 8);
        y -= 12;
        page.texte(350, y, "F2", 12, "TOTAL");
        page.texteDroite(LARGEUR - MARGE - 4, y, "F2", 12, montant(facture.montantTotal()));
        page.texte(MARGE, 40, "F1", 9, "Merci de votre confiance. Facture générée automatiquement par LogiStock SA.");

        return assembler(pages);
    }

    private static double enTeteTableau(Page page, double y) {
        page.rectangleGris(MARGE, y - 5, LARGEUR - 2 * MARGE, 20);
        page.texte(MARGE + 4, y, "F2", 10, "Réf.");
        page.texte(125, y, "F2", 10, "Désignation");
        page.texteDroite(350, y, "F2", 10, "Qté");
        page.texteDroite(440, y, "F2", 10, "Prix unitaire");
        page.texteDroite(LARGEUR - MARGE - 4, y, "F2", 10, "Total");
        return y - 22;
    }

    /** 125000 → « 125 000 FCFA ». */
    static String montant(BigDecimal valeur) {
        String entier = valeur.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
        StringBuilder sb = new StringBuilder();
        int debut = entier.startsWith("-") ? 1 : 0;
        String chiffres = entier.substring(debut);
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(chiffres.charAt(i));
        }
        return (debut == 1 ? "-" : "") + sb + " FCFA";
    }

    private static String coupe(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }

    /** Une page : son contenu est accumulé dans un flux de dessin PDF. */
    private static final class Page {
        private final List<StringBuilder> pages;
        private final StringBuilder flux = new StringBuilder();

        Page(List<StringBuilder> pages) {
            this.pages = pages;
            pages.add(flux);
        }

        Page nouvellePage() {
            return new Page(pages);
        }

        void texte(double x, double y, String police, int taille, String texte) {
            flux.append("BT /").append(police).append(' ').append(taille).append(" Tf ")
                    .append(nombre(x)).append(' ').append(nombre(y)).append(" Td (")
                    .append(echappe(texte)).append(") Tj ET\n");
        }

        void texteDroite(double xDroite, double y, String police, int taille, String texte) {
            texte(xDroite - largeur(texte, taille), y, police, taille, texte);
        }

        void ligne(double x1, double y1, double x2, double y2) {
            flux.append("0.5 w ").append(nombre(x1)).append(' ').append(nombre(y1)).append(" m ")
                    .append(nombre(x2)).append(' ').append(nombre(y2)).append(" l S\n");
        }

        void rectangleGris(double x, double y, double l, double h) {
            flux.append("0.9 g ").append(nombre(x)).append(' ').append(nombre(y)).append(' ')
                    .append(nombre(l)).append(' ').append(nombre(h)).append(" re f 0 g\n");
        }

        /** Largeur approchée en Helvetica : suffisante pour aligner des nombres à droite. */
        private static double largeur(String texte, int taille) {
            double total = 0;
            for (char c : texte.toCharArray()) {
                total += (c == ' ') ? 278 : (Character.isDigit(c) ? 556 : (Character.isUpperCase(c) ? 667 : 520));
            }
            return total * taille / 1000.0;
        }
    }

    private static String nombre(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    /** Échappe ( ) \ et passe en Latin-1 (octal pour les caractères accentués). */
    private static String echappe(String texte) {
        StringBuilder sb = new StringBuilder();
        for (char c : texte.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 32) {
                sb.append(' ');
            } else if (c < 127) {
                sb.append(c);
            } else if (c <= 255) {
                sb.append('\\').append(String.format("%03o", (int) c));
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /** Assemble les objets PDF et la table des positions (xref). */
    private static byte[] assembler(List<StringBuilder> pages) {
        int n = pages.size();
        // Numérotation : 1 catalogue, 2 pages, 3 police normale, 4 police grasse,
        // puis pour chaque page : un objet page et un objet contenu.
        List<String> objets = new ArrayList<>();
        objets.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        objets.add("<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
        objets.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objets.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < n; i++) {
            objets.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) LARGEUR + " " + (int) HAUTEUR + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>");
            byte[] contenu = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            objets.add("<< /Length " + contenu.length + " >>\nstream\n"
                    + new String(contenu, StandardCharsets.ISO_8859_1) + "endstream");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ecrire(out, "%PDF-1.4\n");
        int[] positions = new int[objets.size() + 1];
        for (int i = 0; i < objets.size(); i++) {
            positions[i + 1] = out.size();
            ecrire(out, (i + 1) + " 0 obj\n" + objets.get(i) + "\nendobj\n");
        }
        int debutXref = out.size();
        ecrire(out, "xref\n0 " + (objets.size() + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= objets.size(); i++) {
            ecrire(out, String.format("%010d 00000 n \n", positions[i]));
        }
        ecrire(out, "trailer\n<< /Size " + (objets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + debutXref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void ecrire(ByteArrayOutputStream out, String s) {
        byte[] octets = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(octets, 0, octets.length);
    }
}
