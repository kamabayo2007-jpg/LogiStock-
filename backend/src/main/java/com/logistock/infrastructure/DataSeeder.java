package com.logistock.infrastructure;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Au démarrage, remplace le marqueur HASH_A_REMPLACER de seed.sql
 * par de vrais hash BCrypt pour les 3 comptes de démonstration.
 * Ne touche jamais à un compte dont le mot de passe a déjà été défini.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String MARQUEUR = "HASH_A_REMPLACER";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public DataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        Map<String, String> comptes = Map.of(
                "admin@logistock.ml", "Admin123!",
                "stock@logistock.ml", "Stock123!",
                "vente@logistock.ml", "Vente123!");

        comptes.forEach((email, motDePasse) -> {
            int n = jdbc.update(
                    "UPDATE utilisateur SET mot_de_passe_hache = ? WHERE email = ? AND mot_de_passe_hache = ?",
                    encoder.encode(motDePasse), email, MARQUEUR);
            if (n > 0) {
                log.info("Mot de passe de démonstration défini pour {}", email);
            }
        });
    }
}
