package com.logistock.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/inscription", "/api/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATEUR")
                        .requestMatchers("/api/users/**").hasRole("ADMINISTRATEUR")

                        // Catalogue : lecture pour tous les rôles (le commercial doit voir les produits),
                        // écriture réservée au gestionnaire de stock.
                        // Alertes et ruptures : réservées à l'administrateur et au gestionnaire de stock.
                        .requestMatchers(HttpMethod.GET, "/api/produits/alertes", "/api/produits/ruptures")
                        .hasAnyRole("ADMINISTRATEUR", "GESTIONNAIRE_STOCK")
                        .requestMatchers(HttpMethod.GET, "/api/produits/**", "/api/categories/**")
                        .hasAnyRole("ADMINISTRATEUR", "GESTIONNAIRE_STOCK", "COMMERCIAL")
                        .requestMatchers("/api/produits/**", "/api/categories/**")
                        .hasRole("GESTIONNAIRE_STOCK")

                        // Mouvements : l'administrateur peut consulter, seul le gestionnaire enregistre.
                        .requestMatchers(HttpMethod.GET, "/api/mouvements/**")
                        .hasAnyRole("ADMINISTRATEUR", "GESTIONNAIRE_STOCK")
                        .requestMatchers("/api/mouvements/**")
                        .hasRole("GESTIONNAIRE_STOCK")

                        // Ventes : le commercial crée, valide et annule ; l'administrateur consulte.
                        // (Le gestionnaire de stock n'a pas accès aux clients ni aux commandes.)
                        .requestMatchers(HttpMethod.GET, "/api/clients/**", "/api/commandes/**")
                        .hasAnyRole("ADMINISTRATEUR", "COMMERCIAL")
                        .requestMatchers("/api/clients/**", "/api/commandes/**")
                        .hasRole("COMMERCIAL")

                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"message\":\"Accès refusé pour votre rôle\"}");
                        }))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
