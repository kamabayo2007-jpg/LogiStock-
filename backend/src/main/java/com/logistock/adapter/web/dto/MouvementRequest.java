package com.logistock.adapter.web.dto;

import com.logistock.domain.model.TypeMouvement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MouvementRequest(
        @NotNull(message = "Le produit est obligatoire")
        Long produitId,

        @NotNull(message = "Le type (ENTREE ou SORTIE) est obligatoire")
        TypeMouvement type,

        @Positive(message = "La quantité doit être supérieure à 0")
        int quantite,

        @Size(max = 100, message = "Le motif ne doit pas dépasser 100 caractères")
        String motif) {
}
