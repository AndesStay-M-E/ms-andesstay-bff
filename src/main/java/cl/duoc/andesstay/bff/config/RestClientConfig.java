package cl.duoc.andesstay.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("reservationsRestClient")
    RestClient reservationsRestClient(
            @Value("${andesstay.services.reservations-url}")
            String reservationsUrl
    ) {
        return RestClient.builder()
                .baseUrl(reservationsUrl)
                .build();
    }
}
