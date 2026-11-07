package com.logistock.adapter.web.dto;

import com.logistock.usecase.ClientUseCase;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank(message = "Le nom du client est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
        String prenom,
        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        String telephone,
        @Email(message = "L'adresse e-mail n'est pas valide")
        String email,
        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresse) {

    public ClientUseCase.Donnees toDonnees() {
        return new ClientUseCase.Donnees(nom, prenom, telephone, email, adresse);
    }
}
