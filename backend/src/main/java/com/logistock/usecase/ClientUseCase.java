package com.logistock.usecase;

import com.logistock.domain.exception.ResourceNotFoundException;
import com.logistock.domain.model.Client;
import com.logistock.domain.port.ClientRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases : gestion des clients (US10). */
@Service
@Transactional
public class ClientUseCase {

    public record Donnees(String nom, String prenom, String telephone, String email, String adresse) {
    }

    private final ClientRepository clients;

    public ClientUseCase(ClientRepository clients) {
        this.clients = clients;
    }

    @Transactional(readOnly = true)
    public List<Client> lister(String recherche) {
        return clients.findAll(recherche);
    }

    @Transactional(readOnly = true)
    public Client trouver(Long id) {
        return clients.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));
    }

    public Client creer(Donnees d) {
        return clients.save(versClient(null, d));
    }

    public Client modifier(Long id, Donnees d) {
        trouver(id);
        return clients.save(versClient(id, d));
    }

    public void supprimer(Long id) {
        trouver(id);
        clients.deleteById(id);
    }

    private static Client versClient(Long id, Donnees d) {
        return new Client(id, d.nom().trim(), vide(d.prenom()), vide(d.telephone()),
                d.email() == null || d.email().isBlank() ? null : d.email().trim().toLowerCase(),
                vide(d.adresse()));
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
