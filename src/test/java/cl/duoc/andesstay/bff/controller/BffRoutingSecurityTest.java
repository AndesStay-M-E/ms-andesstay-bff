package cl.duoc.andesstay.bff.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de rol del BFF para los microservicios internos.
 *
 * El JwtDecoder real consulta Azure al arrancar; aquí se reemplaza por uno
 * local que reconoce dos tokens de prueba: uno con rol AUDITOR y otro con ADMIN.
 * Los microservicios no están levantados: si el BFF deja pasar la petición,
 * debe responder 503 "Servicio no disponible".
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "andesstay.services.report-url=http://localhost:1",
        "andesstay.services.audit-url=http://localhost:1",
        "andesstay.services.catalog-url=http://localhost:1",
        "andesstay.services.kafka-admin-url=http://localhost:1",
        "andesstay.services.rabbit-admin-url=http://localhost:1"
})
@AutoConfigureMockMvc
@Import(BffRoutingSecurityTest.LocalJwtDecoder.class)
class BffRoutingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportesSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/reports/summary").with(role("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void panelDeKpisReenviaAlMicroservicio() throws Exception {
        // La ruta existe en el BFF: como report no está levantado, responde 503.
        mockMvc.perform(get("/api/reports/kpis").param("range", "last24h")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio no disponible"));
    }

    @Test
    void auditoriaPermiteAuditor() throws Exception {
        // Token real por header: pasa por el JwtDecoder y el conversor de roles del BFF.
        mockMvc.perform(get("/api/audit/summary")
                        .header("Authorization", "Bearer token-auditor"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio no disponible"));
    }

    @Test
    void auditoriaRechazaCliente() throws Exception {
        mockMvc.perform(get("/api/audit/events").with(role("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorDeKafkaSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/kafka/topics").with(role("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorDeKafkaReenviaAlMicroservicio() throws Exception {
        // ADMIN pasa la seguridad; como el microservicio no está levantado, responde 503.
        mockMvc.perform(get("/api/kafka/consumer-groups")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Servicio no disponible"));
    }

    @Test
    void administradorDeRabbitSoloParaAdmin() throws Exception {
        mockMvc.perform(get("/api/rabbit/queues/q.cmd.email").with(role("AUDITOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogoRequiereToken() throws Exception {
        mockMvc.perform(get("/api/catalog/accommodations"))
                .andExpect(status().isUnauthorized());
    }

    private JwtRequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @TestConfiguration
    static class LocalJwtDecoder {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                List<String> roles = switch (token) {
                    case "token-auditor" -> List.of("AUDITOR");
                    case "token-admin" -> List.of("ADMIN");
                    default -> throw new JwtException("Token de prueba desconocido");
                };
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("roles", roles)
                        .build();
            };
        }
    }
}
