package com.logistock.adapter.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProduitJpaRepository extends JpaRepository<ProduitEntity, Long> {

    /** SELECT ... FOR UPDATE : verrouille la ligne jusqu'à la fin de la transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProduitEntity p where p.id = :id")
    Optional<ProduitEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<ProduitEntity> findByReference(String reference);

    List<ProduitEntity> findAllByOrderByDesignationAsc();

    @Query("select p from ProduitEntity p "
            + "where lower(p.designation) like lower(concat('%', :q, '%')) "
            + "or lower(p.reference) like lower(concat('%', :q, '%')) "
            + "order by p.designation")
    List<ProduitEntity> rechercher(@Param("q") String q);

    @Query("select p from ProduitEntity p where p.quantite <= p.seuilAlerte order by p.quantite, p.designation")
    List<ProduitEntity> findEnAlerte();

    List<ProduitEntity> findByQuantiteOrderByDesignationAsc(int quantite);
}
