# TorMap

This project visualizes current and past Tor relays on a world map. The backend regularly downloads descriptors
from [TorProject Archive](https://metrics.torproject.org/collector/archive/) and saves a processed version in a local
database. Currently, the required archive part makes up 33 GB for the available consensus descriptor years (2007 - 2021)
. Processing of a descriptor type only starts after all descriptors of the same type have been downloaded and saved to
disk. Processed data can instantly be fetched by the frontend to be displayed on the world map.

![UI screenshot](docs/UI-screenshot.jpg)

## Development

### Requirements

Make sure you have at least 100 GB of free disk space, since the downloaded archive and local DB will take up a lot of
space.

On most `Unix` systems you can use the installation script `./install`. It will try to use your package manager to
install missing requirements. Depending on your shell you run the script with `./install` or `bash ./install`.

If you use Windows or the `./install` script failed, please install these manually:

- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) (11 >= Java version)
- [NodeJS](https://nodejs.org/en/)  (12.0.0 >= version < 17.0.0)
- [yarn](https://yarnpkg.com/en/docs/install) (3.0.0 >= version)

Troubleshooting:

- Make sure `JAVA_HOME` points to a Java JDK version >= 11.

### Run development servers

Make sure you have installed all requirements. The `./run` script will start the backend and frontend servers in
separate terminals/tabs. If you have tmux installed, a new tmux session named `tormap` will be started. Once you kill
the terminal, the server is shutdown. The backend console will stay at an executing percentage below `100%`. This is
normal behaviour with Gradle & Spring Boot.

- Linux: Type `./run` or `bash ./run`
- Windows: Type `run.bat`

The `backend` will be available at http://localhost:8080/ and `frontend` at http://localhost:3000/. If the script fails,
or you prefer to run the servers manually:

1. Go into `backend` directory and run commands
    - `./gradlew` or on Windows `gradlew.bat`
    - `./gradlew bootRun` or on Windows `gradlew.bat bootRun`
2. Go into `frontend` directory and run commands
    - `yarn`
    - `yarn start`

In a fresh project without any preprocessed DB or pre-downloaded archive the backend will start to download an
archive `>
33 GB` in size. Once the first `3 GB` of consensus descriptors have been downloaded, they will start processing.
Once `30 GB` of server descriptors have been downloaded, it will take `1-2 days` to complete processing. Any missing
descriptors released by the `TorProject` will twice a day be automatically downloaded and processed.

Processing of descriptors does not necessarily happen in a chronological order, but one month of descriptors is always
processed together. Different descriptor types are handled in parallel. While the backend is processing descriptors, the
frontend will always be able to display finished data. Frontend features like family grouping or relay details will only
be available, if the corresponding relay server descriptors have been processed.

### Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/).

#### CLI commands

Make sure you are in the `backend` directory. On `Windows` use `gradlew.bat`
instead of `./gradlew` for all following commands.

- `./gradlew bootRun`: creates build, runs it and listens on http://localhost:8080/
- `./gradlew build`: creates build, runs tests
- `./gradlew bootJar`: creates build with a fat JAR which contains all dependencies and resources in `build/libs/`
- `./gradlew bootBuildImage`: creates build and a docker image (make sure local docker daemon is running)
- `./gradlew dokkaHtml`: generate code documentation in HTML format in `build/dokka/`

#### Config

The main `backend` config is located at `backend/srv/main/resorces/application.yml`. Logging options can be configured
with
`backend/srv/main/resorces/logback-spring.xml`. Dependencies are managed with `Gradle` and located
at `backend/build.gradle.kts`.

#### OpenAPI specification

An interactive Swagger UI is available under http://localhost:8080 and the specification can also be viewed in raw JSON
under http://localhost:8080/openapi.

#### Database

TorMap uses an embedded H2 database which saves the whole state in a single DB file located
at `backend/resources/database/tormap.mv.db`.

To manually connect to the DB you either can add the datasource in your IDE or open http://localhost:8080/h2 while the
backend is running. Make sure to configure the connection the same way your `application.yml` is set. In an IDE it might
be necessary to configure the datasource URL with an absolute path to ensure the correct working directory is used.

Preprocessed DBs can be downloaded to speed up project deployment. Please make sure the major version (2.x.x) of the
provided download matches the TorMap backend version you want to deploy (check version in `backend/build.gradle.kts`).:

DB download folder:
https://mega.nz/folder/905XAC4Z#wwpWQm7w_R7tdAJHjMHkNg

#### IP lookups

TorMap uses DB files in [MaxMind DB file format](https://maxmind.github.io/MaxMind-DB/) (
.mmdb) to map IPv4 addresses of Tor relays to geo locations and autonomous systems. The mapping is applied when
descriptors are being processed and missing autonomous systems info is also updated regularly.

IP ranges and their geographic location changes over time. We only use the current IP to location data although the
location of some relays in the past might have been different. It is advised to replace the `.mmdb` DB files every few
months, to keep the IP ranges up to date.

Replacing existing DB files:

1. Download latest MMDB file from https://db-ip.com/db/download/ip-to-country-lite
2. Replace `backend/ip-lookup/location/dbip/dbip-city-lite.mmdb`
3. Create account and download latest GeoLite2 ASN MMDB file from https://www.maxmind.com/
4. Replace `backend/ip-lookup/autonomous-system/maxmind/GeoLite2-ASN.mmdb`

#### Admin access

When the backend is started, an admin password is displayed in the console. After the first startup it is saved
in `backend/resources/admin-password.txt` for future runs. While the backend is running you can login
with `username=admin` and the password at `http://localhost:8080/login`.

This grants you access to:

- Spring actuator endpoints http://localhost:8080/actuator
- H2 DB web console http://localhost:8080/h2

### Frontend

The frontend uses a [ReactJS](https://reactjs.org/) web app together with [TypeScript](https://www.typescriptlang.org/)
and [Material-UI](https://mui.com/).

#### CLI commands

Make sure you are in the `frontend` directory. You can learn more about the following commands in
the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

- `yarn`: installs required frontend packages
- `yarn start`: creates build, runs it and listens by default on http://localhost:3000 (page reloads if you save
  frontend changes)
- `yarn build`: creates production ready build in `build` folder

#### Config

The main `frontend` config is located at `frontend/srv/util/config.ts`. Further environment options like enable/disable
Browser autostart and default port can be configured in `frontend/.env`. Dependencies are managed with `yarn` and
located in `frontend/package.json`. Compiler options for `TypeScript` are located at `frontend/tsconfig.json`.

## Build and run project

First make sure you have installed all requirements for development.

### Backend

Build fat JAR:

1. Go to `backend` directory where file `gradlew` is located
2. Run command: `./gradlew && ./gradlew bootJar`
3. A fat jar containing all packages should now be located in `backend/build/libs/`.
4. Create a folder containing a copy of the generated jar and the `backend/resources` folder.
5. Go into the directory where the `.jar` file is located
6. Run command `java -jar <backend jar file>`
7. Backend should be available at http://localhost:8080

Build docker image:

1. Go to `backend` directory where file `gradlew` is located
2. Make sure [docker](https://docs.docker.com/get-docker/) is installed and the docker daemon is running
3. Run command: `./gradlew && ./gradlew bootBuildImage`
4. A new image named juliushenke/tormap should be available in your local docker registry
5. Run image in new container with command: `docker run -p 8080:8080 juliushenke/tormap` (optionally add the
   flag `-v ~/resources:/workspace/resources/` to mount a preprocessed resources like the DB from your host file system
   into the container)
6. Backend should be available at http://localhost:8080

### Frontend

1. Go to `frontend` directory where file `package.json` is located
2. Run command: `yarn build`
3. A `frontend/build` folder should be generated containing all necessary frontend files
4. Go into the directory where `index.html` is located
5. Install [serve](https://www.npmjs.com/package/serve) and run command: `serve -l 3000`
6. Frontend should be available at http://localhost:3000
