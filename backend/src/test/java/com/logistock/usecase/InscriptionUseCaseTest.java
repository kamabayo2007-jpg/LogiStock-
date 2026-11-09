package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistock.domain.exception.BusinessRuleException;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.User;
import com.logistock.domain.port.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class InscriptionUseCaseTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final InscriptionUseCase useCase = new InscriptionUseCase(users, encoder);

    @BeforeEach
    void preparer() {
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void lInscriptionCreeUnCompteInactifCommercialAvecMotDePasseHache() {
        when(users.findByEmail("nouveau@logistock.ml")).thenReturn(Optional.empty());

        User cree = useCase.inscrire(" Aminata ", " Nouveau@Logistock.ML ", "Motdepasse1");

        assertFalse(cree.actif());
        assertEquals(RoleNom.COMMERCIAL, cree.role());
        assertEquals("nouveau@logistock.ml", cree.email());
        assertEquals("Aminata", cree.nom());
        assertNotEquals("Motdepasse1", cree.motDePasseHash());
        assertTrue(encoder.matches("Motdepasse1", cree.motDePasseHash()));
    }

    @Test
    void unEmailDejaUtiliseEstRefuse() {
        User existant = new User(3L, "Commercial", "vente@logistock.ml", "x", true, RoleNom.COMMERCIAL);
        when(users.findByEmail("vente@logistock.ml")).thenReturn(Optional.of(existant));

        assertThrows(BusinessRuleException.class,
                () -> useCase.inscrire("Autre", "vente@logistock.ml", "Motdepasse1"));
        verify(users, never()).save(any());
    }

    @Test
    void unMotDePasseTropCourtEstRefuse() {
        when(users.findByEmail("court@logistock.ml")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> useCase.inscrire("Court", "court@logistock.ml", "abc"));
        verify(users, never()).save(any());
    }
}
