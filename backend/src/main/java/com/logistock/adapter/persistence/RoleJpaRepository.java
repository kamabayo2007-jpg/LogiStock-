package com.logistock.adapter.persistence;

import com.logistock.domain.model.RoleNom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(RoleNom code);
}
