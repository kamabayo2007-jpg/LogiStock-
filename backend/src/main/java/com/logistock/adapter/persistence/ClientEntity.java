package com.logistock.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_client")
    private Long id;

    @Column(name = "nom_client", nullable = false)
    private String nom;

    @Column(name = "prenom_client")
    private String prenom;

    @Column
    private String telephone;

    @Column(name = "email_client")
    private String email;

    @Column
    private String adresse;

    protected ClientEntity() {
    }

    public ClientEntity(Long id, String nom, String prenom, String telephone, String email, String adresse) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getTelephone() { return telephone; }
    public String getEmail() { return email; }
    public String getAdresse() { return adresse; }
}
