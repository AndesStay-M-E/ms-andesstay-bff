package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.DownstreamClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy hacia ms-andesstay-audit (ADMIN o AUDITOR, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/audit")
public class AuditProxyController {

    private final DownstreamClient client;
    private final String auditUrl;

    public AuditProxyController(
            DownstreamClient client,
            @Value("${andesstay.services.audit-url}") String auditUrl
    ) {
        this.client = client;
        this.auditUrl = auditUrl;
    }

    @GetMapping("/events")
    public ResponseEntity<String> events(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(auditUrl, HttpMethod.GET, "/api/audit/events",
                request.getQueryString(), null, authorization);
    }

    @GetMapping("/reservations/{reservationId}/timeline")
    public ResponseEntity<String> timeline(
            @PathVariable Long reservationId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(auditUrl, HttpMethod.GET,
                "/api/audit/reservations/" + reservationId + "/timeline",
                null, null, authorization);
    }

    @GetMapping("/summary")
    public ResponseEntity<String> summary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(auditUrl, HttpMethod.GET, "/api/audit/summary",
                null, null, authorization);
    }
}
