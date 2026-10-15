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
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Proxy hacia ms-andesstay-report (solo ADMIN, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportsProxyController {

    private final DownstreamClient client;
    private final String reportUrl;

    public ReportsProxyController(
            DownstreamClient client,
            @Value("${andesstay.services.report-url}") String reportUrl
    ) {
        this.client = client;
        this.reportUrl = reportUrl;
    }

    @GetMapping("/summary")
    public ResponseEntity<String> summary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(reportUrl, HttpMethod.GET, "/api/reports/summary",
                null, null, authorization);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<String> vouchers(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(reportUrl, HttpMethod.GET, "/api/reports/vouchers",
                request.getQueryString(), null, authorization);
    }

    @GetMapping("/vouchers/{code}")
    public ResponseEntity<String> voucher(
            @PathVariable String code,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        String encodedCode = UriUtils.encodePathSegment(code, StandardCharsets.UTF_8);
        return client.forward(reportUrl, HttpMethod.GET, "/api/reports/vouchers/" + encodedCode,
                null, null, authorization);
    }
}
