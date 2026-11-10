package com.logistock.adapter.web.dto;

import com.logistock.usecase.FactureUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureResponse(
        Long id,
        String numero,
        LocalDate date,
        BigDecimal montantTotal,
        Long commandeId,
        String clientNom) {

    public static FactureResponse from(FactureUseCase.Detail d) {
        return new FactureResponse(d.facture().id(), d.facture().numero(), d.facture().date(),
                d.facture().montantTotal(), d.facture().commandeId(), d.client().nomComplet());
    }
}
