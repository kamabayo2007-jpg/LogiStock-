package com.logistock.usecase;

import com.logistock.domain.exception.InvalidCredentialsException;
import com.logistock.domain.model.User;
import com.logistock.domain.port.TokenProvider;
import com.logistock.domain.port.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Use case : connexion d'un utilisateur (US1). */
@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public LoginUseCase(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResult execute(String email, String password) {
        // Même erreur dans tous les cas : on ne révèle pas si l'e-mail existe.
        User user = userRepository.findByEmail(email)
                .filter(User::actif)
                .filter(u -> passwordEncoder.matches(password, u.motDePasseHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return new LoginResult(tokenProvider.generate(user), user);
    }

    public record LoginResult(String token, User user) {
    }
}
