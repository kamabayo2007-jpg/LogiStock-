package com.logistock.adapter.web.dto;

import com.logistock.usecase.CommandeUseCase;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneRequest(
        @NotNull(message = "Le produit est obligatoire")
        Long produitId,
        @Min(value = 1, message = "La quantité doit être supérieure à 0")
        int quantite) {

    public CommandeUseCase.LigneDemande toDemande() {
        return new CommandeUseCase.LigneDemande(produitId, quantite);
    }
}
