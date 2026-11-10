package com.logistock.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContenirJpaRepository extends JpaRepository<ContenirEntity, ContenirId> {

    List<ContenirEntity> findByCommandeIdOrderByProduitId(Long commandeId);

    /** Supprime d'un coup toutes les lignes d'une commande (le contexte JPA est vidé pour éviter les lignes périmées). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ContenirEntity c where c.commandeId = :commandeId")
    void supprimerParCommande(@Param("commandeId") Long commandeId);
}
