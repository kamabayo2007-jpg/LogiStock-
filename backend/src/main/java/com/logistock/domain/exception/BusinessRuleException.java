package com.logistock.domain.exception;

/** Règle de gestion non respectée (stock insuffisant, doublon...) : répondu en 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
