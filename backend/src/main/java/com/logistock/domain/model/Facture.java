package com.logistock.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Facture(
        Long id,
        String numero,
        LocalDate date,
        BigDecimal montantTotal,
        Long commandeId) {
}
