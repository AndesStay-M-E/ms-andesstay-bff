package cl.duoc.andesstay.bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.URI;

/**
 * Si un microservicio interno está apagado, el BFF responde 503 con un
 * mensaje claro en vez de un error 500 genérico.
 */
@RestControllerAdvice
public class BffExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(BffExceptionHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleUnavailable(ResourceAccessException exception) {
        log.error("Microservicio no disponible: {}", exception.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio solicitado no está disponible en este momento. Verifica que el microservicio esté en ejecución."
        );
        problem.setTitle("Servicio no disponible");
        problem.setType(URI.create("https://andesstay.cl/errors/service-unavailable"));
        return problem;
    }
}
