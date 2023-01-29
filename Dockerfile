# syntax = docker/dockerfile:1.4.3

FROM ghcr.io/graalvm/native-image:22.3.0 AS builder

RUN microdnf update -y \
 && microdnf install --nodocs -y tar gzip findutils \
 && microdnf clean all \
 && rm -rf /var/cache/yum
WORKDIR /build
ENV GRADLE_OPTS="-Dorg.gradle.project.buildDir=/gradle/build" \
    GRADLE_USER_HOME="/gradle/home" \
    PROJECT_CACHE_DIR="/gradle/project-cache-dir"
RUN --mount=type=cache,target=$GRADLE_USER_HOME --mount=type=cache,target=$PROJECT_CACHE_DIR --mount=source=backend,target=. \
    ./gradlew --no-watch-fs --no-daemon --project-cache-dir $PROJECT_CACHE_DIR nativeCompile
RUN strip --strip-all /gradle/build/native/nativeCompile/tormap


# final image
FROM spritsail/busybox:1.36.0

RUN adduser --disabled-password -g '' -h '/home/nonroot' -s '/sbin/nologin' -u '10001' nonroot \
    && rm /etc/group- /etc/passwd- /etc/shadow-
USER 10001:10001
EXPOSE 8080
WORKDIR /home/nonroot
ENTRYPOINT ["/sbin/tini", "--", "/home/nonroot/tormap"]
COPY --link --from=builder --chown=10001:10001 --chmod=500 /gradle/build/native/nativeCompile/tormap /home/nonroot/tormap
