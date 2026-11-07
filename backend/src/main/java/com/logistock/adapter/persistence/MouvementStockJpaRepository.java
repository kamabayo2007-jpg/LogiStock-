package com.logistock.adapter.persistence;

import com.logistock.domain.model.TypeMouvement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MouvementStockJpaRepository extends JpaRepository<MouvementStockEntity, Long> {

    List<MouvementStockEntity> findAllByOrderByDateMouvementDescIdDesc();

    List<MouvementStockEntity> findByProduitIdOrderByDateMouvementDescIdDesc(Long produitId);

    List<MouvementStockEntity> findByTypeMouvementOrderByDateMouvementDescIdDesc(TypeMouvement type);

    List<MouvementStockEntity> findByProduitIdAndTypeMouvementOrderByDateMouvementDescIdDesc(
            Long produitId, TypeMouvement type);
}
