# Local Keycloak setup

Phase 3 adds optional OIDC/JWT authorization. Local services still start with
`APP_SECURITY_ENABLED=false`; set it to `true` when you want Gateway and services
to validate Keycloak access tokens.

## Start with Docker Compose

```bash
cp "Solution Items/.env.example" "Solution Items/.env"
$EDITOR "Solution Items/.env"
docker compose --env-file "Solution Items/.env" \
  -f "Solution Items/docker-compose.yml" \
  -f "Solution Items/docker-compose.override.yml" up -d keycloak
```

Then enable authorization in `Solution Items/.env`:

```dotenv
APP_SECURITY_ENABLED=true
APP_SECURITY_ISSUER_URI=http://keycloak:8080/realms/microservices
APP_SECURITY_AUDIENCE=microservices-api
```

The realm import creates:

- realm: `microservices`
- public PKCE client: `microservices-web`
- service-account client: `ordering-service`
- realm roles: `CUSTOMER`, `ADMIN`, `SERVICE_ORDERING`
- internal scope: `internal`

Create local users from the Keycloak admin console, then assign `CUSTOMER` or
`ADMIN` realm roles as needed. The repository does not import user passwords.

## Login transition

`POST /api/v1/identity/login` remains available for compatibility during Phase 3,
but new clients should use Authorization Code + PKCE against Keycloak.
