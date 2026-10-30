# Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/). A PostgreSQL database is used to store backend data and needs to be available
when starting the backend.

## Tor Descriptors

Descriptors which are processed by TorMap are either consensus or server descriptors. A consensus descriptor contains
network information which has been collected by Tor's directory authorities. A server descriptor contains detailed
information about an individual relay that it published itself.

Processing of descriptors does not necessarily happen in a chronological order, but one month of descriptors is always
processed together. Different descriptor types can be handled in parallel. Frontend features like family grouping or
relay details will only be available, if the corresponding relay server descriptors have been processed.

### Consensus weights

Each relay location stores the relay's consensus weight (`w Bandwidth=` of its consensus entry) and the part of it that
clients use for picking the relay as entry guard (`guard_weight`, derived from the consensus' `bandwidth-weights`).
The values come from the first consensus of the day in which the relay was seen.

The migration that introduced them (`V4__Add_consensus_weights.sql`) marks all consensus files as unprocessed once.
After upgrading, the backend therefore reads every consensus file again and only fills in the missing weights. This takes
about as long as the initial import and requires the archive files to still be available in the download directory
(otherwise they are downloaded again). The API keeps serving all data while this runs, relays just have no weight yet.

### Country usage

The estimated number of relay users per country and day is imported from the Tor Metrics CSV
[`userstats-relay-country.csv`](https://metrics.torproject.org/stats.html) (CC0). The first import downloads the complete
history (~40 MB, from 2011-03), afterwards every 12 hours only the latest days with some overlap, because Tor Metrics
revises recent estimates. The source can be changed with the `usage-statistics.*` properties. The estimates are based
on the directory requests that relays report. Early days are based on very few relays (`frac`), so their numbers are
less reliable.

### TorMap Lite

`/lite/` serves the map for browsers without JavaScript, e.g. Tor Browser with the "Safest" security level. The backend
renders the HTML and the map image (`/lite/map.png`, country borders and relay markers drawn with Java2D, no map tiles
and no fonts needed). The page is one `GET` form: the day, zoom and move buttons, the filters and clicks on the map
submit it, so every view is a link. It links to the JavaScript frontend at `app.lite.full-version-url` (default
`https://tormap.org`). The reverse proxy must forward `/lite` and `/static/` to the backend as well.

## Requirements

- At least 50 GB of free disk space (for downloaded archives)
- [Docker](https://docs.docker.com/get-docker/)
- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) (`JAVA_HOME` should point to a version >= 21)

## Run Development Server

1. Go into `backend` directory
2. Start PostgreSQL database: `docker compose up -d database` (the development override publishes it on
   `127.0.0.1:5432`)
3. Run `./gradlew bootRun` or on Windows `gradlew.bat bootRun`. It activates the `dev` Spring profile unless
   `SPRING_PROFILES_ACTIVE` is set.
4. Backend should be available at http://localhost:8080

Run `./gradlew test`, to only build & tests. Run `./gradlew test jacocoTestReport` for a coverage report in `build/reports/jacoco/test/html/index.html`.

In a freshly cloned repository the backend will start to download an archive `>33 GB` in size
from [TorProject](https://metrics.torproject.org/collector/archive/). The total time for the whole archive will depend
on your bandwidth and hardware, but can be expected to take at least several hours.

### Swagger UI

When started with `./gradlew bootRun` in the `dev` profile, an interactive Swagger UI is available under
http://localhost:8080/swagger and the specification can also be viewed in raw JSON under http://localhost:8080/openapi.
Without the `dev` profile, Swagger UI and API docs are disabled. springdoc is a `developmentOnly` dependency, so the boot
jar and the Docker image do not contain it and never serve Swagger, not even in the `dev` profile.

### Admin access

A selection of actuator endpoints are **enabled** by default at the management layer and exposed via HTTP Basic Auth
only. Ensure your deployment exposes only the actuator endpoints you need (Spring property
`management.endpoints.web.exposure.include`).

To enable admin access to `/actuator/**` endpoints. The admin username is configured via Spring
properties:

- `spring.security.user.name` (default: `admin`)

The admin password is **env-only** and must be provided via the `TORMAP_ADMIN_PASSWORD` environment variable
(plaintext; it is BCrypt-hashed in memory at startup).

If it is not set, a warning is logged and actuator endpoints remain unavailable.

`POST`/`PUT`/`PATCH`/`DELETE` requests to actuator endpoints are CSRF protected (double submit cookie, no session):
send the same value as cookie `XSRF-TOKEN` and header `X-XSRF-TOKEN`, e.g.
`curl -u admin -X POST -H 'X-XSRF-TOKEN: x' --cookie 'XSRF-TOKEN=x' ...`. `GET` requests are unaffected.
`GET` endpoints can also be opened in a browser (e.g. `https://api.tormap.org/actuator`), which prompts for the
admin credentials.

Recommended (production):

- Provide a generated `TORMAP_ADMIN_PASSWORD` of at least 16 characters via your secret manager (Kubernetes Secret,
  Docker secret, env etc.). Shorter passwords log a warning at startup.
- Only `/actuator/**` uses HTTP Basic. Restrict it at your reverse proxy (IP allowlist and/or rate limit) if it does not
  need to be reachable from the internet.
- Do not put passwords into `application.yml`.

## Config

- `backend/src/main/resources/application.yml` - main backend config, its defaults are the production settings
- `backend/src/main/resources/application-dev.yml` - local development: Swagger UI (with `bootRun`), CORS for
  `http://localhost:3000`, no cache headers, database password fallback `postgres`. Never activate it in production.
- `backend/src/main/resources/logback-spring.xml` - logging options
- `backend/build.gradle.kts` - dependencies are managed with Gradle
- `docker-compose.yml` - example docker compose file for production deployment
- `docker-compose.override.yml` - overrides `docker-compose.yml` for local development (`dev` profile, database on
  `127.0.0.1:5432`)
- `.env.example` can be copied to `.env` - control docker compose environment variables for local development

### Production environment variables

It is recommended to set following production environment variables via a secret manager:

- `TORMAP_DATABASE_PASSWORD` - The database password (required, there is no default outside the `dev` profile)
- `TORMAP_ADMIN_PASSWORD` - Optionally set admin password for accessing actuator endpoints
- `NEW_RELIC_INGEST_KEY` - Export metrics to New Relic (EU region) via OTLP, see `management.otlp.metrics.export` in `application.yml`

### IP lookups

TorMap uses DB files in [MaxMind DB file format](https://maxmind.github.io/MaxMind-DB/) (
.mmdb) to map IPv4 addresses of Tor relays to geolocations and autonomous systems. The mapping is applied when
descriptors are being processed and missing autonomous systems info is also updated regularly.

IP ranges and their geographic location changes over time. We only use the current IP to location data although the
location of some relays in the past might have been different. If desired, the `.mmdb` DB files can be updated every few
months, to keep the IP ranges up to date.

The zip archives in `backend/ip-lookup/` are the source. The Gradle task `unpackIpLookupDatabases` extracts them into
`build/generated/ip-lookup-resources/ip-lookup/` (as `location.mmdb` and `autonomous-system.mmdb`) whenever resources
are built, so they end up in the JAR and the Docker image.

Replacing existing DB files:

1. Download latest MMDB file from https://db-ip.com/db/download/ip-to-city-lite
2. Replace zip archive `backend/ip-lookup/dbip-city-lite-<DATE>.mmdb.zip`
3. Create account and download latest GeoLite2 ASN MMDB file from https://www.maxmind.com/
4. Replace zip archive `backend/ip-lookup/GeoLite2-ASN_<DATE>.zip`

Keep the `dbip-city-lite` and `GeoLite2-ASN` parts of the file names: the build uses them to rename the databases.

## Deploy

First make sure you have installed all requirements for development.

Build fat JAR:

1. Go to `backend` directory where file `gradlew` is located
2. Run command: `./gradlew bootJar`
3. A fat jar containing all packages should now be located in `backend/build/libs/`.
4. Run command `java -jar <backend jar file>` with `SPRING_DATASOURCE_PASSWORD` set (add `--spring.profiles.active=dev`
   for local development)
5. Backend should be available at http://localhost:8080

Build docker image:

1. Go to `backend` directory where file `gradlew` is located
2. Make sure [docker](https://docs.docker.com/get-docker/) is installed and the docker daemon is running
3. Run command: `./gradlew bootBuildImage`
4. Run new image in a container with: `docker run -p 127.0.0.1:8080:8080 tormap/backend`. Set
   `SPRING_PROFILES_ACTIVE=dev` for local development.
5. Backend should be available at http://localhost:8080

Prebuild docker images are available at https://hub.docker.com/r/tormap/backend. An example docker compose file for
production deployment is available at `docker-compose.yml`. To execute it without development overrides, run
`docker compose -f docker-compose.yml up -d`. It does not publish the database port and binds the backend to
`127.0.0.1:8080`, so a reverse proxy on the same host is required (see below). No Spring profile is needed in
production. A previously set `SPRING_PROFILES_ACTIVE=prod` has no effect and can be removed.

#### Deployment notes

The container runs as non-root user `1000:1000`. The descriptor data is stored in the volume at `/tormap-data`; logs go
to `/tmp/logs` (set `LOG_DIR` to change).

- **Bind mount or volume created by an older (root) version:** run once `chown -R 1000:1000 <host dir or volume path>`.

#### Reverse proxy

The backend serves plain HTTP. Run it behind a TLS-terminating reverse proxy (required: admin credentials are sent with
HTTP Basic) and do not publish port 8080 on a public interface. Docker publishes ports past host firewalls such as ufw,
so always bind to `127.0.0.1` as in `docker-compose.yml`. Besides TLS and rate limits, limit the request body size
there as well, e.g. `client_max_body_size 1m;` for nginx. The backend itself answers `413` for request bodies above
`app.http.request.max-body-bytes` (default 1 MiB). The largest legitimate request, a list of 50 000 relay IDs, is about
0.5 MiB.

`X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-Host` are only applied for connections from trusted proxies
(`server.tomcat.remoteip.internal-proxies`, default: loopback and private networks such as Docker networks). If your
proxy or CDN connects from another address, add it there, e.g. via `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`.

### Hardware / VM Requirements

- 50 GB of free disk space (for downloaded archives)
- 500 MB of RAM (typical actual backend usage is ~300 MB). It is recommended to set a JVM max heap size of 500 MB or
  more (e.g., `-Xmx500m`)
- Additional resources if PostgreSQL is deployed on the same machine

### CORS and caching

#### CORS

The backend enables CORS so specific frontend browser origins can call the public API. Configuration:

- Default: `app.security.cors.allowed-origins: https://tormap.org,https://www.tormap.org,https://tormap-*.web.app`
- Dev: `application-dev.yml` overrides to `http://localhost:3000`

#### HTTP caching

Public `GET` endpoints under `/relay/**` and `/lite/**` can return `Cache-Control` headers to allow browser and
CDN/proxy caching.

- Default: `app.http.cache.public.max-age-seconds: 300`, so `/relay/**` and `/lite/**` responses include
  `Cache-Control: public, max-age=300`.
- Dev: `application-dev.yml` sets it to `0`, which disables the caching headers. Responses are then typically not
  cached unless a controller explicitly sets caching headers.
