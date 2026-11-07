package com.logistock.usecase;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.User;
import com.logistock.domain.port.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : gestion des comptes par l'administrateur (US2). */
@Service
@Transactional
public class UserUseCase {

    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    /** Données saisies pour créer ou modifier un compte. */
    public record Donnees(String nom, String email, String motDePasse, RoleNom role) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserUseCase(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<User> lister() {
        return users.findAll();
    }

    @Transactional(readOnly = true)
    public User trouver(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    /** Crée un compte actif. Le mot de passe est obligatoire et stocké haché (jamais en clair). */
    public User creer(Donnees d) {
        String email = normaliser(d.email());
        if (users.findByEmail(email).isPresent()) {
            throw new BusinessRuleException("Cet e-mail est déjà utilisé");
        }
        verifierMotDePasse(d.motDePasse());
        return users.save(new User(null, d.nom().trim(), email, encoder.encode(d.motDePasse()), true, d.role()));
    }

    /**
     * Modifie un compte. Le mot de passe est facultatif : s'il est vide, l'ancien est conservé.
     * Un administrateur ne peut pas changer son propre rôle (il se retirerait ses droits).
     */
    public User modifier(Long id, Donnees d, String emailAdminConnecte) {
        User actuel = trouver(id);
        String email = normaliser(d.email());

        users.findByEmail(email)
                .filter(u -> !u.id().equals(id))
                .ifPresent(u -> {
                    throw new BusinessRuleException("Cet e-mail est déjà utilisé");
                });

        if (estLuiMeme(actuel, emailAdminConnecte) && d.role() != actuel.role()) {
            throw new BusinessRuleException("Vous ne pouvez pas changer votre propre rôle");
        }

        String hash = actuel.motDePasseHash();
        if (d.motDePasse() != null && !d.motDePasse().isBlank()) {
            verifierMotDePasse(d.motDePasse());
            hash = encoder.encode(d.motDePasse());
        }
        return users.save(new User(id, d.nom().trim(), email, hash, actuel.actif(), d.role()));
    }

    /** Active ou désactive un compte. Un administrateur ne peut pas désactiver son propre compte. */
    public User changerStatut(Long id, boolean actif, String emailAdminConnecte) {
        User actuel = trouver(id);
        if (!actif && estLuiMeme(actuel, emailAdminConnecte)) {
            throw new BusinessRuleException("Vous ne pouvez pas désactiver votre propre compte");
        }
        return users.save(new User(id, actuel.nom(), actuel.email(), actuel.motDePasseHash(), actif, actuel.role()));
    }

    private boolean estLuiMeme(User utilisateur, String emailConnecte) {
        return emailConnecte != null && utilisateur.email().equalsIgnoreCase(emailConnecte);
    }

    private String normaliser(String email) {
        return email.trim().toLowerCase();
    }

    private void verifierMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new BusinessRuleException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères");
        }
    }
}
