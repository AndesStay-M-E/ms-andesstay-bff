package cl.duoc.andesstay.bff.registration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Public onboarding for B2B guests; never accepts a role from the caller. */
@RestController
@RequestMapping("/api/public")
public class RegisterController {
    private final GuestService guests;
    private final ConcurrentHashMap<String, Window> requests = new ConcurrentHashMap<>();

    public RegisterController(GuestService guests) {
        this.guests = guests;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest input,
            HttpServletRequest request) {
        // For a classroom demonstration. Add WAF/CAPTCHA and distributed rate limiting
        // before allowing public traffic; never trust arbitrary X-Forwarded-For values.
        String key = request.getRemoteAddr();
        Instant now = Instant.now();
        Window state = requests.compute(key, (ignored, previous) -> {
            if (previous == null || Duration.between(previous.start, now).toMinutes() >= 60) {
                return new Window(now, 1);
            }
            return new Window(previous.start, previous.count + 1);
        });
        if (state.count > 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados registros desde esta dirección");
        }
        guests.inviteClient(input.name().trim(), input.email().trim());
        return ResponseEntity.accepted().body(Map.of("message",
                "Revisa tu correo y acepta la invitación de AndesStay para completar el registro."));
    }

    private record Window(Instant start, int count) {}
}
