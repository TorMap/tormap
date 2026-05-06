# Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/). A PostgresSQL database is used to store backend data and needs to be available
when starting the backend.

## Tor Descriptors

Descriptors which are processed by TorMap are either consensus or server descriptors. A consensus descriptor contains
network information which has been collected by Tor's directory authorities. A server descriptor contains detailed
information about an individual relay that it published itself.

Processing of descriptors does not necessarily happen in a chronological order, but one month of descriptors is always
processed together. Different descriptor types can be handled in parallel. Frontend features like family grouping or
relay details will only be available, if the corresponding relay server descriptors have been processed.

## Requirements

- At least 50 GB of free disk space (for downloaded archives)
- [Docker](https://docs.docker.com/get-docker/)
- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) (`JAVA_HOME` should point to a version >= 11)

## Run Development Server

1. Go into `backend` directory
2. Start PostgresSQL database: `docker compose up -d database`
3. Run `./gradlew bootRun` or on Windows `gradlew.bat bootRun`
4. Backend should be available at http://localhost:8080

Run `./gradlew test`, to only build & tests.

In a freshly cloned repository the backend will start to download an archive `>33 GB` in size
from [TorProject](https://metrics.torproject.org/collector/archive/). The total time for the whole archive will depend
on your bandwidth and hardware, but can be expected to take at least several hours.

### Swagger UI

For development purposes, an interactive Swagger UI is available under http://localhost:8080/swagger and the
specification can also be viewed in raw JSON
under http://localhost:8080/openapi. In the production environment, Swagger UI and API docs are disabled by default.

### Admin access

A selection of actuator endpoints are **enabled** by default at the management layer and exposed via HTTP Basic Auth
only. Ensure your deployment exposes only the actuator endpoints you need (Spring property
`management.endpoints.web.exposure.include`).

To enable admin access to `/actuator/**` endpoints. The admin username is configured via Spring
properties:

- `spring.security.user.name` (default: `admin`)

The admin password is **env-only** and must be provided via one of the following environment variables:

- `TORMAP_ADMIN_PASSWORD` (plaintext)
- `TORMAP_ADMIN_PASSWORD_BCRYPT` (BCrypt hash starting with `$2a$`, `$2b$`, or `$2y$`)

If neither env var is set, a warning is logged and actuator endpoints remain unavailable.

Recommended (production):

- Provide `TORMAP_ADMIN_PASSWORD_BCRYPT` via your secret manager (Kubernetes Secret, Docker secret, env etc.).
- Do not put passwords/hashes into `application.yml`.

Generate a BCrypt hash:

```bash
# Requires Docker; will prompt for password and print the hash to stdout
docker run --rm -it httpd:2.4-alpine htpasswd -nBC 12 admin
```

## Config

- `backend/src/main/resources/application.yml` - main backend config
- `backend/src/main/resources/application-prod.yml` - production specific overwrites
- `backend/src/main/resources/logback-spring.xml` - logging options
- `backend/build.gradle.kts` - dependencies are managed with Gradle
- `docker-compose.yml` - example docker compose file for production deployment
- `docker-compose.override.yml` - overrides `docker-compose.yml` for local development
- `.env.example` can be copied to `.env` - control docker compose environment variables for local development

### Production environment variables

It is recommended to set following production environment variables via a secret manager:

- `TORMAP_DATABASE_PASSWORD` - Overrides the database password
- `TORMAP_ADMIN_PASSWORD` - Optionally set admin password for accessing actuator endpoints
- `NEW_RELIC_INGEST_KEY` - Collect metrics to https://newrelic.com

### IP lookups

TorMap uses DB files in [MaxMind DB file format](https://maxmind.github.io/MaxMind-DB/) (
.mmdb) to map IPv4 addresses of Tor relays to geolocations and autonomous systems. The mapping is applied when
descriptors are being processed and missing autonomous systems info is also updated regularly.

IP ranges and their geographic location changes over time. We only use the current IP to location data although the
location of some relays in the past might have been different. If desired, the `.mmdb` DB files can be updated every few
months, to keep the IP ranges up to date.

Replacing existing DB files:

1. Download latest MMDB file from https://db-ip.com/db/download/ip-to-city-lite
2. Replace zip archive `backend/ip-lookup/location/dbip-city-lite-<DATE>.zip`
3. Create account and download latest GeoLite2 ASN MMDB file from https://www.maxmind.com/
4. Replace zip archive `backend/ip-lookup/autonomous-system/GeoLite2-ASN-<DATE>.zip`

## Deploy

First make sure you have installed all requirements for development.

Build fat JAR:

1. Go to `backend` directory where file `gradlew` is located
2. Run command: `./gradlew bootJar`
3. A fat jar containing all packages should now be located in `backend/build/libs/`.
4. Run command `java -jar <backend jar file>`
5. Backend should be available at http://localhost:8080

Build docker image:

1. Go to `backend` directory where file `gradlew` is located
2. Make sure [docker](https://docs.docker.com/get-docker/) is installed and the docker daemon is running
3. Run command: `./gradlew bootBuildImage`
4. Run new image in a container with: `docker run -p 8080:8080 tormap/backend`
5. Backend should be available at http://localhost:8080

Prebuild docker images are available at https://hub.docker.com/r/tormap/backend. An example docker compose file for
production deployment is available at `docker-compose.yml`. To execute it without development overrides, run
`docker compose -f docker-compose.yml up -d`.

### Hardware / VM Requirements

- 50 GB of free disk space (for downloaded archives)
- 500 MB of RAM (typical actual backend usage is ~300 MB). It is recommended to set a JVM max heap size of 500 MB or
  more (e.g., `-Xmx500m`)
- Additional resources if PostgreSQL is deployed on the same machine

### CORS and caching

#### CORS

The backend enables CORS so specific frontend browser origins can call the public API. Configuration:

- Dev (default): `app.security.cors.allowed-origins: http://localhost:3000`
- Prod: `application-prod.yml` overrides to `https://tormap.org,https://www.tormap.org`

#### HTTP caching

Public `GET` endpoints under `/relay/**` can return `Cache-Control` headers to allow browser and CDN/proxy caching.

- Dev/default: caching headers are **disabled** (`app.http.cache.public.max-age-seconds` defaults to `0`).
  Responses are typically not cached unless a controller explicitly sets caching headers.
- Prod: `application-prod.yml` sets `app.http.cache.public.max-age-seconds: 300`, so `/relay/**` responses include
  `Cache-Control: public, max-age=300`.
