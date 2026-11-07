package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.ClientRequest;
import com.logistock.adapter.web.dto.CommandeResponse;
import com.logistock.domain.model.Client;
import com.logistock.usecase.ClientUseCase;
import com.logistock.usecase.CommandeUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientUseCase clients;
    private final CommandeUseCase commandes;

    public ClientController(ClientUseCase clients, CommandeUseCase commandes) {
        this.clients = clients;
        this.commandes = commandes;
    }

    @GetMapping
    public List<Client> lister(@RequestParam(name = "q", required = false) String recherche) {
        return clients.lister(recherche);
    }

    @GetMapping("/{id}")
    public Client trouver(@PathVariable Long id) {
        return clients.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Client creer(@Valid @RequestBody ClientRequest requete) {
        return clients.creer(requete.toDonnees());
    }

    @PutMapping("/{id}")
    public Client modifier(@PathVariable Long id, @Valid @RequestBody ClientRequest requete) {
        return clients.modifier(id, requete.toDonnees());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        clients.supprimer(id);
    }

    /** US14 : historique d'achats du client. */
    @GetMapping("/{id}/commandes")
    public List<CommandeResponse> historique(@PathVariable Long id) {
        return commandes.historiqueClient(id).stream().map(CommandeResponse::from).toList();
    }
}
