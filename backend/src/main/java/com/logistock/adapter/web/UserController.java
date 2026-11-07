package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.StatutRequest;
import com.logistock.adapter.web.dto.UserRequest;
import com.logistock.adapter.web.dto.UserResponse;
import com.logistock.usecase.UserUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Réservé à l'ADMINISTRATEUR (règle posée dans SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase useCase;

    public UserController(UserUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<UserResponse> lister() {
        return useCase.lister().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse trouver(@PathVariable Long id) {
        return UserResponse.from(useCase.trouver(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse creer(@Valid @RequestBody UserRequest requete) {
        return UserResponse.from(useCase.creer(requete.toDonnees()));
    }

    @PutMapping("/{id}")
    public UserResponse modifier(@PathVariable Long id,
                                 @Valid @RequestBody UserRequest requete,
                                 Authentication authentication) {
        return UserResponse.from(useCase.modifier(id, requete.toDonnees(), authentication.getName()));
    }

    @PatchMapping("/{id}/actif")
    public UserResponse changerStatut(@PathVariable Long id,
                                      @Valid @RequestBody StatutRequest requete,
                                      Authentication authentication) {
        return UserResponse.from(useCase.changerStatut(id, requete.actif(), authentication.getName()));
    }
}
