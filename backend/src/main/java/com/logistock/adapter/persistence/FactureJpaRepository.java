package com.logistock.adapter.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactureJpaRepository extends JpaRepository<FactureEntity, Long> {

    Optional<FactureEntity> findByCommandeId(Long commandeId);

    @Query("select count(f) from FactureEntity f where year(f.date) = :annee")
    long compterPourAnnee(@Param("annee") int annee);
}
