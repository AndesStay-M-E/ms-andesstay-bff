package cl.duoc.andesstay.bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Component
public class ReservationsClient {

    private final RestClient restClient;

    public ReservationsClient(
            @Qualifier("reservationsRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public ResponseEntity<String> findAll(String authorization) {
        return exchange(
                restClient.get()
                        .uri("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
        );
    }

    public ResponseEntity<String> findById(
            Long id,
            String authorization
    ) {
        return exchange(
                restClient.get()
                        .uri("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
        );
    }

    public ResponseEntity<String> findMine(String authorization) {
        return exchange(
                restClient.get()
                        .uri("/api/reservations/me")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
        );
    }

    public ResponseEntity<String> create(
            String body,
            String authorization
    ) {
        return exchange(
                restClient.post()
                        .uri("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }

    public ResponseEntity<String> update(
            Long id,
            String body,
            String authorization
    ) {
        return exchange(
                restClient.put()
                        .uri("/api/reservations/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }

    public ResponseEntity<String> confirm(
            Long id,
            String authorization
    ) {
        return exchange(
                restClient.patch()
                        .uri("/api/reservations/{id}/confirm", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
        );
    }

    public ResponseEntity<String> cancel(
            Long id,
            String authorization
    ) {
        return exchange(
                restClient.patch()
                        .uri("/api/reservations/{id}/cancel", id)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
        );
    }

    private ResponseEntity<String> exchange(
            RestClient.RequestHeadersSpec<?> request
    ) {
        return request.exchange((httpRequest, httpResponse) -> {
            String responseBody = StreamUtils.copyToString(
                    httpResponse.getBody(),
                    StandardCharsets.UTF_8
            );

            HttpHeaders responseHeaders = new HttpHeaders();

            MediaType contentType =
                    httpResponse.getHeaders().getContentType();

            if (contentType != null) {
                responseHeaders.setContentType(contentType);
            }

            return new ResponseEntity<>(
                    responseBody,
                    responseHeaders,
                    httpResponse.getStatusCode()
            );
        });
    }
}