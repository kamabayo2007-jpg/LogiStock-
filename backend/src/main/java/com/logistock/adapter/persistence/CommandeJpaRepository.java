package com.logistock.adapter.persistence;

import com.logistock.domain.model.StatutCommande;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandeJpaRepository extends JpaRepository<CommandeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommandeEntity c where c.id = :id")
    Optional<CommandeEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select c from CommandeEntity c "
            + "where (:statut is null or c.statut = :statut) "
            + "and (:clientId is null or c.clientId = :clientId) "
            + "order by c.date desc, c.id desc")
    List<CommandeEntity> rechercher(@Param("statut") StatutCommande statut, @Param("clientId") Long clientId);
}
