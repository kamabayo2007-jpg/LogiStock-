package com.logistock.domain.model;

/** Entité métier : un utilisateur de LogiStock (sans aucune dépendance technique). */
public record User(
        Long id,
        String nom,
        String email,
        String motDePasseHash,
        boolean actif,
        RoleNom role) {
}
