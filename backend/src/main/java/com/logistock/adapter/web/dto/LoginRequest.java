package com.logistock.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'e-mail est obligatoire") String email,
        @NotBlank(message = "Le mot de passe est obligatoire") String password) {
}
