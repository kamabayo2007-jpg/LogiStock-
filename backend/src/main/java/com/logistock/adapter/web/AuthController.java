package com.logistock.adapter.web;

import com.logistock.adapter.web.dto.LoginRequest;
import com.logistock.adapter.web.dto.LoginResponse;
import com.logistock.adapter.web.dto.LoginResponse.UserDto;
import com.logistock.domain.model.User;
import com.logistock.usecase.LoginUseCase;
import com.logistock.usecase.LoginUseCase.LoginResult;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(request.email(), request.password());
        User u = result.user();
        return new LoginResponse(result.token(),
                new UserDto(u.id(), u.nom(), u.email(), u.role().name()));
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return Map.of("email", authentication.getName(), "role", role);
    }
}
