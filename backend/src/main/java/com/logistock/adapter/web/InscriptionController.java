package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.InscriptionRequest;
import com.logistock.usecase.InscriptionUseCase;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** US4 : demande de compte (publique). Le compte reste inactif jusqu'à validation par l'administrateur. */
@RestController
@RequestMapping("/api/auth")
public class InscriptionController {

    private final InscriptionUseCase inscription;

    public InscriptionController(InscriptionUseCase inscription) {
        this.inscription = inscription;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> inscrire(@Valid @RequestBody InscriptionRequest r) {
        inscription.inscrire(r.nom(), r.email(), r.motDePasse());
        return Map.of("message",
                "Demande envoyée. Votre compte sera utilisable dès qu'un administrateur l'aura activé.");
    }
}
