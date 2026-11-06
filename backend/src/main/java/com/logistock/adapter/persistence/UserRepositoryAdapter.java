package com.logistock.adapter.persistence;

import com.logistock.domain.model.User;
import com.logistock.domain.port.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptateur : traduit les entités JPA en objets métier. */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(e -> new User(
                e.getId(),
                e.getNom(),
                e.getEmail(),
                e.getMotDePasseHash(),
                e.isActif(),
                e.getRole().getCode()));
    }
}
