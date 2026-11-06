package com.logistock.domain.exception;

/** Élément introuvable (produit, catégorie...) : répondu en 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
