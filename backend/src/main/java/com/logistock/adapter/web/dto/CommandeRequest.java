package com.logistock.adapter.web.dto;

import com.logistock.usecase.CommandeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CommandeRequest(
        @NotNull(message = "Le client est obligatoire")
        Long clientId,
        @NotEmpty(message = "Une commande doit contenir au moins un produit")
        @Valid
        List<LigneRequest> lignes) {

    public List<CommandeUseCase.LigneDemande> toDemandes() {
        return lignes.stream().map(LigneRequest::toDemande).toList();
    }
}
