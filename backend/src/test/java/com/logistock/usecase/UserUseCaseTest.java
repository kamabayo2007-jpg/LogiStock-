package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.logistock.usecase.UserUseCase.Donnees;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserUseCaseTest {

    private static final String ADMIN = "admin@logistock.ml";

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserUseCase useCase = new UserUseCase(users, encoder);

    private User admin() {
        return new User(1L, "Admin", ADMIN, encoder.encode("Admin123!"), true, RoleNom.ADMINISTRATEUR);
    }

    private User commercial() {
        return new User(3L, "Commercial", "vente@logistock.ml", encoder.encode("Vente123!"), true, RoleNom.COMMERCIAL);
    }

    @BeforeEach
    void preparer() {
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.findById(1L)).thenReturn(Optional.of(admin()));
        when(users.findById(3L)).thenReturn(Optional.of(commercial()));
    }

    @Test
    void creationHacheLeMotDePasseEtNormaliseLEmail() {
        when(users.findByEmail("nouveau@logistock.ml")).thenReturn(Optional.empty());

        User cree = useCase.creer(new Donnees(" Aminata ", " Nouveau@Logistock.ML ", "Motdepasse1", RoleNom.COMMERCIAL));

        assertEquals("nouveau@logistock.ml", cree.email());
        assertEquals("Aminata", cree.nom());
        assertNotEquals("Motdepasse1", cree.motDePasseHash());
        assertTrue(encoder.matches("Motdepasse1", cree.motDePasseHash()));
        assertTrue(cree.actif());
    }

    @Test
    void emailDejaUtiliseRefuse() {
        when(users.findByEmail(ADMIN)).thenReturn(Optional.of(admin()));

        assertThrows(BusinessRuleException.class,
                () -> useCase.creer(new Donnees("Autre", ADMIN, "Motdepasse1", RoleNom.COMMERCIAL)));
        verify(users, never()).save(any());
    }

    @Test
    void motDePasseTropCourtRefuse() {
        when(users.findByEmail("x@logistock.ml")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> useCase.creer(new Donnees("X", "x@logistock.ml", "court", RoleNom.COMMERCIAL)));
        assertThrows(BusinessRuleException.class,
                () -> useCase.creer(new Donnees("X", "x@logistock.ml", null, RoleNom.COMMERCIAL)));
    }

    @Test
    void modificationSansMotDePasseGardeLAncienHash() {
        User avant = commercial();
        when(users.findByEmail("vente@logistock.ml")).thenReturn(Optional.of(avant));
        when(users.findById(3L)).thenReturn(Optional.of(avant));

        useCase.modifier(3L, new Donnees("Commercial Senior", "vente@logistock.ml", null, RoleNom.COMMERCIAL), ADMIN);

        ArgumentCaptor<User> sauve = ArgumentCaptor.forClass(User.class);
        verify(users).save(sauve.capture());
        assertEquals(avant.motDePasseHash(), sauve.getValue().motDePasseHash());
        assertEquals("Commercial Senior", sauve.getValue().nom());
    }

    @Test
    void administrateurNePeutPasChangerSonPropreRole() {
        when(users.findByEmail(ADMIN)).thenReturn(Optional.of(admin()));

        assertThrows(BusinessRuleException.class,
                () -> useCase.modifier(1L, new Donnees("Admin", ADMIN, null, RoleNom.COMMERCIAL), ADMIN));
        verify(users, never()).save(any());
    }

    @Test
    void administrateurNePeutPasSeDesactiver() {
        assertThrows(BusinessRuleException.class, () -> useCase.changerStatut(1L, false, ADMIN));
        verify(users, never()).save(any());
    }

    @Test
    void administrateurPeutDesactiverUnAutreCompte() {
        User resultat = useCase.changerStatut(3L, false, ADMIN);

        assertEquals(false, resultat.actif());
    }
}
