package cl.duoc.andesstay.bff.controller;

import cl.duoc.andesstay.bff.client.ReservationsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationsProxyController {

    private final ReservationsClient reservationsClient;

    public ReservationsProxyController(
            ReservationsClient reservationsClient
    ) {
        this.reservationsClient = reservationsClient;
    }

    @GetMapping
    public ResponseEntity<String> findAll(
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.findAll(authorization);
    }

    @GetMapping("/me")
    public ResponseEntity<String> findMine(
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.findMine(authorization);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> findById(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.findById(id, authorization);
    }

    @PostMapping
    public ResponseEntity<String> create(
            @RequestBody String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.create(body, authorization);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable Long id,
            @RequestBody String body,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.update(
                id,
                body,
                authorization
        );
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<String> confirm(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.confirm(id, authorization);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorization
    ) {
        return reservationsClient.cancel(id, authorization);
    }
}