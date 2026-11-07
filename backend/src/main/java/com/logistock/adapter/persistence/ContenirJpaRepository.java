package com.logistock.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContenirJpaRepository extends JpaRepository<ContenirEntity, ContenirId> {

    List<ContenirEntity> findByCommandeIdOrderByProduitId(Long commandeId);
}
