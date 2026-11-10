package com.logistock.domain.port;

import com.logistock.domain.model.Client;
import java.util.List;
import java.util.Optional;

public interface ClientRepository {

    /** Tous les clients, ou ceux dont le nom ou le prénom contient « recherche ». */
    List<Client> findAll(String recherche);

    Optional<Client> findById(Long id);

    Client save(Client client);

    /** Lève BusinessRuleException si le client a des commandes. */
    void deleteById(Long id);
}
