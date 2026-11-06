package com.logistock.adapter.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategorieJpaRepository extends JpaRepository<CategorieEntity, Long> {

    Optional<CategorieEntity> findByNom(String nom);

    List<CategorieEntity> findAllByOrderByNomAsc();
}
