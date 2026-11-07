package com.logistock.domain.port;

import com.logistock.domain.model.User;
import java.util.List;
import java.util.Optional;

/** Port : ce dont le métier a besoin pour retrouver et enregistrer les utilisateurs. */
public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    List<User> findAll();

    /** Crée l'utilisateur (id null) ou met à jour celui qui porte cet id. */
    User save(User user);
}
