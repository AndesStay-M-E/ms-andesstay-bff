package cl.duoc.andesstay.bff.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DownstreamClientTest {

    private static final String TOKEN = "Bearer token-de-prueba";

    private MockRestServiceServer server;
    private DownstreamClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DownstreamClient(builder.build());
    }

    @Test
    void reenviaRutaQueryYTokenSinRecodificar() {
        server.expect(requestTo("http://localhost:8085/api/audit/events?userEmail=ana%40andesstay.cl&limit=50"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.forward(
                "http://localhost:8085", HttpMethod.GET, "/api/audit/events",
                "userEmail=ana%40andesstay.cl&limit=50", null, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        server.verify();
    }

    @Test
    void reenviaElCuerpoJson() {
        server.expect(requestTo("http://localhost:8082/api/catalog/accommodations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"name\":\"Cabaña\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":5}"));

        ResponseEntity<String> response = client.forward(
                "http://localhost:8082", HttpMethod.POST, "/api/catalog/accommodations",
                null, "{\"name\":\"Cabaña\"}", TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        server.verify();
    }

    @Test
    void devuelveLosErroresDelMicroservicioTalCual() {
        server.expect(requestTo("http://localhost:8084/api/reports/summary"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        ResponseEntity<String> response = client.forward(
                "http://localhost:8084", HttpMethod.GET, "/api/reports/summary",
                null, null, TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
