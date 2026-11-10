package com.logistock.domain.port;

import com.logistock.domain.model.Facture;
import java.util.Optional;

public interface FactureRepository {

    Optional<Facture> findByCommandeId(Long commandeId);

    /** Nombre de factures déjà émises pour cette année (sert au numéro FAC-AAAA-NNNN). */
    long compterPourAnnee(int annee);

    Facture save(Facture facture);
}
