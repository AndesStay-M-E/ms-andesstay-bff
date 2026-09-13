package cl.duoc.andesstay.bff.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SecurityTestController {

    @GetMapping("/public/status")
    public Map<String, Object> publicStatus() {
        return Map.of(
                "service", "ms-andesstay-bff",
                "status", "UP",
                "security", "PUBLIC");
    }

    @GetMapping("/secure/me")
    public Map<String, Object> currentUser(
            JwtAuthenticationToken authentication) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("authenticated", true);
        response.put(
                "username",
                authentication.getToken()
                        .getClaimAsString("preferred_username"));
        response.put(
                "name",
                authentication.getToken().getClaimAsString("name"));
        response.put(
                "roles",
                authentication.getToken()
                        .getClaimAsStringList("roles"));
        response.put(
                "audience",
                authentication.getToken().getAudience());
        response.put(
                "issuer",
                authentication.getToken().getIssuer());

        return response;
    }

    @GetMapping("/admin/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminStatus() {
        return Map.of(
                "authorized", true,
                "requiredRole", "ADMIN",
                "message", "Acceso administrativo autorizado");
    }
}
