package com.logistock.adapter.web.dto;

import com.logistock.domain.model.User;

/** Ce que l'API renvoie : jamais le mot de passe, même haché. */
public record UserResponse(Long id, String nom, String email, String role, boolean actif) {

    public static UserResponse from(User u) {
        return new UserResponse(u.id(), u.nom(), u.email(), u.role().name(), u.actif());
    }
}
