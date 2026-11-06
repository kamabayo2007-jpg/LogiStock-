package com.logistock.adapter.web.dto;

import com.logistock.usecase.ProduitUseCase.Donnees;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corps de requête pour créer ou modifier un produit (la quantité est ignorée à la modification). */
public record ProduitRequest(
        @NotBlank(message = "La référence est obligatoire")
        @Size(max = 30, message = "La référence ne doit pas dépasser 30 caractères")
        String reference,

        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 100, message = "La désignation ne doit pas dépasser 100 caractères")
        String designation,

        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix doit être supérieur ou égal à 0")
        BigDecimal prix,

        @Min(value = 0, message = "La quantité doit être supérieure ou égale à 0")
        int quantite,

        @Min(value = 0, message = "Le seuil d'alerte doit être supérieur ou égal à 0")
        int seuilAlerte,

        @NotNull(message = "La catégorie est obligatoire")
        Long categorieId) {

    public Donnees toDonnees() {
        return new Donnees(reference, designation, description, prix, quantite, seuilAlerte, categorieId);
    }
}
