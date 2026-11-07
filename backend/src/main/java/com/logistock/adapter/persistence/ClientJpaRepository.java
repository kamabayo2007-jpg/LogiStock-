package com.logistock.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {

    List<ClientEntity> findAllByOrderByNomAsc();

    @Query("select c from ClientEntity c "
            + "where lower(c.nom) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(c.prenom, '')) like lower(concat('%', :q, '%')) "
            + "order by c.nom")
    List<ClientEntity> rechercher(@Param("q") String q);
}
