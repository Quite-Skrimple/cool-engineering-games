# Cool Engineering Games

A CoolMathGames-style arcade of short, engineering-themed browser games, built as a set of cloud-native microservices.

## Services

| Service | Port | Owns | Status |
|---|---|---|---|
| `identity-service` | 8081 | Accounts, roles, account status | Built |
| `game-catalog-service` | 8082 | Creator-submitted games, genres, approval status | Built |
| `engagement-service` | 8083 | Likes/dislikes, library entries, comments | Built |
| `configserver` | 8888 | Centralized `dev` / `prod` configuration | Built |

Each service is its own Maven module with its own 3-layer stack (entity → repository → service → controller) and its own database — no shared tables across services.

## Requirements

- Java 25+
- Maven 4.0.0+ (or any Maven 3.9.x works fine)
- Docker + Docker Compose, for the `prod` profile

## Running locally (`dev` profile — H2, no Docker)

Each service runs independently with an in-memory H2 database. From the repo root:

```bash
mvn -pl identity-service spring-boot:run
mvn -pl game-catalog-service spring-boot:run
mvn -pl engagement-service spring-boot:run
```

Each service also tries to pull config from `configserver` on startup (`http://localhost:8888`); if the config server isn't running, that's fine — the import is optional and each service falls back to its own local `application-dev.properties`. To run the config server too:

```bash
mvn -pl configserver spring-boot:run
```

H2 consoles (dev only): `http://localhost:808{1,2,3}/h2-console`.

## Running with Docker (`prod` profile — Postgres)

The Dockerfiles expect pre-built jars rather than building inside the container, so package first, then bring the stack up:

```bash
mvn package
```

```bash
cd docker
docker compose up --build
```

This builds and starts `configserver`, a dedicated Postgres instance per service (`identity-db`, `catalog-db`, `engagement-db`), and all three business services with `SPRING_PROFILES_ACTIVE=prod`. Services wait for the config server and their own database to report healthy before starting, then fetch their `prod` settings from `configserver` (the same values also exist as a local fallback in each `application-prod.properties`). Data lives in named Docker volumes, so it survives restarts; `docker compose down -v` wipes it.

The compose project is named `cool-engineering-games`, so it won't collide with other compose stacks on your machine. Host ports used: 8081, 8082, 8083, 8888.

## Profiles

- **`dev`** (default): H2 in-memory database per service, H2 console enabled, verbose SQL logging.
- **`prod`**: Postgres per service, H2 console disabled, quiet SQL logging. Selected via the `SPRING_PROFILES_ACTIVE=prod` environment variable (set automatically in `docker-compose.yml`).

## Database migrations

Schemas are managed by [Flyway](https://flywaydb.org), one migration history per service database, in both profiles. Scripts live in each service's `src/main/resources/db/migration/` and run automatically at startup; Hibernate is set to `ddl-auto=validate`, so it checks the schema against the entities but never changes it.

- To change a schema, add a new file, e.g. `V2__add_game_thumbnail.sql`. Never edit an already-applied `V1__...` script.
- The same SQL runs on H2 (`dev`, in PostgreSQL mode) and Postgres (`prod`), so stick to standard SQL.
- Enum columns are stored as `varchar` with a `check` constraint listing the allowed values; add new enum values with a migration that updates the constraint.
- H2 is pinned to 2.3.232 in each service's `pom.xml`: 2.4.240 has a bug that makes check constraints created by Flyway reject valid rows.

## Tests

```bash
mvn test
```

Repository tests (`@DataJpaTest`) run the real Flyway migrations on H2 and then validate them against the entities.

## Postman

Import `postman/cool-engineering-games.postman_collection.json` into Postman (Import → File). It has 38 requests with 65 assertions covering every endpoint of every service, including the error paths (400 / 404 / 409), and a Cleanup folder at the end so it can be re-run. Start the services first (either run mode above), then run the whole collection top to bottom with the Collection Runner. The base URLs are collection variables (`identityUrl`, `catalogUrl`, `engagementUrl`, `configUrl`).

From the command line with [Newman](https://www.npmjs.com/package/newman):

```bash
npx newman run postman/cool-engineering-games.postman_collection.json
```

## Troubleshooting

If `mvn` fails to download a dependency with `PKIX path building failed` / `unable to find valid certification path`, your machine's antivirus (e.g. Avast) is doing local TLS inspection and its certificate isn't trusted by your JDK's truststore (only by Windows' own store, which is why browsers/git still work). Import it into your JDK once:

```bash
keytool -importcert -trustcacerts -noprompt -alias local-tls-proxy -file <exported-root-cert>.cer -keystore "<JAVA_HOME>/lib/security/cacerts" -storepass changeit
```
