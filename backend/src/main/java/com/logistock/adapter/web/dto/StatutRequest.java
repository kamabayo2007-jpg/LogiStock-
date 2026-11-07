package com.logistock.adapter.web.dto;

import jakarta.validation.constraints.NotNull;

public record StatutRequest(
        @NotNull(message = "Le champ actif (true ou false) est obligatoire")
        Boolean actif) {
}
