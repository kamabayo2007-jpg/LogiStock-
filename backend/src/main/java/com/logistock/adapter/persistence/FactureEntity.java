package com.logistock.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "facture")
public class FactureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facture")
    private Long id;

    @Column(name = "numero_facture", nullable = false)
    private String numero;

    @Column(name = "date_facture", nullable = false)
    private LocalDate date;

    @Column(name = "montant_total", nullable = false)
    private BigDecimal montantTotal;

    @Column(name = "id_commande", nullable = false)
    private Long commandeId;

    protected FactureEntity() {
    }

    public FactureEntity(Long id, String numero, LocalDate date, BigDecimal montantTotal, Long commandeId) {
        this.id = id;
        this.numero = numero;
        this.date = date;
        this.montantTotal = montantTotal;
        this.commandeId = commandeId;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public LocalDate getDate() { return date; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public Long getCommandeId() { return commandeId; }
}
