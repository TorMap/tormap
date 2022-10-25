FROM gradle:7.5-jdk11 as builder
WORKDIR application

COPY . .
RUN ./gradlew bootJar
RUN java -Djarmode=layertools -jar build/libs/tormap-1.0.0.jar extract

FROM eclipse-temurin:11-jre
WORKDIR application

COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

# Makes JVM aware of memory limit available to the container (cgroups)
ENV JAVA_OPTS='-XX:+UseContainerSupport -XX:MaxRAMPercentage=80'

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
