package cl.duoc.andesstay.bff.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo obtiene el BFF la IP del usuario según por dónde llegó la petición.
 */
class ClientIpTest {

    private static final String API_GATEWAY_IP = "52.1.2.3";

    @Test
    void enAwsLaIpVieneEnElHeaderForwardedDeApiGateway() {
        String forwarded = "by=" + API_GATEWAY_IP
                + ";for=190.100.20.30;host=abc.execute-api.us-east-1.amazonaws.com;proto=https";

        assertThat(ClientIp.resolve(null, forwarded, API_GATEWAY_IP)).isEqualTo("190.100.20.30");
        assertThat(ClientIp.chain(null, forwarded, API_GATEWAY_IP)).isEqualTo("190.100.20.30, 52.1.2.3");
    }

    @Test
    void seUsaElUltimoForParaQueElUsuarioNoPuedaInventarSuIp() {
        // El usuario mandó "X-Forwarded-For: 1.1.1.1" y API Gateway lo dejó como primer elemento.
        String forwarded = "for=1.1.1.1, by=" + API_GATEWAY_IP
                + ";for=190.100.20.30;host=abc.execute-api.us-east-1.amazonaws.com;proto=https";

        assertThat(ClientIp.resolve(null, forwarded, API_GATEWAY_IP)).isEqualTo("190.100.20.30");
    }

    @Test
    void entiendeIpv6ConComillasYPuerto() {
        assertThat(ClientIp.lastForwardedFor("for=\"[2001:db8:cafe::17]:4711\"")).isEqualTo("2001:db8:cafe::17");
        assertThat(ClientIp.lastForwardedFor("for=190.100.20.30:51234")).isEqualTo("190.100.20.30");
        assertThat(ClientIp.lastForwardedFor("for=unknown")).isNull();
    }

    @Test
    void conOtroProxySeUsaElPrimeroDeXForwardedFor() {
        assertThat(ClientIp.resolve("200.1.2.3, 10.0.0.5", null, "10.0.0.5")).isEqualTo("200.1.2.3");
    }

    @Test
    void sinProxySeUsaLaIpQueSeConectoAlBff() {
        assertThat(ClientIp.resolve(null, null, "172.18.0.1")).isEqualTo("172.18.0.1");
        assertThat(ClientIp.chain(null, null, "172.18.0.1")).isEqualTo("172.18.0.1");
    }
}
