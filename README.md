# TorMap

This project visualizes current and past Tor relays on a world map. The backend regularly downloads descriptors
from [TorProject Archive](https://metrics.torproject.org/collector/archive/) and saves a processed version in a local
database. Currently, the required archive part makes up 33 GB for the available years (2007 - 2021). Processing of a
descriptor type only starts after all descriptors of the same type have been downloaded and saved to disk. Processed
data can instantly be fetched by the frontend to be displayed on the world map.

![UI screenshot](docs/UI-screenshot.jpg)

## Development

### Requirements

Make sure you have at least 100 GB of free disk space, since the downloaded archive and local DB will take up a lot of
space.

On most `Unix` systems you can use the installation script `./install`. It will try to use your package manager to
install missing requirements. Depending on your shell you run the script with `./install` or `bash ./install`.

If you use Windows or the `./install` script failed, please install these manually:

- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) >= Java version 11
- [NodeJS](https://nodejs.org/en/)
- [yarn](https://yarnpkg.com/en/docs/install)
- [serve](https://www.npmjs.com/package/serve)

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
be available, if the corresponding server descriptors have also been processed.

### Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/).

#### CLI commands

Make sure you are in the `backend` directory. On `Windows` use `gradlew.bat`
instead of `./gradlew` for all following commands.

- `./gradlew`: installs required backend packages
- `./gradlew build`: creates build and runs Flyway database migrations
- `./gradlew bootRun`: creates build, runs it and listens on http://localhost:8080/
- `./gradlew bootJar`: creates build with a fat JAR which contains all dependencies and resources in `build/libs/`
- `./gradlew flywayMigrate`: Migrates the database
- `./gradlew flywayClean`: Drops all objects in the configured schemas
- `./gradlew flywayInfo`: Prints the details and status information about all the migrations
- `./gradlew flywayValidate`: Validates the applied migrations against the ones available on the classpath
- `./gradlew flywayBaseline`: Baselines an existing database, excluding all migrations up to and including
  baselineVersion
- `./gradlew flywayRepair`: Repairs the schema history table
- `./gradlew dokkaHtml`: generate code documentation in HTML format in `build/dokka/`

#### Config

The main `backend` config is located at `backend/srv/main/resorces/application.properties`. Logging options can be
configured with
`backend/srv/main/resorces/logback-spring.xml`. Dependencies are managed with `Gradle` and located
at `backend/build.gradle.kts`.

#### OpenAPI specification

An interactive Swagger UI is available under http://localhost:8080/documentation and the specification can also be
viewed in raw JSON under http://localhost:8080/documentation/json.

#### Database

TorMap uses an embedded H2 database which saves the whole state in a single DB file located
at `backend/database/tormap.mv.db`. If you want to use a DB already containing a few processed months, you can download
one from https://lightningpuzzle.com/tormap/ and put it here `backend/database/tormap.mv.db`.

To manually connect to the DB you either can add the datasource in your IDE or open http://localhost:8080/h2 while the
backend is running. Make sure to configure the connection the same way your `application.properties` are set. In an IDE
it might be necessary to configure the datasource URL with an absolute path to ensure the correct working directory is
used.

#### IP to Autonomous Systems

TorMap uses previously imported Autonomous System (AS) data from `IP2Location` to get the AS of a Tor node's IP address.
The mapping is done after a server descriptors file was processed and additionally every 12 hours (configurable). If you
are not starting with a preprocessed TorMap DB you will need to import a CSV file containing autonomous systems into the
local H2 database. It is advised to reimport a new CSV file every few months, to keep the IP ranges up to date.

1. Create a free account at https://lite.ip2location.com/sign-up
2. Download latest IPv4 CSV file from https://lite.ip2location.com/database-asn or use the CSV file located
   at `backend/database/ip2location/IP2LOCATION-LITE-ASN.CSV`
3. Run command `./gradlew build` in `backend` directory
4. Connect to a DB query console and run following commands:
    - `TRUNCATE TABLE AUTONOMOUS_SYSTEM;`
    - `INSERT INTO AUTONOMOUS_SYSTEM SELECT * FROM CSVREAD('<absolute_path_to_csv_file>');`

#### IP to geo location

TorMap uses a binary DB file from `IP2Location` to map IPv4 addresses of Tor nodes to geo locations. The mapping is
applied when a consensus descriptor is being processed. It is advised to replace the binary file every few months, to keep
the IP ranges up to date.

1. Create a free account at https://lite.ip2location.com/sign-up
2. Download latest IPv4 BIN file
   from https://lite.ip2location.com/database/db5-ip-country-region-city-latitude-longitude
3. Replace old BIN file with new one in `backend/database/ip2location/IP2LOCATION-LITE-DB5.BIN`

### Frontend

The frontend uses a [ReactJS](https://reactjs.org/) web app together with [TypeScript](https://www.typescriptlang.org/)
and [Material-UI](https://material-ui.com/).

#### CLI commands

Make sure you are in the `frontend` directory. You can learn more about the following commands in
the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

- `yarn`: installs required frontend packages
- `yarn start`: creates build, runs it and listens by default on http://localhost:3000 (page reloads if you save frontend changes)
- `yarn build`: creates production ready build in `build` folder

#### Config

The main `frontend` config is located at `frontend/srv/util/config.ts`. Further environment options like enable/disable
Browser autostart and default port can be configured in `frontend/.env`. Dependencies are managed with `yarn` and located
in `frontend/package.json`. Compiler options for `TypeScript` are located at `frontend/tsconfig.json`.

## Build project

First make sure you have installed all requirements for development.

### Backend

1. Go to `backend` directory where file `gradlew` is located
2. Run command: `./gradlew && ./gradlew bootJar`
3. A fat jar containing all packages should now be located in `backend/build/libs/`.
4. Create a folder containing a copy of the generated jar and the `backend/database` folder.

### Frontend

1. Go to `frontend` directory where file `package.json` is located
2. Run command: `yarn build`
3. A `frontend/build` folder should be generated containing all necessary frontend files

## Host project

Make sure you have installed all requirements for development. To be able to host, you should have a copy of a release
or just successfully created your own project build.

### Releases
- http://timkilb.com/releases/tormap-version-1.1.0.zip

### Preprocessed databases
- http://timkilb.com/databases/tormap-full-DB-2021-09-01-version-1.1.0.zip
- http://timkilb.com/databases/tormap-only-AS-DB-2021-09-01-version-1.1.0.zip

### Start backend

1. Go into the directory where the `.jar` file is located
2. Run command `java -jar <backend jar file>`
3. Backend should be available at http://localhost:8080

### Start frontend

1. Go into the directory where `index.html` is located
2. Run command: `serve -l 3000`
3. Frontend should be available at http://localhost:3000


