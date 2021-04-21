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
- `./gradlew build`: build, test, and analyze the project
- On `Windows` replace `./gradlew` with `./gradlew.bat` for all commands mentioned above
