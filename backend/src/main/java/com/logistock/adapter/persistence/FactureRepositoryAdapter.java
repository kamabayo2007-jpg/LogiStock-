package com.logistock.adapter.persistence;

import com.logistock.domain.model.Facture;
import com.logistock.domain.port.FactureRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FactureRepositoryAdapter implements FactureRepository {

    private final FactureJpaRepository jpa;

    public FactureRepositoryAdapter(FactureJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Facture> findByCommandeId(Long commandeId) {
        return jpa.findByCommandeId(commandeId).map(this::toDomain);
    }

    @Override
    public long compterPourAnnee(int annee) {
        return jpa.compterPourAnnee(annee);
    }

    @Override
    public Facture save(Facture f) {
        return toDomain(jpa.save(new FactureEntity(f.id(), f.numero(), f.date(), f.montantTotal(), f.commandeId())));
    }

    private Facture toDomain(FactureEntity e) {
        return new Facture(e.getId(), e.getNumero(), e.getDate(), e.getMontantTotal(), e.getCommandeId());
    }
}
