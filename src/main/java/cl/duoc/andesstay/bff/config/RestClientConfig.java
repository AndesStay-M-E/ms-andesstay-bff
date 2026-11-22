package cl.duoc.andesstay.bff.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String FORWARDED = "Forwarded";

    @Bean("reservationsRestClient")
    RestClient reservationsRestClient(
            @Value("${andesstay.services.reservations-url}")
            String reservationsUrl
    ) {
        return RestClient.builder()
                .baseUrl(reservationsUrl)
                .requestInterceptor(forwardClientIp())
                .build();
    }

    /**
     * Cliente genérico para catalog, report, audit y los administradores.
     * Cada controlador proxy indica la URL base del microservicio
     * (andesstay.services.*-url).
     */
    @Bean("downstreamRestClient")
    RestClient downstreamRestClient() {
        return RestClient.builder()
                .requestInterceptor(forwardClientIp())
                .build();
    }

    /**
     * Reenvía la IP del usuario a los microservicios en la cabecera
     * X-Forwarded-For ("ip-del-usuario, ip-que-llamó-al-bff"). Reservas toma
     * la primera para registrar en la auditoría DESDE DÓNDE se hizo cada cambio.
     *
     * En AWS la IP del usuario viene en el header "Forwarded" que agrega
     * API Gateway (ver ClientIp).
     */
    private static ClientHttpRequestInterceptor forwardClientIp() {
        return (request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest incoming = attributes.getRequest();

                String chain = ClientIp.chain(
                        incoming.getHeader(X_FORWARDED_FOR),
                        incoming.getHeader(FORWARDED),
                        incoming.getRemoteAddr()
                );

                if (chain != null && !chain.isBlank()) {
                    request.getHeaders().set(X_FORWARDED_FOR, chain);
                }
            }
            return execution.execute(request, body);
        };
    }
}
