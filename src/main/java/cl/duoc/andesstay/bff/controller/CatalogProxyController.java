package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.DownstreamClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy hacia ms-andesstay-catalog. Los roles finos los valida el microservicio.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogProxyController {

    private static final String ACCOMMODATIONS = "/api/catalog/accommodations";
    private static final String TICKETS = "/api/catalog/housekeeping-tickets";

    private final DownstreamClient client;
    private final String catalogUrl;

    public CatalogProxyController(
            DownstreamClient client,
            @Value("${andesstay.services.catalog-url}") String catalogUrl
    ) {
        this.client = client;
        this.catalogUrl = catalogUrl;
    }

    @GetMapping("/accommodations")
    public ResponseEntity<String> findAccommodations(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.GET, ACCOMMODATIONS,
                request.getQueryString(), null, authorization);
    }

    @GetMapping("/accommodations/{id}")
    public ResponseEntity<String> findAccommodation(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.GET, ACCOMMODATIONS + "/" + id,
                null, null, authorization);
    }

    @PostMapping("/accommodations")
    public ResponseEntity<String> createAccommodation(
            @RequestBody String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.POST, ACCOMMODATIONS,
                null, body, authorization);
    }

    @PutMapping("/accommodations/{id}")
    public ResponseEntity<String> updateAccommodation(
            @PathVariable Long id,
            @RequestBody String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.PUT, ACCOMMODATIONS + "/" + id,
                null, body, authorization);
    }

    @DeleteMapping("/accommodations/{id}")
    public ResponseEntity<String> deactivateAccommodation(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.DELETE, ACCOMMODATIONS + "/" + id,
                null, null, authorization);
    }

    @GetMapping("/housekeeping-tickets")
    public ResponseEntity<String> findTickets(
            HttpServletRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.GET, TICKETS,
                request.getQueryString(), null, authorization);
    }

    @PatchMapping("/housekeeping-tickets/{id}/complete")
    public ResponseEntity<String> completeTicket(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return client.forward(catalogUrl, HttpMethod.PATCH, TICKETS + "/" + id + "/complete",
                null, null, authorization);
    }
}
