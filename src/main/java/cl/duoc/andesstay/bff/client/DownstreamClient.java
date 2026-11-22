package cl.duoc.andesstay.bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Reenvía una petición del frontend a un microservicio interno.
 *
 * Copia el método HTTP, la ruta, los parámetros de consulta, el cuerpo JSON
 * y el header Authorization (el JWT del usuario). Devuelve la respuesta del
 * microservicio tal cual (código HTTP, Content-Type, Location y cuerpo), de modo
 * que los errores 400/403/404/409 llegan al frontend con su detalle.
 */
@Component
public class DownstreamClient {

    private final RestClient restClient;

    public DownstreamClient(
            @Qualifier("downstreamRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    /**
     * @param baseUrl       URL base del microservicio (http://localhost:8082)
     * @param method        método HTTP
     * @param path          ruta ya codificada (/api/catalog/accommodations/3)
     * @param rawQuery      query string original, ya codificada (puede ser null)
     * @param body          cuerpo JSON (null para GET/DELETE)
     * @param authorization header Authorization recibido ("Bearer ...")
     */
    public ResponseEntity<String> forward(
            String baseUrl,
            HttpMethod method,
            String path,
            String rawQuery,
            String body,
            String authorization
    ) {
        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(path)
                .query(rawQuery)
                .build(true)
                .toUri();

        RestClient.RequestBodySpec request = restClient
                .method(method)
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorization);

        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }

        return request.exchange((httpRequest, httpResponse) -> {
            String responseBody = StreamUtils.copyToString(
                    httpResponse.getBody(),
                    StandardCharsets.UTF_8
            );

            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = httpResponse.getHeaders().getContentType();
            if (contentType != null) {
                headers.setContentType(contentType);
            }

            // En los 201 Created se copia también el Location (ej. /api/kafka/topics/demo.events).
            URI location = httpResponse.getHeaders().getLocation();
            if (location != null) {
                headers.setLocation(location);
            }

            return new ResponseEntity<>(responseBody, headers, httpResponse.getStatusCode());
        });
    }
}
