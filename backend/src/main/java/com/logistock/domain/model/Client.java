package com.logistock.domain.model;

/** Un client de l'entreprise (société ou particulier : le prénom est facultatif). */
public record Client(
        Long id,
        String nom,
        String prenom,
        String telephone,
        String email,
        String adresse) {

    /** Nom à afficher : « Prénom Nom » pour un particulier, sinon le nom seul. */
    public String nomComplet() {
        return (prenom == null || prenom.isBlank()) ? nom : prenom + " " + nom;
    }
}
