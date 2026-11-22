# ms-andesstay-bff

**Backend for Frontend** de AndesStay (puerto **8080**). Es la **única puerta de entrada** a la API:
el frontend (y API Gateway en AWS) solo conocen al BFF, y el BFF reenvía cada petición al
microservicio que corresponde.

```
Frontend (CSR) ──► API Gateway ──► BFF :8080 ──► reservations :8081
                                            ├──► catalog      :8082
                                            ├──► report       :8084
                                            ├──► audit        :8085
                                            ├──► rabbit-admin :8086
                                            └──► kafka-admin  :8087
```

## Qué hace

- Valida el **JWT de Azure AD** (issuer del tenant y audiencia de la API de AndesStay) y revisa el rol
  del claim `roles` antes de reenviar. Cada microservicio vuelve a validar el token (defensa en profundidad).
- Reenvía método, ruta, query string, cuerpo JSON y el header `Authorization` (`DownstreamClient`).
  La respuesta vuelve tal cual (código HTTP, `Content-Type`, `Location` y cuerpo), así los errores
  400/404/409 llegan con su detalle al frontend.
- Manda la IP del usuario en el header **`X-Forwarded-For`** (`RestClientConfig` + `ClientIp`), para que
  reservas guarde **desde dónde** se hizo cada cambio. En AWS esa IP viene en el header estándar
  `Forwarded` que agrega API Gateway (HTTP API no manda `X-Forwarded-For`); se usa el último `for=`
  porque es el que puso API Gateway y el usuario no lo puede inventar.
- Si un microservicio está apagado responde **503 "Servicio no disponible"**.
- CORS: solo acepta los orígenes de `CORS_ALLOWED_ORIGINS` (localhost:4200, CloudFront o Amplify).

## Rutas y roles

| Ruta | Roles | Microservicio |
|---|---|---|
| `/api/reservations/**` | usuario autenticado (los roles finos los valida reservations) | reservations |
| `/api/catalog/**` | ADMIN, OPERADOR, CLIENTE, AUDITOR | catalog |
| `/api/reports/**` (`summary`, `vouchers`, `kpis`, `top-units`, `activity`) | ADMIN | report |
| `/api/audit/**` | ADMIN, AUDITOR | audit |
| `/api/rabbit/**` | ADMIN | rabbit-admin |
| `/api/kafka/**` | ADMIN | kafka-admin |
| `/api/public/**` | sin token | BFF (estado y registro público) |
| `/actuator/health` | sin token | BFF |

## Variables de entorno

| Variable | Por defecto |
|---|---|
| `PORT` | 8080 |
| `RESERVATIONS_URL` | `http://localhost:8081` |
| `CATALOG_URL` | `http://localhost:8082` |
| `REPORT_URL` | `http://localhost:8084` |
| `AUDIT_URL` | `http://localhost:8085` |
| `RABBIT_ADMIN_URL` | `http://localhost:8086` |
| `KAFKA_ADMIN_URL` | `http://localhost:8087` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` (separar con coma, sin `/` al final) |
| `AZURE_ISSUER_URI`, `AZURE_ALLOWED_AUDIENCES` | tenant y API de AndesStay |
| `REGISTRATION_ENABLED` | `false` |

En Docker (`infra-andesstay-messaging/apps/compose.yml`) las URL usan los nombres de los contenedores
(`http://reservations:8081`, etc.).

## Ejecutar

Con Docker desde `infra-andesstay-messaging` (`docker compose -f compose.local.yml up -d --build`).
Solo este servicio: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

## Pruebas

`./mvnw test`. `BffRoutingSecurityTest` reemplaza el `JwtDecoder` por uno local con tokens de prueba;
las pruebas que levantan el contexto completo consultan el issuer de Azure, así que necesitan internet.
