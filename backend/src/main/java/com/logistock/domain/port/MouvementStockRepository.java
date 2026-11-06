package com.logistock.domain.port;

import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.TypeMouvement;
import java.util.List;

public interface MouvementStockRepository {

    MouvementStock save(MouvementStock mouvement);

    /** Historique, du plus récent au plus ancien. Chaque filtre est facultatif (null = pas de filtre). */
    List<MouvementStock> findAll(Long produitId, TypeMouvement type);
}
