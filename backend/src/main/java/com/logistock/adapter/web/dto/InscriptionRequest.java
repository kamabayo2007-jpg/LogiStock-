package com.logistock.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de requête de l'inscription : pas de rôle, il est imposé par le serveur. */
public record InscriptionRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "L'e-mail est obligatoire")
        @Email(message = "L'e-mail n'est pas valide")
        @Size(max = 100, message = "L'e-mail ne doit pas dépasser 100 caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
        String motDePasse) {
}
