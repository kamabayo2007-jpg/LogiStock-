package com.logistock.adapter.persistence;

import com.logistock.domain.model.RoleNom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_role", nullable = false)
    private RoleNom code;

    @Column(name = "libelle_role", nullable = false)
    private String libelle;

    protected RoleEntity() {
    }

    public Long getId() {
        return id;
    }

    public RoleNom getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }
}
