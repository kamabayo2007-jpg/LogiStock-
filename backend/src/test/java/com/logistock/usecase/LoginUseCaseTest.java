package com.logistock.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.logistock.domain.exception.InvalidCredentialsException;
import com.logistock.domain.model.RoleNom;
import com.logistock.domain.model.User;
import com.logistock.domain.port.TokenProvider;
import com.logistock.domain.port.UserRepository;
import com.logistock.usecase.LoginUseCase.LoginResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginUseCaseTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final TokenProvider tokens = u -> "token-" + u.email();
    private final LoginUseCase useCase = new LoginUseCase(repo, encoder, tokens);

    private User utilisateur(boolean actif) {
        return new User(1L, "Admin", "admin@logistock.ml",
                encoder.encode("Admin123!"), actif, RoleNom.ADMINISTRATEUR);
    }

    @Test
    void connexionReussie() {
        when(repo.findByEmail("admin@logistock.ml")).thenReturn(Optional.of(utilisateur(true)));

        LoginResult result = useCase.execute("admin@logistock.ml", "Admin123!");

        assertEquals("token-admin@logistock.ml", result.token());
        assertEquals(RoleNom.ADMINISTRATEUR, result.user().role());
    }

    @Test
    void mauvaisMotDePasseRefuse() {
        when(repo.findByEmail("admin@logistock.ml")).thenReturn(Optional.of(utilisateur(true)));

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("admin@logistock.ml", "faux"));
    }

    @Test
    void compteDesactiveRefuse() {
        when(repo.findByEmail("admin@logistock.ml")).thenReturn(Optional.of(utilisateur(false)));

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("admin@logistock.ml", "Admin123!"));
    }

    @Test
    void emailInconnuRefuse() {
        when(repo.findByEmail("inconnu@logistock.ml")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> useCase.execute("inconnu@logistock.ml", "Admin123!"));
    }
}
