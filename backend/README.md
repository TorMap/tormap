# Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/). A PostgresSQL database is used to store backend data and needs to be available
when starting the backend.

## Tor Descriptors

Descriptors which are processed by TorMap are either consensus or server descriptors. A consensus descriptor contains
network information which has been collected by Tor's directory authorities. A server descriptor contains detailed
information about an individual relay that it published itself.

Processing of descriptors does not necessarily happen in a chronological order, but one month of descriptors is always
processed together. Different descriptor types can be handled in parallel. Frontend features like family grouping or relay details will only
be available, if the corresponding relay server descriptors have been processed.

## Requirements

- At least 50 GB of free disk space (for downloaded archives)
- [Docker](https://docs.docker.com/get-docker/)
- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) (`JAVA_HOME` should point to a version >= 11)

## Run Development Server

1. Go into `backend` directory
2. Start PostgresSQL database: `docker-compose -f production.yml up -d database`
3. Run `./gradlew bootRun` or on Windows `gradlew.bat bootRun`
4. Backend should be available at http://localhost:8080

Run `./gradlew test`, to only build & tests.

In a freshly cloned repository the backend will start to download an archive `>33 GB` in size
from [TorProject](https://metrics.torproject.org/collector/archive/). The total time for the whole archive will depend
on your bandwidth and hardware, but can be expected to take at least several hours.

### Swagger UI

An interactive Swagger UI is available under http://localhost:8080 and the specification can also be viewed in raw JSON
under http://localhost:8080/openapi.

### Admin access

When the backend is started, an admin password is displayed in the console. After the first startup it is saved
in `backend/resources/admin-password.txt` for future runs. While the backend is running you can login
with `username=admin` and the password at `http://localhost:8080/login`.

This grants you access to the Spring actuator endpoints: http://localhost:8080/actuator

## Config

The main `backend` config is located at `backend/srv/main/resorces/application.yml`. Logging options can be configured
with
`backend/srv/main/resorces/logback-spring.xml`. Dependencies are managed with `Gradle` and located
at `backend/build.gradle.kts`.
When deploying, the environment variable `NEW_RELIC_INGEST_KEY` can be set to collect metrics to https://newrelic.com.

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

Prebuild docker images are available at https://hub.docker.com/r/tormap/backend.

### Hardware / VM Requirements

- 50 GB of free disk space (for downloaded archives)
- 2 GB of RAM (typical backend usage is below 1 GB)
- It is recommended to set a JVM max heap size of 1.5 GB or more (e.g. `-Xmx1500m`)
- Additional resources if PostgreSQL is deployed on the same machine





