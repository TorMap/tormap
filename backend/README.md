# Backend

## Requirements

### Automatic

On `Linux` systems you can use the main install script `./install`, also described in the main README.md.

### Manual

- Install a Java Version >= 11: [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
  / [OpenJDK](https://openjdk.java.net/install/index.html)
- Go into `backend` and use command `./gradlew`
- Done

## CLI Commands

- `./gradlew`: Installs dependencies
- `./gradlew bootRun`:  creates build, runs it and listens on http://localhost:8080/
- `./gradlew build`: build the project
- `./gradlew flywayMigrate`: Migrates the database
- `./gradlew flywayClean`: Drops all objects in the configured schemas
- `./gradlew flywayInfo`: Prints the details and status information about all the migrations
- `./gradlew flywayValidate`: Validates the applied migrations against the ones available on the classpath
- `./gradlew flywayBaseline`: Baselines an existing database, excluding all migrations up to and including baselineVersion
- `./gradlew flywayRepair`: Repairs the schema history table
- `./gradlew dokkaHtml`: generate code documentation in HTML format
- On `Windows` replace `./gradlew` with `./gradlew.bat` for all commands mentioned above

## Database
TorMap uses an embedded H2 database which saves the whole state in a single DB file located at `database/tormap.mv.db`. To connect to the DB you either can add the datasource in your IDE open http://localhost:8080/h2 while the backend is running. Make sure to configure the connection the same way your `application.properties` are set. In an IDE it might be necessary to configure the datasource URL with `./backend/...` or an absolute path to ensure the correct working directory is used.

### IP to autonomous systems
If you are not starting with a preprocessed TorMap DB you will need to import a CSV file containing autonomous systems via a SQL Query Console. This is also useful if you want to replace records with more recent data.

#### Manual import
1. Create a free account at https://lite.ip2location.com/sign-up
2. Download latest IPv4 CSV file from https://lite.ip2location.com/database-asn or use CSV located at `database/csv/IP2LOCATION-LITE-ASN/IP2LOCATION-LITE-ASN.CSV`
4. Run following commands on the TorMap DB:
   1. `TRUNCATE TABLE AUTONOMOUS_SYSTEM;`
   2. `INSERT INTO AUTONOMOUS_SYSTEM SELECT * FROM CSVREAD('<absolute_path_to_csv_file>');` (replace <absolute_path_to_csv_file> with your file)

### IP to geo location

TorMap uses a binary DB file from `IP2Location` to map IPv4 addresses of Tor nodes to geo locations. It is advised to replace the binary file every few weeks, to keep the IP ranges up to date.

#### Manual replacement
1. Create a free account at https://lite.ip2location.com/sign-up
2. Download latest IPv4 BIN file from https://lite.ip2location.com/database/db5-ip-country-region-city-latitude-longitude
3. Replace old BIN file with new one in `src/main/resources/db/ip2location`
