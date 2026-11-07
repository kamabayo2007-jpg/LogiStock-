package com.logistock.adapter.persistence;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.Client;
import com.logistock.domain.port.ClientRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ClientRepositoryAdapter implements ClientRepository {

    private final ClientJpaRepository jpa;

    public ClientRepositoryAdapter(ClientJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Client> findAll(String recherche) {
        List<ClientEntity> resultat = (recherche == null || recherche.isBlank())
                ? jpa.findAllByOrderByNomAsc()
                : jpa.rechercher(recherche.trim());
        return resultat.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Client save(Client c) {
        return toDomain(jpa.save(new ClientEntity(c.id(), c.nom(), c.prenom(), c.telephone(), c.email(), c.adresse())));
    }

    @Override
    public void deleteById(Long id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Ce client a des commandes : suppression impossible");
        }
    }

    private Client toDomain(ClientEntity e) {
        return new Client(e.getId(), e.getNom(), e.getPrenom(), e.getTelephone(), e.getEmail(), e.getAdresse());
    }
}
