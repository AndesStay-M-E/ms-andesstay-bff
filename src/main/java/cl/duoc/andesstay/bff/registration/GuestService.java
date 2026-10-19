package cl.duoc.andesstay.bff.registration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Creates an Entra B2B guest and assigns ONLY the CLIENTE API role. */
@Service
public class GuestService {
    private final RestClient microsoft = RestClient.create();
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String apiAppId;
    private final String redirectUrl;
    private final boolean enabled;

    public GuestService(
            @Value("${andesstay.registration.enabled:false}") boolean enabled,
            @Value("${andesstay.registration.tenant-id}") String tenantId,
            @Value("${andesstay.registration.client-id:}") String clientId,
            @Value("${andesstay.registration.client-secret:}") String clientSecret,
            @Value("${andesstay.registration.api-app-id}") String apiAppId,
            @Value("${andesstay.registration.redirect-url}") String redirectUrl) {
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apiAppId = apiAppId;
        this.redirectUrl = redirectUrl;
    }

    public void inviteClient(String name, String email) {
        if (!enabled || clientId.isBlank() || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El registro de invitados todavía no está configurado");
        }
        if (!redirectUrl.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La URL pública del registro debe utilizar HTTPS");
        }
        try {
            String token = accessToken();
            // Resolve the API's service principal and role ID dynamically;
            // never rely on display names or truncated IDs from screenshots.
            Map<?, ?> principal = microsoft.get()
                    .uri("https://graph.microsoft.com/v1.0/servicePrincipals(appId='" + apiAppId
                            + "')?$select=id,appRoles")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(Map.class);
            if (principal == null || !(principal.get("appRoles") instanceof List<?> roles)) {
                throw new IllegalStateException("La API no tiene roles accesibles");
            }
            String roleId = roles.stream().filter(role -> role instanceof Map<?, ?>)
                    .map(role -> (Map<?, ?>) role)
                    .filter(role -> "CLIENTE".equals(role.get("value")))
                    .filter(role -> Boolean.TRUE.equals(role.get("isEnabled")))
                    .filter(role -> role.get("allowedMemberTypes") instanceof List<?> kinds
                            && kinds.contains("User"))
                    .map(role -> Objects.toString(role.get("id"), ""))
                    .filter(id -> !id.isBlank()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No existe el rol CLIENTE habilitado"));
            String servicePrincipalId = Objects.toString(principal.get("id"), "");
            if (servicePrincipalId.isBlank()) throw new IllegalStateException("API sin principal de servicio");

            Map<?, ?> invitation = microsoft.post()
                    .uri("https://graph.microsoft.com/v1.0/invitations")
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("invitedUserEmailAddress", email,
                            "invitedUserDisplayName", name,
                            "inviteRedirectUrl", redirectUrl,
                            "sendInvitationMessage", true))
                    .retrieve().body(Map.class);
            if (invitation == null || !(invitation.get("invitedUser") instanceof Map<?, ?> guest)) {
                throw new IllegalStateException("Microsoft Graph no devolvió al usuario invitado");
            }
            String guestId = Objects.toString(guest.get("id"), "");
            if (guestId.isBlank()) throw new IllegalStateException("Invitación sin identificador de usuario");

            // In a failure between invitation and assignment, a guest may exist
            // without CLIENTE. Reconcile such accounts administratively.
            microsoft.post()
                    .uri("https://graph.microsoft.com/v1.0/users/" + guestId + "/appRoleAssignments")
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("principalId", guestId,
                            "resourceId", servicePrincipalId,
                            "appRoleId", roleId))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            // Never expose Graph responses, tokens or directory information publicly.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo crear la invitación en Microsoft Entra ID");
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El registro en Entra ID requiere configuración administrativa");
        }
    }

    private String accessToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");
        Map<?, ?> response = microsoft.post()
                .uri("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class);
        if (response == null || !(response.get("access_token") instanceof String accessToken)) {
            throw new IllegalStateException("No se obtuvo un token de Microsoft Graph");
        }
        return accessToken;
    }
}
