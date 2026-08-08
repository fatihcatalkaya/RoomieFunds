# roomiefunds

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Local infrastructure

```shell script
docker compose up -d
```

This starts PostgreSQL on port 5432 and Keycloak on port 9090. Keycloak imports the `roomiefunds` realm from
`keycloak/import/roomiefunds-realm.json` on every fresh start, so the defaults in `application.properties` work
without further setup.

| What                     | Value                                                   |
|--------------------------|---------------------------------------------------------|
| Admin console            | <http://localhost:9090> — `admin` / `admin`             |
| Realm                    | `roomiefunds`                                           |
| Test user                | `user` / `user` (member of group `roomiefunds-admin`)   |
| Backend client           | `roomiefunds`, secret `roomiefunds-dev-secret`          |
| Sync service account     | `roomiefunds-svc`, secret `roomiefunds-svc-dev-secret`  |
| Frontend client (public) | `roomiefunds-frontend`, redirects to `localhost:5173`   |

The realm also contains the realm role `roomiefunds-admin` (granted through the group of the same name, and required
by every REST endpoint), the group `floor-members`, and the user attributes `room` and `isCurrentTenant` used by the
Keycloak sync service.

### OIDC configuration for the frontend

The SPA no longer bakes its OIDC settings into the bundle. It reads them at startup from
`GET /api/config/oidc`, which the backend serves unauthenticated from these environment variables:

| Environment variable       | Default (docker-compose)                   | Meaning                                  |
|----------------------------|--------------------------------------------|------------------------------------------|
| `OIDC_FRONTEND_ISSUER_URI` | `http://localhost:9090/realms/roomiefunds` | Issuer the browser authenticates against |
| `OIDC_FRONTEND_CLIENT_ID`  | `roomiefunds-frontend`                     | Public OIDC client of the SPA            |

These are separate from `quarkus.oidc.auth-server-url`, which is how the *backend* reaches Keycloak — in a deployment
the backend may use an internal hostname the browser cannot resolve. Only values that are already public in the
browser belong on this endpoint; never add a client secret to it.

Keycloak keeps no data volume, so `docker compose down` discards any changes made in the admin console. To write them
back into the realm file, stop the container first — the export cannot run while the server holds the lock on its
embedded database:

```shell script
docker compose stop keycloak
docker commit roomiefunds-keycloak-1 kc-export-tmp
docker run --rm -v "$PWD/keycloak/import":/export kc-export-tmp \
  export --dir /export --users realm_file --realm roomiefunds
docker rmi kc-export-tmp
docker compose start keycloak
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/roomiefunds-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and
  Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on
  it.
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes
  with Swagger UI
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus
  REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- OpenID Connect ([guide](https://quarkus.io/guides/security-openid-connect)): Verify Bearer access tokens and
  authenticate users with Authorization Code Flow
- Agroal - Database connection pool ([guide](https://quarkus.io/guides/datasource)): JDBC Datasources and connection
  pooling
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
