package com.logistock.adapter.persistence;

import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.User;
import com.logistock.domain.port.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptateur : traduit les entités JPA en objets métier. */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final RoleJpaRepository roles;

    public UserRepositoryAdapter(UserJpaRepository jpa, RoleJpaRepository roles) {
        this.jpa = jpa;
        this.roles = roles;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAllByOrderByNomAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public User save(User u) {
        RoleEntity role = roles.findByCode(u.role())
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable"));
        UserEntity entite = new UserEntity(u.id(), u.nom(), u.email(), u.motDePasseHash(), u.actif(), role);
        return toDomain(jpa.save(entite));
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getNom(), e.getEmail(), e.getMotDePasseHash(),
                e.isActif(), e.getRole().getCode());
    }
}
