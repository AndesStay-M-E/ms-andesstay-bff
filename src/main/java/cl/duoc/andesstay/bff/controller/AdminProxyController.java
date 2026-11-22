package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.DownstreamClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy hacia los microservicios administradores (solo ADMIN, ver SecurityConfig).
 *
 *   /api/rabbit/**  ->  ms-andesstay-rabbit-admin
 *   /api/kafka/**   ->  ms-andesstay-kafka-admin
 *
 * Así se cumple el flujo del caso: JWT -> API Gateway -> BFF -> microservicio.
 * Se reenvía el mismo método, ruta, query string, cuerpo y token.
 */
@RestController
public class AdminProxyController {

    private final DownstreamClient client;
    private final String rabbitAdminUrl;
    private final String kafkaAdminUrl;

    public AdminProxyController(
            DownstreamClient client,
            @Value("${andesstay.services.rabbit-admin-url}") String rabbitAdminUrl,
            @Value("${andesstay.services.kafka-admin-url}") String kafkaAdminUrl
    ) {
        this.client = client;
        this.rabbitAdminUrl = rabbitAdminUrl;
        this.kafkaAdminUrl = kafkaAdminUrl;
    }

    @RequestMapping("/api/rabbit/**")
    public ResponseEntity<String> rabbit(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return forward(rabbitAdminUrl, request, body, authorization);
    }

    @RequestMapping("/api/kafka/**")
    public ResponseEntity<String> kafka(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return forward(kafkaAdminUrl, request, body, authorization);
    }

    private ResponseEntity<String> forward(
            String baseUrl,
            HttpServletRequest request,
            String body,
            String authorization
    ) {
        return client.forward(
                baseUrl,
                HttpMethod.valueOf(request.getMethod()),
                request.getRequestURI(),
                request.getQueryString(),
                body,
                authorization
        );
    }
}
