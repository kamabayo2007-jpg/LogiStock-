package com.logistock.usecase;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.User;
import com.logistock.domain.port.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case : inscription d'un utilisateur (US4).
 *
 * Le compte créé est INACTIF et reçoit le rôle le moins privilégié (COMMERCIAL) :
 * personne ne peut se connecter tant qu'un administrateur n'a pas activé le compte
 * (et éventuellement changé son rôle) depuis l'écran « Comptes ».
 */
@Service
@Transactional
public class InscriptionUseCase {

    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public InscriptionUseCase(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User inscrire(String nom, String email, String motDePasse) {
        String emailNormalise = email.trim().toLowerCase();
        if (users.findByEmail(emailNormalise).isPresent()) {
            throw new BusinessRuleException("Cet e-mail est déjà utilisé");
        }
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new BusinessRuleException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères");
        }
        return users.save(new User(null, nom.trim(), emailNormalise,
                encoder.encode(motDePasse), false, RoleNom.COMMERCIAL));
    }
}
