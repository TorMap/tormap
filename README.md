# TorMap

This project visualizes current and past Tor relays on a world map. The backend regularly downloads descriptors
from [TorProject Archive](https://metrics.torproject.org/collector/archive/) and saves a processed version in a local
database. Currently the required archive part makes up 33 GB for the available years (2007 - 2021). Processing of a
descriptor type only starts after all descriptors of the same type have been downloaded and saved to disk. Processed
data can instantly be fetched by the frontend to be display on the world map.

## Development

### Requirements

Make sure you have at least 100 GB of free disk space, since the downloaded archive and local DB will take up a lot of
space.

On most `Unix` systems you can use the install script `./install`. It will try to use your package manager to install
missing requirements. Depending on your shell you run the script with `./install` or `bash ./install`.

If you use Windows or the `./install` script failed, please install these manually:

- [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html) >= Java version 11
- [NodeJS](https://nodejs.org/en/)
- [yarn](https://yarnpkg.com/en/docs/install)
- [serve](https://www.npmjs.com/package/serve)

### Run development servers

Make sure you have installed all requirements.

- Linux: Type `./run` or `bash ./run`
- Windows: `run.bat`

If the script fails or you prefer to run the servers manually:

1. Go into `backend` directory and run commands
    - `./gradlew` or on Windows `gradlew.bat`
    - `./gradlew bootRun` or on Windows `gradlew.bat bootRun`
2. Go into `frontend` directory and run commands
    - `yarn`
    - `yarn start`

### Backend

The backend uses a [Spring Boot](https://spring.io/projects/spring-boot) standalone webserver and is written
in [Kotlin](https://kotlinlang.org/).

#### CLI commands

Make sure you are in the `backend` directory. On `Windows` use `gradlew.bat`
instead of `./gradlew` for all following commands.

- `./gradlew`: installs required backend packages
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

#### OpenAPI specification

An interactive Swagger UI is available under http://localhost:8080/documentation and the specification can also be
viewed in raw JSON under http://localhost:8080/documentation/json.

#### Database

TorMap uses an embedded H2 database which saves the whole state in a single DB file located
at `backend/database/tormap.mv.db`. To manually connect to the DB you either can add the datasource in your IDE or
open http://localhost:8080/h2 while the backend is running. Make sure to configure the connection the same way
your `application.properties` are set. In an IDE it might be necessary to configure the datasource URL
with `./backend/...` or an absolute path to ensure the correct working directory is used.

#### IP to autonomous systems

If you are not starting with a preprocessed TorMap DB you will need to import a CSV file containing autonomous systems
into the local H2 database. It is advised to reimport a new CSV file every few months, to keep the IP ranges up to date.

1. Create a free account at https://lite.ip2location.com/sign-up
2. Download latest IPv4 CSV file from https://lite.ip2location.com/database-asn or use the CSV file located
   at `backend/database/ip2location/IP2LOCATION-LITE-ASN.CSV`
3. Run following commands on the TorMap DB:
    - `TRUNCATE TABLE AUTONOMOUS_SYSTEM;`
    - `INSERT INTO AUTONOMOUS_SYSTEM SELECT * FROM CSVREAD('<absolute_path_to_csv_file>');`

#### IP to geo location

TorMap uses a binary DB file from `IP2Location` to map IPv4 addresses of Tor nodes to geo locations. It is advised to
replace the binary file every few months, to keep the IP ranges up to date.

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
- `yarn start`: creates build, runs it and listens on http://localhost:3000 (page reloads if you save frontend changes)
- `yarn build`: creates production ready build in `build` folder

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
3. A `build` folder should be generated containing all necessary frontend files

## Host project

Make sure you have installed all requirements for development. To be able to host, you should have a copy of a release
or just successfully created your own project build. For now prebuild releases are available
at https://lightningpuzzle.com/tormap/.

### Backend

1. Go into the directory where the `.jar` file is located
2. Run command `java -jar <backend jar file>`
3. Backend should be available at http://localhost:8080

### Frontend

1. Go into the directory where `index.html` is located
2. Run command: `serve -l 3000`
3. Frontend should be available at http://localhost:3000


