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

## IP to geo location DBs

TorMap uses two file based DBs to map IPv4 addresses of Tor nodes to latitude and longitude values. To use the latest IP records it is advised to replace the database files of `IP2Location` and `Maxmind` every few weeks.
Currently the DB files still need to be downloaded manually.

### IP2Location

1. Create a free account at https://lite.ip2location.com/sign-up
2. Replace the `DOWNLOAD_TOKEN` in the following link with you own one:
   https://www.ip2location.com/download/?token=DOWNLOAD_TOKEN&file=DB5LITEBIN
3. Now replace the old DB file located at `./database/ip2location/database.BIN` with the new one.

### Maxmind

1. Create an account and license key at https://www.maxmind.com/
2. Replace `YOUR_LICENSE_KEY` in the following link and download the DB:
   https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=YOUR_LICENSE_KEY&suffix=tar.gz
3. Now replace the old DB file located at `./database/maxmind/database.mmdb` with the new one.
