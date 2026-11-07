package com.logistock.adapter.persistence;

import com.logistock.domain.model.MouvementStock;
import com.logistock.domain.model.TypeMouvement;
import com.logistock.domain.port.MouvementStockRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MouvementStockRepositoryAdapter implements MouvementStockRepository {

    private final MouvementStockJpaRepository jpa;

    public MouvementStockRepositoryAdapter(MouvementStockJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MouvementStock save(MouvementStock m) {
        MouvementStockEntity entite = new MouvementStockEntity(m.id(), m.date(), m.type(), m.quantite(),
                m.motif(), m.produitId(), m.utilisateurId(), m.commandeId());
        return toDomain(jpa.save(entite));
    }

    @Override
    public List<MouvementStock> findAll(Long produitId, TypeMouvement type) {
        List<MouvementStockEntity> resultat;
        if (produitId != null && type != null) {
            resultat = jpa.findByProduitIdAndTypeMouvementOrderByDateMouvementDescIdDesc(produitId, type);
        } else if (produitId != null) {
            resultat = jpa.findByProduitIdOrderByDateMouvementDescIdDesc(produitId);
        } else if (type != null) {
            resultat = jpa.findByTypeMouvementOrderByDateMouvementDescIdDesc(type);
        } else {
            resultat = jpa.findAllByOrderByDateMouvementDescIdDesc();
        }
        return resultat.stream().map(this::toDomain).toList();
    }

    private MouvementStock toDomain(MouvementStockEntity e) {
        return new MouvementStock(e.getId(), e.getDateMouvement(), e.getTypeMouvement(), e.getQuantite(),
                e.getMotif(), e.getProduitId(), e.getUtilisateurId(), e.getCommandeId());
    }
}
