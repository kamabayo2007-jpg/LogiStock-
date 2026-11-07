package com.logistock.adapter.web.dto;

import com.logistock.domain.model.RoleNom;
import com.logistock.usecase.UserUseCase.Donnees;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corps de requête pour créer ou modifier un compte (mot de passe facultatif à la modification). */
public record UserRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "L'e-mail est obligatoire")
        @Email(message = "L'e-mail n'est pas valide")
        @Size(max = 100, message = "L'e-mail ne doit pas dépasser 100 caractères")
        String email,

        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
        String motDePasse,

        @NotNull(message = "Le rôle est obligatoire (ADMINISTRATEUR, GESTIONNAIRE_STOCK ou COMMERCIAL)")
        RoleNom role) {

    public Donnees toDonnees() {
        return new Donnees(nom, email, motDePasse, role);
    }
}
