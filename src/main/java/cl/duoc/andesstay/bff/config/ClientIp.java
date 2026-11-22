package cl.duoc.andesstay.bff.config;

/**
 * Averigua la IP real del usuario para la auditoría ("desde dónde").
 *
 * Según por dónde llegó la petición al BFF:
 *
 *  1) AWS API Gateway (HTTP API): NO manda X-Forwarded-For, lo transforma en el
 *     header estándar "Forwarded" (RFC 7239), por ejemplo:
 *        Forwarded: by=52.1.2.3;for=190.100.20.30;host=xxx.execute-api.us-east-1.amazonaws.com;proto=https
 *     Se usa el ÚLTIMO "for=" porque es el que agregó API Gateway (los anteriores
 *     los puede inventar el propio usuario).
 *
 *  2) Otro proxy que use X-Forwarded-For ("cliente, proxy1, proxy2"): el primero.
 *
 *  3) Sin proxy (PC local): la IP que se conectó al BFF.
 */
final class ClientIp {

    private ClientIp() {
    }

    static String resolve(String xForwardedFor, String forwarded, String remoteAddr) {
        String fromForwarded = lastForwardedFor(forwarded);
        if (fromForwarded != null) {
            return fromForwarded;
        }

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return remoteAddr;
    }

    /**
     * Valor que el BFF manda a los microservicios en X-Forwarded-For:
     * "ip-del-usuario, ip-que-llamó-al-bff" (o solo una si son iguales).
     */
    static String chain(String xForwardedFor, String forwarded, String remoteAddr) {
        String client = resolve(xForwardedFor, forwarded, remoteAddr);

        if (client == null || client.isBlank()) {
            return remoteAddr;
        }
        if (remoteAddr == null || remoteAddr.isBlank() || remoteAddr.equals(client)) {
            return client;
        }
        return client + ", " + remoteAddr;
    }

    /** Último "for=" del header Forwarded, o null si no viene. */
    static String lastForwardedFor(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }

        String last = null;

        // Elementos separados por coma; cada uno tiene pares clave=valor separados por punto y coma.
        for (String element : forwarded.split(",")) {
            for (String pair : element.split(";")) {
                String[] keyValue = pair.trim().split("=", 2);

                if (keyValue.length == 2 && keyValue[0].trim().equalsIgnoreCase("for")) {
                    String ip = cleanIp(keyValue[1]);
                    if (ip != null) {
                        last = ip;
                    }
                }
            }
        }
        return last;
    }

    /** Quita comillas, corchetes de IPv6 y el puerto: "[2001:db8::1]:4711" -> "2001:db8::1". */
    private static String cleanIp(String value) {
        String ip = value.trim().replace("\"", "");

        if (ip.startsWith("[")) {
            int end = ip.indexOf(']');
            ip = end > 0 ? ip.substring(1, end) : ip.substring(1);
        } else if (ip.indexOf(':') > 0 && ip.indexOf(':') == ip.lastIndexOf(':')) {
            // IPv4 con puerto: 190.100.20.30:51234
            ip = ip.substring(0, ip.indexOf(':'));
        }

        if (ip.isBlank() || ip.equalsIgnoreCase("unknown") || ip.startsWith("_")) {
            return null;
        }
        return ip;
    }
}
