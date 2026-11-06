package com.logistock.adapter.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Route de test du RBAC : seul l'ADMIN passe (à supprimer plus tard).
    @GetMapping("/admin/ping")
    public Map<String, String> adminPing() {
        return Map.of("message", "Accès administrateur OK");
    }
}
